<div align="center">

# 🌿 Mitra – AI Agri Assistant

**An intelligent, multimodal agricultural assistant that helps Indian farmers with crop disease detection, live market prices, weather advisories, and natural language Q&A — all accessible via voice or text.**

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Python](https://img.shields.io/badge/Python-3.10%2B-blue.svg)](https://www.python.org/)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)
[![React](https://img.shields.io/badge/React-19-61DAFB.svg)](https://react.dev/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F.svg)](https://spring.io/projects/spring-boot)

</div>

---

## 📖 Project Overview

**Mitra** (meaning *friend* in Hindi) is a full-stack, AI-powered agricultural assistant designed for Indian farmers. It understands voice, text, and images — letting a farmer simply say *"Mitra"* to activate it, ask a question naturally, or photo a diseased leaf to get an instant diagnosis and crop advice.

The system is built as a **microservice architecture**: a central Spring Boot backend orchestrates four independent AI services (each in Python/Flask) and serves a React frontend.

---

## 🧠 AI Architecture — How Each Model Works

### 🌿 1. Plant Disease Detection — MobileNetV2 (Transfer Learning)

**Technology:** TensorFlow · Keras · MobileNetV2 · PlantVillage Dataset

The disease detection model is built using **transfer learning** on top of Google's **MobileNetV2**, a lightweight yet powerful convolutional neural network (CNN) pre-trained on **ImageNet** (1.4 million images, 1000 classes). Instead of training from scratch, we:

1. **Load the pretrained MobileNetV2** (without top classification layer) from `tf.keras.applications`
2. **Freeze the base layers** — the feature extraction knowledge from ImageNet is retained
3. **Add a custom classification head** — a `GlobalAveragePooling2D` → `Dense(128, relu)` → `Dense(15, softmax)` stack
4. **Fine-tune** the full model on the **PlantVillage dataset** (plant leaf images with disease labels)
5. The resulting trained weights are **exported as `mobilenet_model.keras`** (~27 MB)

The fine-tuned model classifies **15 disease/health classes** across Pepper, Potato, and Tomato crops with high accuracy. At runtime, the Flask API at `:5002` accepts an uploaded image, preprocesses it to `224×224`, normalizes pixel values to `[0, 1]`, runs inference, and returns the predicted class + confidence score.

```
Farmer uploads leaf photo
         ↓
Image/app.py (Flask :5002)
  → preprocess: resize to 224×224, normalize /255
  → model.predict() using fine-tuned MobileNetV2
  → returns { predicted_class, confidence }
         ↓
Spring Boot → TextAI → FLAN-T5 generates crop advice
```

---

### 🎤 2. Voice Transcription — OpenAI Whisper

**Technology:** OpenAI Whisper (medium model) · pydub · Flask

For voice input, we use **OpenAI's Whisper**, an open-source automatic speech recognition (ASR) model trained on 680,000 hours of multilingual audio. The **medium** variant is used, which runs locally (no API key required) and handles Indian-accented English and regional language inputs well.

**How it works:**

1. The React frontend captures audio via the **MediaRecorder API** and sends the `.webm` audio blob to Spring Boot
2. Spring Boot forwards it to the Voice Flask service at `:5001`
3. **pydub** converts the audio to mono 16 kHz WAV (the format Whisper expects)
4. Whisper performs **multilingual transcription** with automatic language detection
5. The transcribed text is returned to Spring Boot, which then routes it as a text query

```
User speaks → MediaRecorder captures .webm
         ↓
Voice/app.py (Flask :5001)
  → pydub converts to mono 16kHz WAV
  → whisper.transcribe() → text + detected language
         ↓
Spring Boot routes transcribed text → same as text query
```

> **Wake-word activation:** The frontend also uses the browser's built-in **Web Speech Recognition API** to continuously listen for the word *"Mitra"*. When detected, it automatically starts recording with MediaRecorder — no button press needed.

---

### 💬 3. Text Q&A / Agriculture Advisory — FLAN-T5-Large

**Technology:** Hugging Face Transformers · Google FLAN-T5-Large · PyTorch · Flask

For answering natural language agricultural questions, Mitra uses **Google's FLAN-T5-Large**, a 780M parameter encoder-decoder language model (Seq2Seq) from Hugging Face. FLAN-T5 is instruction-tuned on thousands of NLP tasks, including Q&A, making it well-suited for domain-specific advisory responses.

**How it works:**

1. Any text query (from voice transcription, direct text, or image diagnosis results) is formatted as:
   ```
   Question: <user's query>
   Answer:
   ```
2. The Flask API at `:5000` tokenizes the input with `AutoTokenizer` and runs `model.generate()` with  **beam search** (5 beams, no sampling) for deterministic, high-quality responses
3. The decoded response text is returned as the agricultural advisory
4. The model is loaded from Hugging Face Hub on first run (`google/flan-t5-large`) and cached locally

```
Query text → text/app.py (Flask :5000)
  → Tokenize: "Question: {query}\nAnswer:"
  → model.generate(num_beams=5, max_length=128)
  → Decode → { agriq_advisory }
```

---

### 🌦️ 4. Weather Advisory — OpenWeatherMap API + Spring Boot

**Technology:** OpenWeatherMap REST API · Spring Boot · RestTemplate · FLAN-T5

Weather data is fetched from the **OpenWeatherMap API** by a dedicated Spring Boot service (`Weather/demo/`, running at `:8080`). It returns live temperature, humidity, and weather conditions for any given district.

The clever part: raw weather numbers alone aren't useful to a farmer. So the data is **passed to FLAN-T5** with a structured prompt like:

```
"You are Mitra, a friendly farm advisor. Give a short, conversational weather update
for a farmer growing {crop} in {district}. Temperature is {temp}°C, humidity is {humidity}%,
conditions are '{condition}'. Incorporate this advice: '{cropAdvice}'.
Combine into one smooth, helpful paragraph."
```

FLAN-T5 then synthesizes this raw data into a natural, farmer-friendly weather advisory.

---

### 📊 5. Live Market Prices — data.gov.in (Agmarknet API)

**Technology:** Flask · data.gov.in REST API · Agmarknet Dataset

The market price service (`agmarknet.py`, `:5004`) calls the **Government of India's open data API** ([data.gov.in](https://data.gov.in)) which provides real-time mandi (wholesale market) prices from the **Agmarknet** database across all Indian states.

Queries support filtering by state, district, market, and commodity. For specific queries (market + commodity), it returns the latest day's modal price. For broad queries (just a state), it returns a conversational summary of top active markets and commodities.

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                chatbot-ui  (React 19 + Vite)                    │
│  Wake-word detection → MediaRecorder → Chat UI → Browser TTS   │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP Multipart / JSON  (port 8082)
┌──────────────────────────▼──────────────────────────────────────┐
│            chatbot/   Spring Boot  — Orchestrator               │
│                                                                  │
│  ChatbotController — intent routing:                            │
│    "weather" → WeatherService   "price/market" → MarketService  │
│    image uploaded → ImageAIService → TextAIService              │
│    voice uploaded → VoiceAIService → processTextRequest()       │
│    text → TextAIService                                         │
└──┬─────────────────┬──────────────────┬───────────────┬─────────┘
   │ :5000           │ :5001            │ :5002         │ :5004
   ▼                 ▼                  ▼               ▼
text/app.py     Voice/app.py      Image/app.py    agmarknet.py
FLAN-T5-Large   Whisper (medium)  MobileNetV2     data.gov.in API
(Hugging Face)  (OpenAI OSS)      (PlantVillage)  (Agmarknet)
                                                        ↑
                                       WeatherService → :8080
                                       Weather/demo (Spring Boot)
                                       → OpenWeatherMap API
```

| Component | Technology | Port |
|---|---|---|
| Frontend UI | React 19 + Vite + Web Speech API | 5173 |
| Chatbot Orchestrator | Spring Boot 3.5 + Java 17 | 8082 |
| Text Q&A (FLAN-T5) | Flask + Hugging Face Transformers | 5000 |
| Voice STT (Whisper) | Flask + OpenAI Whisper medium | 5001 |
| Disease Detection | Flask + TensorFlow + MobileNetV2 | 5002 |
| Market Prices | Flask + data.gov.in Agmarknet API | 5004 |
| Weather Advisory | Spring Boot + OpenWeatherMap API | 8080 |

---

## ⚛️ Frontend — React + Browser-Native APIs

The UI is built with **React 19 + Vite** and uses **zero external AI SDKs** on the frontend — all AI calls go through the Spring Boot backend. Two browser-native APIs power the voice experience:

| Browser API | Role |
|---|---|
| **Web Speech Recognition API** (`SpeechRecognition`) | Continuous background listening for the wake-word *"Mitra"* |
| **MediaRecorder API** | Records user's voice query as `.webm` audio after wake-word detected |
| **SpeechSynthesis API** (`window.speechSynthesis`) | Reads bot responses aloud (Text-to-Speech) — zero external TTS service needed |

This means Mitra's entire voice I/O pipeline — wake-word → record → transcribe (Whisper) → respond → speak back (TTS) — works **entirely offline on the browser side** except for Whisper transcription.

---

## 🚀 Getting Started

### Prerequisites

| Tool | Minimum Version |
|---|---|
| Python | 3.10+ |
| Java JDK | 17 |
| Maven | 3.8+ |
| Node.js | 18+ |
| FFmpeg | Any (required by pydub/Whisper) |

> A CUDA-capable GPU is recommended for the `text/` (FLAN-T5) and `Voice/` (Whisper) services but CPU mode works too.

---

### 1️⃣ Clone the Repository

```bash
git clone https://github.com/hariprasanth5002/Mitra-AI-Agri-Assistant.git
cd Mitra-AI-Agri-Assistant
```

---

### 2️⃣ Start Python Microservices

Each has its own `requirements.txt`.

#### 💬 Text Q&A — FLAN-T5 (port 5000)
```bash
cd text
python -m venv venv && venv\Scripts\activate
pip install -r requirements.txt
python app.py
# FLAN-T5-Large (~3 GB) downloads automatically from Hugging Face on first run
```

#### 🎤 Voice Transcription — Whisper (port 5001)
> Requires [FFmpeg](https://ffmpeg.org/download.html) on your PATH.

```bash
cd Voice
python -m venv venv && venv\Scripts\activate
pip install -r requirements.txt
python app.py
# Whisper medium (~1.5 GB) downloads automatically on first run
```

#### 🌿 Disease Detection — MobileNetV2 (port 5002)
> Place `mobilenet_model.keras` in the `Image/` folder (see [Model Downloads](#-model-downloads) below).
> Update the model path in `Image/app.py` if needed.

```bash
cd Image
python -m venv venv && venv\Scripts\activate
pip install -r requirements.txt
python app.py
```

#### 📊 Market Prices — Agmarknet (port 5004)
```bash
# From project root
pip install flask requests
python agmarknet.py
```

---

### 3️⃣ Start Spring Boot Services

#### Chatbot Orchestrator (port 8082)
```bash
cd chatbot/chatbot
mvnw.cmd spring-boot:run
```

#### Weather Advisory (port 8080)
```bash
cd Weather/demo
mvnw.cmd spring-boot:run
```

---

### 4️⃣ Start the React Frontend (port 5173)

```bash
cd chatbot-ui
npm install
npm run dev
```

Open **http://localhost:5173** in your browser. Allow microphone access when prompted.

---

## 📁 Project Structure

```
Mitra – AI Agri Assistant/
│
├── agmarknet.py              # 📊 Market price Flask API (port 5004)
├── requirements.txt          # Python deps for agmarknet.py
│
├── Image/                    # 🌿 Plant Disease Detection Service
│   ├── app.py                #   Flask API (port 5002)
│   ├── requirements.txt      #   TensorFlow + Flask deps
│   └── mobilenet_model.keras #   Fine-tuned MobileNetV2 weights (see below)
│
├── Voice/                    # 🎤 Voice Transcription Service
│   ├── app.py                #   Flask API (port 5001)
│   └── requirements.txt      #   Whisper + pydub deps
│
├── text/                     # 💬 Text Q&A Service
│   ├── app.py                #   Flask API (port 5000), FLAN-T5-Large
│   └── requirements.txt      #   PyTorch + Transformers deps
│
├── chatbot/chatbot/          # ☕ Spring Boot Orchestrator (port 8082)
│   ├── pom.xml               #   Maven: Spring Web, WebFlux, JPA, MySQL
│   └── src/main/java/.../
│       ├── controller/
│       │   └── ChatbotController.java  # Intent routing (image/voice/text/weather/market)
│       └── service/
│           ├── ImageAIService.java     # Calls Image Flask service
│           ├── VoiceAIService.java     # Calls Voice Flask service
│           ├── TextAIService.java      # Calls Text Flask service (FLAN-T5)
│           ├── WeatherService.java     # Calls Weather service → FLAN-T5
│           └── MarketService.java      # Calls Agmarknet Flask service
│
├── chatbot-ui/               # ⚛️  React + Vite Frontend (port 5173)
│   └── src/
│       ├── App.jsx           #   Wake-word, voice recording, TTS, chat state
│       └── components/       #   ChatWindow, InputBar components
│
└── Weather/
    ├── demo/                 # 🌦️ Weather Spring Boot API (port 8080, OpenWeatherMap)
    └── weather-advisory-frontend/  # Standalone React weather demo
```

---

## 📦 Model Downloads

| Model | Size | How it's obtained |
|---|---|---|
| `mobilenet_model.keras` | ~27 MB | **Download separately** — see below |
| Whisper `medium` | ~1.5 GB | Auto-downloaded by `whisper` on first run |
| FLAN-T5-Large | ~3 GB | Auto-downloaded via Hugging Face on first run |

### Getting the MobileNetV2 Model

The `.keras` file is tracked with **Git LFS**. After cloning, run:
```bash
git lfs pull
```
If Git LFS is not set up, download the model directly from the [Releases page](../../releases) and place it at `Image/mobilenet_model.keras`.

---

## 🔑 Configuration

| Setting | Location | Notes |
|---|---|---|
| Agmarknet API Key | `agmarknet.py` → `API_KEY` | Free key from [data.gov.in](https://data.gov.in) |
| OpenWeatherMap API Key | `Weather/demo/src/main/resources/application.properties` | Free key from [openweathermap.org](https://openweathermap.org) |
| Default crop / district | `chatbot/chatbot/src/.../application.properties` | Used when not detected in query |
| Whisper model size | `Voice/app.py` → `whisper.load_model("medium")` | Options: tiny, base, small, medium, large |
| FLAN-T5 variant | `text/app.py` → `model_name = "google/flan-t5-large"` | Can change to `flan-t5-xl` for more power |

> 🔐 Never commit real API keys to git. Move them to a `.env` file and load with `python-dotenv`.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🎤 Wake-word activation | Say **"Mitra"** — browser listens in background, auto-starts recording |
| 📷 Leaf disease scan | Upload a leaf photo → MobileNetV2 diagnosis → FLAN-T5 advice |
| 📊 Live market prices | Real-time mandi prices via Agmarknet / data.gov.in |
| 💬 Agri Q&A | Any farming question answered by FLAN-T5-Large |
| 🌦️ Weather advisory | District weather → FLAN-T5 generates crop-specific advice |
| 🔊 Text-to-Speech | Bot replies read aloud via browser's native `SpeechSynthesis` API |
| 🚫 Cancel / Stop | Cancel in-flight requests or stop TTS mid-sentence |

---

## 🛠️ Full Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 19, Vite 7, Web Speech API, MediaRecorder API, SpeechSynthesis API |
| Orchestrator Backend | Spring Boot 3.5, Java 17, Spring WebFlux, Spring Data JPA |
| Database | MySQL 8 (chat history via Spring Data JPA) |
| Text AI | FLAN-T5-Large (Hugging Face), PyTorch, Flask |
| Voice STT | OpenAI Whisper (medium), pydub, Flask |
| Image AI | TensorFlow 2.16, MobileNetV2 (ImageNet → PlantVillage fine-tune), Flask |
| Market Data | data.gov.in Agmarknet REST API, Flask |
| Weather Data | OpenWeatherMap API, Spring Boot RestTemplate |

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

---

## 📄 License

Distributed under the **MIT License**. See [LICENSE](LICENSE) for details.

---

## 👤 Author

**Hariprasanth** — [GitHub](https://github.com/hariprasanth5002)

---

<div align="center">
Made with ❤️ for Indian farmers 🌾
</div>
