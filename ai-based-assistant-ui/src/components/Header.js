function Header() {

  const logout = () => {
    localStorage.removeItem("userId");
    window.location.reload();
  };

  return (
    <div className="header">
      🤖 AI Based Fraud Monitoring Assistant

      <button onClick={logout} style={{ float: "right" }}>
        Logout
      </button>
    </div>
  );
}

export default Header;