function Header() {

  const userId = localStorage.getItem("userId");

  const logout = () => {
    localStorage.removeItem("userId");
    window.location.reload();
  };

  return (
    <div className="header">

      {/* LEFT TITLE */}
      <div className="header-left">
        🤖 AI Based Fraud Monitoring Assistant
      </div>

      {/* RIGHT SIDE (USER + LOGOUT) */}
      <div className="header-right">
        <span className="username">
          👤 {userId ? ` ${userId}` : "User"}
        </span>

        <button onClick={logout} className="logout-btn">
          Logout
        </button>
      </div>

    </div>
  );
}

export default Header;