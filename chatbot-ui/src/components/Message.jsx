// src/components/Message.jsx
import React from 'react';

const TypingIndicator = () => (
    <div className="typing-indicator">
        <span></span><span></span><span></span>
    </div>
);

const Message = ({ message }) => {
    const { text, sender, time, isLoading } = message;

    return (
        <div className={`message-wrapper ${sender}`}>
            <div className="message-content">
                {isLoading ? <TypingIndicator /> : text}
            </div>
            {time && <div className="timestamp">{time}</div>}
        </div>
    );
};

export default Message;