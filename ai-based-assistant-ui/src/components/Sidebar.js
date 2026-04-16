import { useEffect, useState } from "react";

function Sidebar({ onBlockedClick,onFraudSummaryClick }) {

  const [recentQueries, setRecentQueries] = useState([]);

  const userId = localStorage.getItem("userId");

  useEffect(() => {
    fetchRecent();
  }, []);

  const fetchRecent = async () => {
    try {
      const token = localStorage.getItem("token");

      const res = await fetch(
        `http://localhost:8090/api-gateway/aiassistant/api/v1/fraud-assistant/recent?userId=${userId}`,
        {
          headers: {
            "Authorization": "Bearer " + token
          }
        }
      );

      const data = await res.json();

      //  DEBUG (check what API returns)
      console.log("Recent Queries API Response:", data);

      //  HANDLE MULTIPLE RESPONSE TYPES
      if (Array.isArray(data)) {
        setRecentQueries(data);
      } else if (Array.isArray(data.data)) {
        setRecentQueries(data.data);
      } else {
        setRecentQueries([]);
      }

    } catch (err) {
      console.error("❌ Error fetching recent queries", err);
    }
  };

  return (
    <div className="sidebar">

      <h3>Menu</h3>

      <ul>

      <li
        onClick={() => {
          console.log("📊 Fraud Summary CLICKED");
          onFraudSummaryClick && onFraudSummaryClick();
        }}
        style={{ cursor: "pointer" }}
      >
        📊 Fraud Summary
      </li>

        <li
          onClick={() => {
            console.log("🖱️ CLICKED BLOCKED ACCOUNTS");
            onBlockedClick && onBlockedClick();
          }}
          style={{ cursor: "pointer" }}
        >
          🚫 Blocked Accounts
        </li>
      </ul>

      {/*  RECENT QUERIES */}
      <h4 style={{ marginTop: "20px" }}>🕓 Last 5 Queries of user : {userId}</h4>

      <ul>
        {recentQueries.length === 0 && (
          <li style={{ color: "gray" }}>No recent queries</li>
        )}

        {recentQueries.map((q, i) => (
          <li key={i} style={{ fontSize: "13px" }}>
            • {typeof q === "string" ? q : q.question || JSON.stringify(q)}
          </li>
        ))}
      </ul>

       <div className="sidebar-footer">
          <p>This application is developed by Himanshu</p>
          <br></br>
          <p>© All rights reserved by Wipro Limited</p>
        </div>

    </div>


  );


}

export default Sidebar;