function Message({ message }) {


if (message.type === "FRAUD_PATTERNS" && message.data) {
  return (
    <div className="message-wrapper bot">
      <div className="message-bubble bot-bubble">

        <h4 className="table-title">📊 Fraud Patterns</h4>

        <table className="data-table">
          <thead>
            <tr>
              <th>Reason</th>
              <th>Count</th>
            </tr>
          </thead>
          <tbody>
            {message.data.map((p, i) => (
              <tr key={i}>
                <td>{p.reason}</td>
                <td>{p.count}</td>
              </tr>
            ))}
          </tbody>
        </table>

        <span className="source">Source: {message.source}</span>
      </div>
    </div>
  );
}


if (message.type === "FRAUD_INSIGHTS" && message.data) {
  return (
    <div className="message-wrapper bot">
      <div className="message-bubble bot-bubble">

       <h4 className="table-title">📊 Fraud Insights</h4>

        <table className="data-table">
          <thead>
            <tr>
              <th>Status</th>
              <th>Count</th>
            </tr>
          </thead>

          <tbody>
            {message.data.map((row, i) => (
              <tr key={i}>
                <td>{row.status}</td>
                <td>{row.count}</td>
              </tr>
            ))}
          </tbody>
        </table>

        <span className="source">Source: {message.source}</span>

      </div>
    </div>
  );
}

// =========================
// ERROR MESSAGE
// =========================
if (message.type === "ERROR") {
  return (
    <div className="message-wrapper bot">
      <div className="message-bubble bot-bubble" style={{
        background: "#ffe6e6",
        border: "1px solid #ff4d4f",
        color: "#a8071a"
      }}>
        {message.text}
      </div>
    </div>
  );
}

  const isUser = message.sender === "user";

  const wrapperClass = `message-wrapper ${isUser ? "user" : "bot"}`;
  const bubbleClass = `message-bubble ${isUser ? "user-bubble" : "bot-bubble"}`;

  // =========================
  // 🔥 AUTO-DETECT TABLE DATA
  // =========================
  const isTransactionData =
    message.data &&
    Array.isArray(message.data) &&
    message.data.length > 0 &&
    (
      message.data[0].from_account ||
      message.data[0].accnofrom
    );

  const isFraudData =
    message.data &&
    Array.isArray(message.data) &&
    message.data.length > 0 &&
    message.data[0].account_from;

  // =========================
  // TRANSACTIONS (AUTO)
  // =========================
  if (isTransactionData) {
    return (
      <div className={wrapperClass}>
        <div className={bubbleClass}>
          <h4>💳 Transactions</h4>

          <table className="data-table">
            <thead>
              <tr>
                <th>From</th>
                <th>To</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Time</th>
              </tr>
            </thead>

            <tbody>
              {message.data.map((t, i) => (
                <tr key={i}>
                  <td>{t.from_account || t.accnofrom}</td>
                  <td>{t.to_account || t.accnoto}</td>
                  <td>₹{t.amount}</td>
                  <td className={
                    t.status === "FAILED" ? "status-failed" :
                    t.status === "FRAUD" ? "status-fraud" :
                    t.status === "ALERT" ? "status-alert" :
                    "status-success"
                  }>
                    {t.status}

                  </td>

                  <td>{t.timestamp}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <span className="source">Source: {message.source}</span>
        </div>
      </div>
    );
  }


  const isAccountsData = message.type === "ACCOUNTS";

  if (isAccountsData) {
    return (
      <div className={wrapperClass}>
        <div className={bubbleClass}>

          <h4 className="table-title">
            {message.title || "🏦 Accounts"}
          </h4>

          <table className="data-table">
            <thead>
              <tr>
                <th>Account</th>
                <th>Balance</th>
                <th>IFSC</th>
                <th>User ID</th>
                <th>Status</th>
                <th>Currency</th>
              </tr>
            </thead>

            <tbody>
              {message.data.map((a, i) => (
                <tr key={i}>
                  <td>{a.account_number}</td>
                  <td>₹{a.balance}</td>
                  <td>{a.ifsc_code}</td>
                  <td>{a.user_id}</td>
                  <td>{a.account_enabled ? "ACTIVE" : "DISABLED"}</td>
                  <td>{a.currency}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <span className="source">Source: {message.source}</span>
        </div>
      </div>
    );
  }

  // =========================
  // FRAUD TRANSACTIONS (AUTO)
  // =========================
  if (isFraudData) {
    return (
      <div className={wrapperClass}>
        <div className={bubbleClass}>
<h4 className="table-title">
  {message.title || "📊 Transactions"}
</h4>
          <table className="data-table">
            <thead>
              <tr>
                <th>From</th>
                <th>To</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Reason</th>
                <th>Time</th>
              </tr>
            </thead>

            <tbody>
              {message.data.map((t, i) => (
                <tr key={i}>
                  <td>{t.account_from}</td>
                  <td>{t.account_to}</td>
                  <td>₹{t.amount}</td>
                  <td className={
                    t.status === "FRAUD" ? "status-fraud" :
                    t.status === "ALERT" ? "status-alert" :
                    "status-success"
                  }>
                    {t.status}
                  </td>
                  <td>{t.reason}</td>
                  <td>{t.timestamp}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <span className="source">Source: {message.source}</span>
        </div>
      </div>
    );
  }


  const isAccountLimitData = message.type === "ACCOUNT_LIMIT";

  if (isAccountLimitData) {
    return (
      <div className={wrapperClass}>
        <div className={bubbleClass}>

          <h4 className="table-title">
            {message.title || "🏦 Account Limits"}
          </h4>

          <table className="data-table">
            <thead>
              <tr>
                <th>Account</th>
                <th>Daily Limit</th>
                <th>Transaction Limit</th>
              </tr>
            </thead>

            <tbody>
              {message.data.map((t, i) => (
                <tr key={i}>
                  <td>{t.account_number}</td>
                  <td>₹{t.daily_limit}</td>
                  <td>₹{t.transaction_limit}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <span className="source">Source: {message.source}</span>
        </div>
      </div>
    );
  }

  // =========================
  // ACCOUNT BALANCE
  // =========================
  if (message.type === "ACCOUNT_BALANCE" && message.data) {
    return (
      <div className="message-wrapper bot">
        <div className="message-bubble bot-bubble">

          <h4 className="table-title">💰 Account Balance</h4>

          <table className="data-table">
            <thead>
              <tr>
                <th>Account Number</th>
                <th>Balance</th>
              </tr>
            </thead>

            <tbody>
              {message.data.map((acc, i) => (
                <tr key={i}>
                  <td>{acc.account_number}</td>
                  <td>₹{acc.balance}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <span className="source">Source: {message.source}</span>

        </div>
      </div>
    );
  }

  // =========================
  // USERS TABLE (NEW FIX)
  // =========================
  if (message.type === "USERS" && message.data) {
    return (
      <div className="message-wrapper bot">
        <div className="message-bubble bot-bubble">

          <h4 className="table-title">👤 Users</h4>

          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Username</th>
                <th>Email</th>
                <th>Status</th>
              </tr>
            </thead>

            <tbody>
              {message.data.map((u, i) => (
                <tr key={i}>
                  <td>{u.id}</td>
                  <td>{u.username}</td>
                  <td>{u.email}</td>
                  <td className={u.enabled ? "status-success" : "status-failed"}>
                    {u.enabled ? "ACTIVE" : "DISABLED"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <span className="source">Source: {message.source}</span>

        </div>
      </div>
    );
  }

  // =========================
  // BLOCKED ACCOUNTS TABLE
  // =========================
  if (message.type === "BLOCKED_ACCOUNT" && message.data) {
    return (
      <div className="message-wrapper bot">
        <div className="message-bubble bot-bubble">

          <h4 className="table-title">🚫 Blocked Accounts</h4>

          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Account</th>
                <th>Reason</th>
                <th>Blocked At</th>
              </tr>
            </thead>

            <tbody>
              {message.data.map((acc, i) => (
                <tr key={i}>
                  <td>{acc.id}</td>
                  <td>{acc.accountNumber || acc.account_number}</td>
                  <td>{acc.reason}</td>
                  <td>
                    {acc.blockedAt || acc.blocked_at
                      ? new Date(acc.blockedAt || acc.blocked_at).toLocaleString()
                      : "-"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <span className="source">Source: {message.source}</span>

        </div>
      </div>
    );
  }




  // =========================
  // DEFAULT TEXT
  // =========================
  return (
    <div className={wrapperClass}>
      <div className={bubbleClass}>
        {message.text}
        {message.source && (
          <span className="source">Source: {message.source}</span>
        )}
      </div>
    </div>
  );
}

export default Message;