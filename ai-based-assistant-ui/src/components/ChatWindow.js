import { useState } from "react";
import Message from "./Message";
import '../index.css';
import '../App.css';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  PieChart,
  Pie,
  Cell
} from "recharts";

function ChatWindow({ userId, activeView, blockedAccounts, fraudSummary,setActiveView }) {

  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");



// BAR CHART → SUCCESS vs FAILED
const transactionData = fraudSummary
  ? [
      { name: "SUCCESSFUL", value: fraudSummary.successCount || 0 },
      { name: "FAILED", value: fraudSummary.failedCount || 0 }
    ]
  : [];

// PIE CHART → VALID / ALERT / FRAUD
const fraudData = fraudSummary?.statusDistribution
  ? [
      { name: "VALID", value: fraudSummary.statusDistribution.VALID || 0 },
      { name: "ALERT", value: fraudSummary.statusDistribution.ALERT || 0 },
      { name: "FRAUD", value: fraudSummary.statusDistribution.FRAUD || 0 }
    ]
  : [];

  const COLORS = ["#4caf50", "#ff9800", "#f44336"];

  if (activeView === "summary" && !fraudSummary) {
    return <div>Loading fraud summary...</div>;
  }


  if (activeView === "summary" && fraudSummary && fraudSummary.statusDistribution) {
      return (
        <div className="chat-window">

          <h2>📊 Fraud Summary</h2>


         <button className="back-btn" onClick={() => setActiveView("chat")}>
           ⬅ Back to chat window
         </button>

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

        <div className="chart-container">

          {/* BAR CHART */}
          <div className="chart-box">
            <h3>Transaction Status</h3>

            <BarChart width={400} height={250} data={transactionData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="value">
                {transactionData.map((entry, index) => (
                  <Cell
                    key={index}
                    fill={entry.name === "SUCCESSFUL" ? "#4caf50" : "#f44336"}
                  />
                ))}
              </Bar>
            </BarChart>
          </div>

          {/* PIE CHART */}
          <div className="chart-box">
            <h3>Fraud Distribution</h3>

            <PieChart width={300} height={300}>
              <Pie
                data={fraudData}
                dataKey="value"
                nameKey="name"
                outerRadius={100}
                label
              >
                {fraudData.map((entry, index) => (
                  <Cell
                    key={index}
                    fill={
                      entry.name === "VALID"
                        ? "#4caf50"
                        : entry.name === "ALERT"
                        ? "#ff9800"
                        : "#f44336"
                    }
                  />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </div>

        </div>

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
       source: data.source,
       suggestions: data.suggestions || []
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
           <button className="back-btn" onClick={() => setActiveView("chat")}>
                      ⬅ Back to chat window
                    </button>
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
          <div key={index}>

            {/* Message */}
            <Message message={msg} />

            {/* Suggestions (ONLY for bot messages) */}
            {msg.sender === "bot" && msg.suggestions && (
              <div className="suggestions">
                {msg.suggestions.map((s, i) => (
                  <button
                    key={i}
                    onClick={() => setInput(s)}
                    style={{
                      margin: "5px",
                      padding: "6px 10px",
                      borderRadius: "15px",
                      border: "1px solid #ccc",
                      cursor: "pointer"
                    }}
                  >
                    {s}
                  </button>
                ))}
              </div>
            )}

          </div>
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