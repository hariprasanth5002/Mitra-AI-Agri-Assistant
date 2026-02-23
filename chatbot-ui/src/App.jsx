// src/App.jsx
import React, { useState, useEffect, useRef, useCallback } from 'react';
import ChatWindow from './components/ChatWindow';
import InputBar from './components/InputBar';
import './App.css';

// --- CONFIGURATION ---
const SPRING_BOOT_URL = "http://localhost:8082/chatbot/ask";

function App() {
    const [messages, setMessages] = useState([]);
    const [status, setStatus] = useState("Initializing...");
    const [isRecording, setIsRecording] = useState(false);
    const [isTtsEnabled, setIsTtsEnabled] = useState(true);
    const [isBotSpeaking, setIsBotSpeaking] = useState(false);
    const [isWaitingForResponse, setIsWaitingForResponse] = useState(false);
    
    const mediaRecorderRef = useRef(null);
    const recognitionRef = useRef(null);
    const audioStreamRef = useRef(null);
    const abortControllerRef = useRef(null);
    const isRecordingRef = useRef(isRecording);

    useEffect(() => {
        isRecordingRef.current = isRecording;
    }, [isRecording]);

    const addMessage = (text, sender, isLoading = false) => {
        const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        setMessages(prev => [...prev, { text, sender, time, isLoading }]);
    };
    
    const updateLastMessage = (text) => {
        setMessages(prev => {
            const newMessages = [...prev];
            if (newMessages.length === 0 || !newMessages[newMessages.length - 1].isLoading) return prev;
            const lastMessage = { ...newMessages[newMessages.length - 1], text, isLoading: false };
            newMessages[newMessages.length - 1] = lastMessage;
            return newMessages;
        });
    };
    
    const cancelPreviousRequest = () => { if (abortControllerRef.current) { abortControllerRef.current.abort(); } };

    const sendData = async (formData) => {
        cancelPreviousRequest();
        abortControllerRef.current = new AbortController();
        const signal = abortControllerRef.current.signal;
        addMessage("", 'bot', true);
        setIsWaitingForResponse(true);
        try {
            const response = await fetch(SPRING_BOOT_URL, { method: 'POST', body: formData, signal });
            if (!response.ok) throw new Error(`Server error: ${response.statusText}`);
            const data = await response.json();
            updateLastMessage(data.response);
            if (isTtsEnabled) synthesizeAndPlay(data.response);
        } catch (error) {
            if (error.name === 'AbortError') { setMessages(prev => prev.filter(msg => msg.isLoading === false));
            } else { console.error("Error sending data:", error); updateLastMessage("Sorry, I'm having trouble connecting."); }
        } finally { abortControllerRef.current = null; setIsWaitingForResponse(false); }
    };

    const synthesizeAndPlay = useCallback((text) => {
        if (!text || typeof text !== 'string') return;
        try {
            if ('speechSynthesis' in window) {
                window.speechSynthesis.cancel();
                const utterance = new SpeechSynthesisUtterance(text);
                utterance.onstart = () => setIsBotSpeaking(true);
                utterance.onend = () => setIsBotSpeaking(false);
                utterance.onerror = () => setIsBotSpeaking(false);
                window.speechSynthesis.speak(utterance);
            }
        } catch (error) { console.error("Browser TTS Error:", error); setIsBotSpeaking(false); }
    }, []);
    
    const stopTts = () => { window.speechSynthesis.cancel(); setIsBotSpeaking(false); };
    
    const stopWakeWordListener = useCallback(() => {
        if (recognitionRef.current) {
            recognitionRef.current.stop();
        }
    }, []);

    const startWakeWordListener = useCallback(() => {
        if (recognitionRef.current) {
            try {
                recognitionRef.current.start();
                setStatus("Listening for 'Mitra'...");
            } catch (e) { /* Already started, which is fine */ }
        }
    }, []);

    const stopRecording = useCallback(() => {
        if (mediaRecorderRef.current && mediaRecorderRef.current.state === "recording") {
            mediaRecorderRef.current.stop();
        }
        setIsRecording(false);
    }, []);

    const startRecording = useCallback(() => {
        if (!audioStreamRef.current) { setStatus("Error: Cannot access microphone."); return; }
        stopWakeWordListener();
        stopTts();
        
        try {
            mediaRecorderRef.current = new MediaRecorder(audioStreamRef.current);
            const audioChunks = [];
            mediaRecorderRef.current.ondataavailable = (event) => audioChunks.push(event.data);
            mediaRecorderRef.current.onstop = () => {
                const audioBlob = new Blob(audioChunks, { type: 'audio/webm' });
                if (audioBlob.size > 1000) {
                    const formData = new FormData();
                    formData.append('voice', audioBlob, 'recording.webm');
                    addMessage("🎤 Voice query sent", "user");
                    sendData(formData);
                }
                startWakeWordListener();
            };
            
            mediaRecorderRef.current.start();
            setIsRecording(true);
            setStatus("Recording your query... Click stop when done.");
        } catch (error) {
            console.error("MediaRecorder error:", error);
            setStatus("Recording failed. Please try again.");
        }
    }, [startWakeWordListener, stopWakeWordListener]);

    useEffect(() => {
        async function initializeMicrophone() {
            try {
                const stream = await navigator.mediaDevices.getUserMedia({ 
                    audio: {
                        echoCancellation: true,
                        noiseSuppression: true,
                        sampleRate: 44100
                    }
                });
                audioStreamRef.current = stream;
                const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
                if (!SpeechRecognition) { setStatus("Browser does not support voice commands."); return; }
                
                const recognition = new SpeechRecognition();
                recognition.continuous = true;
                recognition.interimResults = true;
                recognition.lang = 'en-IN';
                recognitionRef.current = recognition;

                recognition.onresult = (event) => {
                    for (let i = event.resultIndex; i < event.results.length; ++i) {
                        if (event.results[i].isFinal) {
                            const transcript = event.results[i][0].transcript.toLowerCase().trim();
                            console.log("[HEARD FINAL TEXT]:", transcript);
                            if (transcript.includes("mitra") && !isRecordingRef.current) {
                                setStatus("'Mitra' detected! Recording...");
                                startRecording();
                            }
                        }
                    }
                };
                
                // This handler now only manages unexpected stops.
                recognition.onend = () => {
                    if (!isRecordingRef.current) {
                       startWakeWordListener();
                    }
                };
                recognition.onerror = (event) => {
                     if (event.error !== 'no-speech' && event.error !== 'aborted') {
                       console.error("Speech recognition error", event.error);
                       setStatus("Voice command error. Please refresh.");
                    }
                };
                
                addMessage("Hello! I am Mitra, your AI Agri Assistant. Ask me about weather, market prices, or upload a leaf image.", "bot");
                startWakeWordListener();
                
            } catch (err) {
                console.error("Microphone access failed:", err);
                setStatus("Microphone permission denied. Please allow and refresh.");
            }
        }
        initializeMicrophone();
        return () => {
            if (recognitionRef.current) { recognitionRef.current.abort(); }
            if (audioStreamRef.current) {
                audioStreamRef.current.getTracks().forEach(track => track.stop());
            }
        };
    }, [startRecording, startWakeWordListener]);

    const handleMicClick = () => { isRecording ? stopRecording() : startRecording(); };
    const handleSendMessage = (text) => { cancelPreviousRequest(); stopTts(); addMessage(text, 'user'); const formData = new FormData(); formData.append('text', text); sendData(formData); };
    const handleImageUpload = (file) => { cancelPreviousRequest(); stopTts(); addMessage(`🖼️ Image uploaded: ${file.name}`, 'user'); const formData = new FormData(); formData.append('image', file); sendData(formData); };

    return (
        <div className="chat-container">
            <div className="chat-header">AI Agri Assistant (Mitra)</div>
            <div className="status-bar">{status}</div>
            <ChatWindow messages={messages} />
            <InputBar 
                onSendMessage={handleSendMessage} onImageUpload={handleImageUpload} onMicClick={handleMicClick}
                isRecording={isRecording} isTtsEnabled={isTtsEnabled} onTtsToggle={() => setIsTtsEnabled(prev => !prev)}
                isBotSpeaking={isBotSpeaking} onStopTts={stopTts}
                isWaitingForResponse={isWaitingForResponse} onCancelRequest={cancelPreviousRequest}
            />
        </div>
    );
}

export default App;