function Message({ message }) {

  return (
    <div className={`message-wrapper ${message.sender}`}>

      <div className="message-bubble">
        {message.text}

        {/* ✅ Source */}
        {message.source && (
          <span className="source">
            Source: {message.source}
          </span>
        )}
      </div>

    </div>
  );
}

export default Message;