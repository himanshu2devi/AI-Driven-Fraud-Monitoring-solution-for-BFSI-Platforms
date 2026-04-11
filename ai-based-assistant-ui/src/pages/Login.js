import { useState } from "react";

function Login({ onLogin, goToRegister }) {

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const handleLogin = async () => {

    if (!username || !password) {
      alert("Please enter username and password");
      return;
    }

    try {
      const res = await fetch("http://localhost:8080/api/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          username,
          password
        })
      });

      if (!res.ok) {
        const error = await res.text();
        alert(error);
        return;
      }

      const data = await res.json();

      //  Store JWT + user
      localStorage.setItem("token", data.token);
     localStorage.setItem("userId", username);

     //  generate new session per login
     localStorage.setItem("sessionId", "session-" + Date.now());

      onLogin(username);

    } catch (err) {
      console.error(err);
      alert("Login failed");
    }
  };

  return (
    <div className="auth-page"> {/* 🔥 VERY IMPORTANT FIX */}

      <div className="auth-container">

        <h2>🔐 Login</h2>

        <input
          type="text"
          placeholder="Enter Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />

        <input
          type="password"
          placeholder="Enter Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        <button onClick={handleLogin}>Login</button>

        <p className="link" onClick={goToRegister}>
          New user? Register
        </p>

      </div>

    </div>
  );
}

export default Login;