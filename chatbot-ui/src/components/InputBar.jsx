// src/components/InputBar.jsx
import React, { useState } from 'react';
import { IoSend, IoImageOutline, IoMic, IoVolumeHigh, IoVolumeMute, IoStopCircle, IoCloseCircle } from 'react-icons/io5';
import { FaStop } from 'react-icons/fa';

const InputBar = ({
    onSendMessage, onImageUpload, onMicClick, isRecording,
    isTtsEnabled, onTtsToggle, isBotSpeaking, onStopTts,
    isWaitingForResponse, onCancelRequest // <-- New props
}) => {
    const [text, setText] = useState('');
    const handleSend = () => { if (text.trim()) { onSendMessage(text); setText(''); } };
    const handleKeyPress = (e) => { if (e.key === 'Enter') handleSend(); };
    const handleImageChange = (e) => {
        if (e.target.files && e.target.files[0]) { onImageUpload(e.target.files[0]); e.target.value = null; }
    };

    return (
        <div className="input-bar">
            {isBotSpeaking ? (
                <button onClick={onStopTts} className="icon-btn tts-stop-btn" title="Stop Speaking"><IoStopCircle /></button>
            ) : (
                <button onClick={onTtsToggle} className="icon-btn" title={isTtsEnabled ? "Disable TTS" : "Enable TTS"}>
                    {isTtsEnabled ? <IoVolumeHigh /> : <IoVolumeMute />}
                </button>
            )}

            <input type="text" className="text-input" placeholder="Type a message or say 'Hey Mitra'" value={text} onChange={(e) => setText(e.target.value)} onKeyPress={handleKeyPress} disabled={isWaitingForResponse} />

            {/* --- NEW: Conditional Cancel/Send Button --- */}
            {isWaitingForResponse ? (
                <button onClick={onCancelRequest} className="icon-btn tts-stop-btn" title="Cancel Request">
                    <IoCloseCircle />
                </button>
            ) : (
                <button onClick={handleSend} className="icon-btn" title="Send Text">
                    <IoSend />
                </button>
            )}
            
            <label htmlFor="image-input" className="icon-btn" title="Upload Image"><IoImageOutline /></label>
            <input type="file" id="image-input" className="hidden-input" accept="image/*" onChange={handleImageChange} />
            <button onClick={onMicClick} className={`icon-btn ${isRecording ? 'recording' : ''}`} title={isRecording ? 'Stop Recording' : 'Record Voice'}>
                {isRecording ? <FaStop /> : <IoMic />}
            </button>
        </div>
    );
};
export default InputBar;