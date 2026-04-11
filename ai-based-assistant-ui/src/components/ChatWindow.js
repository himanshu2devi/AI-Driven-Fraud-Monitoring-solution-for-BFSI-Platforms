import { useState } from "react";
import Message from "./Message";
import '../index.css';
import '../App.css';

function ChatWindow({ userId, activeView, blockedAccounts, fraudSummary,setActiveView }) {

  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");


  if (activeView === "summary" && fraudSummary) {
      return (
        <div className="chat-window">

          <h2>📊 Fraud Summary</h2>

          <button onClick={() => setActiveView("chat")}>⬅ Back</button>

          {/* KPI */}
         <div style={{ display: "flex", gap: "20px", margin: "20px 0" }}>

           <div className="kpi-card">
             <h4>Total</h4>
             <p>{fraudSummary.totalTransactions}</p>
           </div>

           <div className="kpi-card fraud">
             <h4>Fraud</h4>
             <p>{fraudSummary.fraudCount}</p>
           </div>

           <div className="kpi-card alert">
             <h4>Alert</h4>
             <p>{fraudSummary.alertCount}</p>
           </div>

           <div className="kpi-card rate">
             <h4>Fraud Rate</h4>
             <p>{fraudSummary.fraudRate.toFixed(2)}%</p>
           </div>

         </div>

          {/* Distribution */}
          <h3>Status Distribution</h3>
          <ul>
            <li>VALID: {fraudSummary.statusDistribution.VALID}</li>
            <li>ALERT: {fraudSummary.statusDistribution.ALERT}</li>
            <li>FRAUD: {fraudSummary.statusDistribution.FRAUD}</li>
          </ul>

          {/* Recent Frauds */}
          <h3>Recent Fraud Transactions</h3>
          <table className="data-table">
            <thead>
              <tr>
                <th>Account</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Reason</th>
              </tr>
            </thead>
            <tbody>
              {fraudSummary.recentFrauds.map((tx, i) => (
                <tr key={i}>
                  <td>{tx.accountTo}</td>
                  <td>{tx.amount}</td>
                  <td>{tx.status}</td>
                  <td>{tx.reason}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Top Patterns */}
          <h3>Top Fraud Patterns</h3>
          <ul>
            {fraudSummary.topPatterns.map((p, i) => (
              <li key={i}>
                {p.reason} ({p.count})
              </li>
            ))}
          </ul>

        </div>
      );
    }

  // =========================
  // SEND MESSAGE
  // =========================
  const sendMessage = async () => {

    if (!input.trim()) return;

    const userText = input;
    setInput("");

    const userMessage = { text: userText, sender: "user" };
    setMessages(prev => [...prev, userMessage]);

    setLoading(true);

    try {

      const token = localStorage.getItem("token");

      const response = await fetch(
        "http://localhost:8090/api-gateway/aiassistant/api/v1/fraud-assistant/ask",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + token
          },
          body: JSON.stringify({
            sessionId: localStorage.getItem("sessionId"),
            analystId: localStorage.getItem("userId"),
            question: userText
          })
        }
      );

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || "API Error");
      }

      const data = await response.json();

      const botMessage = {
        text: data.answer,
        sender: "bot",
        source: data.source
      };

      setMessages(prev => [...prev, botMessage]);

    } catch (error) {

      console.error(error);

      setMessages(prev => [
        ...prev,
        {
          text: "⚠️ Something went wrong. Please try again.",
          sender: "bot"
        }
      ]);
    }

    setLoading(false);
  };

  // =========================
  // BLOCKED ACCOUNTS VIEW
  // =========================
  if (activeView === "blocked") {
    return (
      <div className="chat-window">

        <div className="blocked-container">

          <div className="blocked-header">
            <h2>🚫 Blocked Accounts</h2>
            <button onClick={() => setActiveView("chat")}>⬅ Back</button>
          </div>

          <table className="data-table">
            <thead>
              <tr>
                <th>Account Number</th>
                <th>Reason</th>
                <th>Blocked At</th>
                <th>Status</th>
              </tr>
            </thead>

            <tbody>
              {blockedAccounts.length > 0 ? (
                blockedAccounts.map((acc, i) => (
                  <tr key={i}>
                    <td>{acc.accountNumber}</td>
                    <td>{acc.reason}</td>
                    <td>{new Date(acc.blockedAt).toLocaleString()}</td>
                    <td style={{ color: "red", fontWeight: "bold" }}>
                      BLOCKED
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="4">No blocked accounts found</td>
                </tr>
              )}
            </tbody>
          </table>

        </div>
      </div>
    );
  }

  // =========================
  // CHAT VIEW (DEFAULT)
  // =========================
  return (
    <div className="chat-window">

      {/* Messages */}
      <div className="messages">
        {messages.map((msg, index) => (
          <Message key={index} message={msg} />
        ))}
      </div>

      {/* Typing Indicator */}
      {loading && (
        <div className="typing-wrapper">

          <div className="typing-bubble">
            <span></span>
            <span></span>
            <span></span>
          </div>

          <div className="typing-label">
            AI Assistant is typing...
          </div>

        </div>
      )}

      {/* Input Box */}
      <div className="input-box">
        <input
          type="text"
          placeholder="Ask fraud related question..."
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") sendMessage();
          }}
        />

        <button onClick={sendMessage}>Send</button>
      </div>

    </div>
  );
}

export default ChatWindow;