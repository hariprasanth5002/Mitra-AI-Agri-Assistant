// src/components/ChatWindow.jsx
import React, { useEffect, useRef } from 'react';
import Message from './Message';

const ChatWindow = ({ messages }) => {
    const messagesEndRef = useRef(null);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);

    return (
        <div className="chat-window">
            {messages.map((msg, index) => (
                <Message key={index} message={msg} />
            ))}
            <div ref={messagesEndRef} />
        </div>
    );
};

export default ChatWindow;