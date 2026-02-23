import torch
from flask import Flask, request, jsonify
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
import threading
import sys

# ------------------------------------------------
# Step 1: Flask initialization
# ------------------------------------------------
app = Flask(__name__)

model = None
tokenizer = None
device = "cuda" if torch.cuda.is_available() else "cpu"

# ------------------------------------------------
# Step 2: Load model in background (NON-daemon)
# ------------------------------------------------
def load_model():
    global model, tokenizer
    print("🔄 Loading AgriQBot model...")

    try:
        model_name = "mrSoul7766/AgriQBot"

        tokenizer = AutoTokenizer.from_pretrained(model_name)
        model = AutoModelForSeq2SeqLM.from_pretrained(model_name)

        model.to(device)
        model.eval()

        print("✅ AgriQBot model loaded successfully")

    except Exception as e:
        print(f"❌ Model loading failed: {e}", file=sys.stderr)

# IMPORTANT: do NOT use daemon=True
threading.Thread(target=load_model).start()

# ------------------------------------------------
# Step 3: Routes
# ------------------------------------------------
@app.route("/", methods=["GET"])
def index():
    return jsonify({"status": "AgriQBot API is running"})


@app.route("/generate", methods=["POST"])
def generate_text():
    global model, tokenizer

    if model is None or tokenizer is None:
        return jsonify({"error": "Model is still loading"}), 503

    # Safely parse JSON
    data = request.get_json(silent=True) or {}

    # Accept prompt / text / query (Spring-safe)
    user_prompt = data.get("prompt") or data.get("text") or data.get("query")
    if not user_prompt:
        return jsonify({
            "error": "Missing prompt/text/query",
            "example": { "text": "What fertilizer is best for rice crop?" }
        }), 400

    max_length = data.get("max_length", 128)

    # 🔹 IMPORTANT: format prompt for Seq2Seq QA model
    prompt = f"Question: {user_prompt}\nAnswer:"

    try:
        inputs = tokenizer(
            prompt,
            return_tensors="pt",
            truncation=True,
            padding=True
        ).to(device)

        with torch.no_grad():
            outputs = model.generate(
                input_ids=inputs["input_ids"],
                attention_mask=inputs["attention_mask"],
                max_length=max_length,
                num_beams=5,
                do_sample=False,
                temperature=0.7,
                repetition_penalty=1.2
            )

        response_text = tokenizer.decode(
            outputs[0],
            skip_special_tokens=True
        ).strip()

        # ------------------------------------------------
        # RETURN FORMAT EXPECTED BY SPRING BACKEND
        # ------------------------------------------------
        return jsonify({
            "agriq_advisory": response_text,
            "rule_based": "",
            "masked_predictions": []
        })

    except Exception as e:
        print(f"❌ Inference error: {e}", file=sys.stderr)
        return jsonify({"error": "Inference failed"}), 500


# ------------------------------------------------
# Step 4: Run Flask
# ------------------------------------------------
if __name__ == "__main__":
    print("=" * 55)
    print("🚀 Starting AgriQBot Flask API")
    print("📍 URL: http://127.0.0.1:5004")
    print("📌 Endpoint: POST /generate")
    print("=" * 55)

    app.run(host="0.0.0.0", port=5004)
