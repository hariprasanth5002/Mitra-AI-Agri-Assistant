from flask import Flask, request, jsonify
from flask_cors import CORS
from werkzeug.utils import secure_filename
import os, uuid, traceback
from pydub import AudioSegment
import whisper

# ---- CONFIG ----
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
UPLOAD_FOLDER = os.path.join(BASE_DIR, "uploads")
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# ---- APP ----
app = Flask(__name__)
CORS(app)

# ---- Load Whisper model ----
print("Loading Whisper model...")
model = whisper.load_model("medium")  # small, base, medium, large
print("Whisper model loaded.")

# ---- Helpers ----
def convert_to_wav(input_path):
    """Convert any audio to mono 16kHz WAV using pydub."""
    out_path = os.path.join(UPLOAD_FOLDER, f"conv_{uuid.uuid4().hex}.wav")
    audio = AudioSegment.from_file(input_path)
    audio = audio.set_channels(1).set_frame_rate(16000)
    audio.export(out_path, format="wav")
    return out_path

# ---- Routes ----
@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "model_loaded": True})

@app.route("/transcribe", methods=["POST"])
def transcribe():
    try:
        if "file" not in request.files:
            return jsonify({"error": "No file uploaded"}), 400

        f = request.files["file"]
        if f.filename == "":
            return jsonify({"error": "Empty filename"}), 400

        # Save uploaded file
        filename = secure_filename(f.filename)
        in_path = os.path.join(UPLOAD_FOLDER, f"upload_{uuid.uuid4().hex}_{filename}")
        f.save(in_path)

        # Convert to WAV
        wav_path = convert_to_wav(in_path)

        # Transcribe with Whisper (auto language detection)
        result = model.transcribe(wav_path)

        # Cleanup
        try:
            os.remove(in_path)
            os.remove(wav_path)
        except:
            pass

        return jsonify({
            "detected_language": result.get("language", "unknown"),
            "lang_confidence": round(result.get("language_probability", 0.0), 3),
            "initial_text": result.get("text", ""),
            "final_transcription": result.get("text", "")
        })

    except Exception as e:
        traceback.print_exc()
        return jsonify({"error": "server_exception", "details": str(e)}), 500

# ---- Run ----
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001, debug=True)
