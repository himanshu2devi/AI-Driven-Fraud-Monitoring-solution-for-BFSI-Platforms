import { useState } from "react";

function Register({ goToLogin }) {

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState("ROLE_USER");

  const handleRegister = async () => {

    if (!username || !email || !password || !role) {
      alert("Please fill all fields");
      return;
    }

    try {
      const res = await fetch("http://localhost:8080/api/auth/register", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          username,
          email,
          password,
          role
        })
      });

      const data = await res.text();

      if (res.ok) {
        alert("Registered successfully!");
        goToLogin();
      } else {
        alert(data);
      }

    } catch (err) {
      console.error(err);
      alert("Something went wrong");
    }
  };

  return (
    <div className="auth-container">

      <h2>📝 Register</h2>

      {/* Username */}
      <input
        type="text"
        placeholder="Create Username"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
      />

      {/* Email */}
      <input
        type="email"
        placeholder="Enter Email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />

      {/* Password */}
      <input
        type="password"
        placeholder="Create Password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />

      {/* Role Dropdown */}
      <select
        value={role}
        onChange={(e) => setRole(e.target.value)}
        style={{
          width: "100%",
          padding: "12px",
          margin: "10px 0",
          borderRadius: "8px",
          border: "1px solid #ccc"
        }}
      >
        <option value="ROLE_USER">User</option>
        <option value="ROLE_FRAUDANALYST">Fraud Analyst</option>
        <option value="ROLE_ADMIN">Admin</option>
      </select>

      <button onClick={handleRegister}>Register</button>

      <p className="link" onClick={goToLogin}>
        Already have an account? Login
      </p>

    </div>
  );
}

export default Register;