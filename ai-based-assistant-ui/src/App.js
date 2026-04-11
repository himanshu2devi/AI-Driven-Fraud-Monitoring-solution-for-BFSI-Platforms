import { useState } from "react";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Header from "./components/Header";
import Sidebar from "./components/Sidebar";
import ChatWindow from "./components/ChatWindow";

function App() {

  const [user, setUser] = useState(localStorage.getItem("userId"));
  const [page, setPage] = useState("login");
  const [fraudSummary, setFraudSummary] = useState(null);

  //  GLOBAL UI STATE
  const [activeView, setActiveView] = useState("chat");
  const [blockedAccounts, setBlockedAccounts] = useState([]);

  //  LOAD BLOCKED ACCOUNTS (FROM SIDEBAR CLICK)
  const loadBlockedAccounts = async () => {

    console.log(" Sidebar clicked → Loading blocked accounts");

    try {

      const res = await fetch(
        "http://localhost:8090/api-gateway/Fraud-Management/api/blocked-accounts"
      );

      console.log("📡 API Response status:", res.status);

      if (!res.ok) {
        throw new Error("API failed");
      }

      const data = await res.json();

      console.log("✅ Data received:", data);

      setBlockedAccounts(data || []);
      setActiveView("blocked");

      console.log("🟢 View switched to BLOCKED");

    } catch (err) {
      console.error("❌ Error loading blocked accounts:", err);
      alert("Failed to load blocked accounts");
    }
  };

  const loadFraudSummary = async () => {

    console.log("📊 Loading Fraud Summary");

    try {
      const res = await fetch(
        "http://localhost:8090/api-gateway/Fraud-Management/api/fraud-summary"
      );

      const data = await res.json();

      console.log("Fraud Summary Data:", data);

      setFraudSummary(data);
      setActiveView("summary");

    } catch (err) {
      console.error("Error loading fraud summary:", err);
    }
  };

  // =========================
  // LOGGED IN UI
  // =========================
  if (user) {
    return (
      <div className="app-container">

        {/* HEADER */}
        <Header />

        {/* MAIN LAYOUT */}
        <div className="main-container">

          {/* SIDEBAR */}
          <div className="sidebar">
           <Sidebar
             onBlockedClick={loadBlockedAccounts}
             onFraudSummaryClick={loadFraudSummary}
           />
          </div>

          {/* MAIN CONTENT */}
          <div className="chat-container">
            <ChatWindow
              userId={user}
              activeView={activeView}
              blockedAccounts={blockedAccounts}
              fraudSummary={fraudSummary}
              setActiveView={setActiveView}
            />
          </div>

        </div>
      </div>
    );
  }

  // =========================
  // REGISTER PAGE
  // =========================
  if (page === "register") {
    return (
      <div className="auth-page">
        <Register goToLogin={() => setPage("login")} />
      </div>
    );
  }

  // =========================
  // LOGIN PAGE
  // =========================
  return (
    <div className="auth-page">
      <Login
        onLogin={setUser}
        goToRegister={() => setPage("register")}
      />
    </div>
  );
}

export default App;