import { useState } from "react";
import Message from "./Message";
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



function ChatWindow({ userId, activeView, blockedAccounts, fraudSummary, setActiveView }) {


  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");



 const [sessionId] = useState(() => {
   const user = localStorage.getItem("username");

   const storedSession = localStorage.getItem("sessionId");
   const storedUser = localStorage.getItem("sessionUser");

   if (storedSession && storedUser === user) {
     return storedSession;
   }

   const newId = "session-" + Date.now();
   localStorage.setItem("sessionId", newId);
   localStorage.setItem("sessionUser", user);

   return newId;
 });

 const analystId = userId;


  // =========================
  // SEND MESSAGE
  // =========================
const sendMessage = async () => {


  const userInput = input;
  setInput("");

  // ✅ Show user message
  setMessages(prev => [
    ...prev,
    { sender: "user", text: userInput }
  ]);
  setLoading(true);

  try {

    // ===============================
    // 🔹 STEP 1: CALL MAIN API
    // ===============================
    const response = await fetch("http://localhost:8090/api-gateway/aiassistant/api/v1/fraud-assistant/ask", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        sessionId,
        analystId,
        question: userInput
      })
    });

    const data = await response.json();

    // ===============================
    // 🔹 STEP 2: SHOW INITIAL RESPONSE
    // ===============================
    setMessages(prev => [
      ...prev,
     {
       sender: "bot",
       text: data.answer,
       type: data.type,
       data: data.data || [],
       source: data.source,
       suggestions: data.suggestions || [],
       title: data.title
     }
    ]);

    // ===============================
    // 🔥 STEP 3: IF LOADING → CALL AI API
    // ===============================
    if (data.type === "LOADING") {

      // Optional: small delay for better UX
      await new Promise(res => setTimeout(res, 500));

      const analysisRes = await fetch("http://localhost:8090/api-gateway/aiassistant/api/v1/fraud-assistant/analyze", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          sessionId,
          analystId,
          question: userInput
        })
      });

      const analysisData = await analysisRes.json();

      // ===============================
      // 🔹 STEP 4: SHOW AI RESULT
      // ===============================
      setMessages(prev => [
        ...prev,
       {
         sender: "bot",
         text: analysisData.answer,
         type: "ANALYSIS",
         data: [],
         source: analysisData.source
       }
      ]);
    }

  } catch (error) {

    console.error("Frontend Error:", error);

    setMessages(prev => [
      ...prev,
      {
        sender: "bot",
        text: "⚠️ Something went wrong. Please try again.",
        type: "ERROR"
      }
    ]);
  }
  finally {
    setLoading(false);
  }

};

 // =========================
 // SUMMARY VIEW
 // =========================
 if (activeView === "summary" && fraudSummary) {
   return (
     <div className="chat-window">

       <h2>📊 Fraud Summary</h2>

       <button className="back-btn" onClick={() => setActiveView("chat")}>
         ⬅ Back
       </button>

       {/* ================= KPI CARDS ================= */}
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

       {/* ================= CHARTS ================= */}
       <div className="chart-container">

         {/* BAR */}
         <div className="chart-box">
           <h3>Transaction Status</h3>

           <BarChart width={400} height={250}
             data={[
               { name: "SUCCESSFUL", value: fraudSummary.successCount || 0 },
               { name: "FAILED", value: fraudSummary.failedCount || 0 }
             ]}
           >
             <CartesianGrid strokeDasharray="3 3" />
             <XAxis dataKey="name" />
             <YAxis />
             <Tooltip />
             <Bar dataKey="value">
               <Cell fill="#22c55e" />
               <Cell fill="#ef4444" />
             </Bar>
           </BarChart>
         </div>

         {/* PIE */}
         <div className="chart-box">
           <h3>Fraud Distribution</h3>

           <PieChart width={300} height={300}>
             <Pie
               data={[
                 { name: "VALID", value: fraudSummary.statusDistribution?.VALID || 0 },
                 { name: "ALERT", value: fraudSummary.statusDistribution?.ALERT || 0 },
                 { name: "FRAUD", value: fraudSummary.statusDistribution?.FRAUD || 0 }
               ]}
               dataKey="value"
               nameKey="name"
               outerRadius={100}
               label
             >
               <Cell fill="#22c55e" />
               <Cell fill="#f59e0b" />
               <Cell fill="#ef4444" />
             </Pie>
             <Tooltip />
           </PieChart>
         </div>

       </div>

       {/* ================= RECENT FRAUD ================= */}
       <h3>Recent Fraud Transactions</h3>

       <table className="data-table">
         <thead>
           <tr>
             <th>Account</th>
             <th>Amount</th>
             <th>Status</th>
             <th>Reason</th>
             <th>Time</th>
           </tr>
         </thead>

         <tbody>
           {fraudSummary.recentFrauds?.map((tx, i) => (
             <tr key={i}>
               <td>{tx.accountTo}</td>
               <td>₹{tx.amount}</td>
               <td className="status-fraud">{tx.status}</td>
               <td>{tx.reason}</td>
               <td>{tx.timestamp}</td>
             </tr>
           ))}
         </tbody>
       </table>

       {/* ================= TOP PATTERNS ================= */}
       <h3>Top Fraud Patterns</h3>

       <ul>
         {fraudSummary.topPatterns?.map((p, i) => (
           <li key={i}>
             {p.reason} ({p.count})
           </li>
         ))}
       </ul>

     </div>
   );
 }
// =========================
// 🚫 BLOCKED VIEW (FINAL FIXED)
// =========================
if (activeView === "blocked") {

  console.log("BLOCKED DATA:", blockedAccounts); // 🔍 debug

  return (
    <div className="chat-window">

      <h2>🚫 Blocked Accounts</h2>

      <button
        className="back-btn"
        onClick={() => setActiveView("chat")}
      >
        ⬅ Back
      </button>

      <table className="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Account</th>
            <th>Reason</th>
            <th>Blocked At</th>
            <th>Status</th>
          </tr>
        </thead>

        <tbody>
          {Array.isArray(blockedAccounts) && blockedAccounts.length > 0 ? (
            blockedAccounts.map((acc, i) => {

              const account = acc.accountNumber || acc.account_number || "-";
              const blockedTime = acc.blockedAt || acc.blocked_at;

              return (
                <tr key={i}>
                  <td>{acc.id ?? "-"}</td>
                  <td>{account}</td>
                  <td>{acc.reason ?? "-"}</td>
                  <td>
                    {blockedTime
                      ? new Date(blockedTime).toLocaleString()
                      : "-"}
                  </td>
                  <td className="status-failed">BLOCKED</td>
                </tr>
              );
            })
          ) : (
            <tr>
              <td colSpan="5" style={{ textAlign: "center" }}>
                No blocked accounts found
              </td>
            </tr>
          )}
        </tbody>
      </table>

    </div>
  );
}
  // =========================
  // CHAT VIEW
  // =========================
  return (
    <div className="chat-window">

     <div className="messages">
       {messages.map((msg, index) => (
         <div key={index}>
           <Message message={msg} />

           {/* Suggestions */}
           {msg.sender === "bot" && msg.suggestions && (
             <div className="suggestions">
               {msg.suggestions.map((s, i) => (
                 <button
                   key={i}
                   className="suggestion-btn"
                   onClick={() => setInput(s)}
                 >
                   {s}
                 </button>
               ))}
             </div>
           )}
         </div>
       ))}

       {/* ✅ ADD TYPING HERE */}
       {loading && (
         <div className="message-wrapper bot">
           <div className="message-bubble bot-bubble">

             {/* DOTS */}
             <div className="typing-bubble">
               <span></span>
               <span></span>
               <span></span>
             </div>

             {/* TEXT */}
             <div className="typing-text">
               AI Assistant is typing...
             </div>

           </div>
         </div>
       )}
     </div>



      {/* INPUT */}
      <div className="input-box">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask fraud related question..."
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              sendMessage();
            }
          }}
        />
        <button onClick={sendMessage}>Send</button>
      </div>

    </div>
  );
}

export default ChatWindow;