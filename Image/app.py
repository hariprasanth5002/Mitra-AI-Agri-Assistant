from flask import Flask, request, jsonify
from flask_cors import CORS  # ✅ Import this
from tensorflow.keras.models import load_model
from tensorflow.keras.preprocessing import image
import numpy as np
import os

app = Flask(__name__)
CORS(app)  # ✅ Enable CORS

# Load the trained model once when the app starts
model = load_model('C:/Users/LENOVO/Downloads/archive/PlantVillage/PlantVillage/mobilenet_model.keras')

# Class labels based on your dataset
class_labels = {
    0: 'Pepper__bell___Bacterial_spot',
    1: 'Pepper__bell___healthy',
    2: 'Potato___Early_blight',
    3: 'Potato___Late_blight',
    4: 'Potato___healthy',
    5: 'Tomato_Bacterial_spot',
    6: 'Tomato_Early_blight',
    7: 'Tomato_Late_blight',
    8: 'Tomato_Leaf_Mold',
    9: 'Tomato_Septoria_leaf_spot',
    10: 'Tomato_Spider_mites_Two_spotted_spider_mite',
    11: 'Tomato__Target_Spot',
    12: 'Tomato__Tomato_YellowLeaf__Curl_Virus',
    13: 'Tomato__Tomato_mosaic_virus',
    14: 'Tomato_healthy'
}

# Function to preprocess the image
def preprocess_image(img_path, target_size=(224, 224)):
    img = image.load_img(img_path, target_size=target_size)
    img_array = image.img_to_array(img)
    img_array = img_array / 255.0  # Normalize
    img_array = np.expand_dims(img_array, axis=0)
    return img_array

# Function to predict the image
def predict(img_path):
    img = preprocess_image(img_path)
    prediction = model.predict(img)
    class_index = np.argmax(prediction, axis=1)[0]
    class_name = class_labels.get(class_index, "Unknown")
    confidence = float(prediction[0][class_index])
    return class_name, confidence

# API route for prediction
@app.route('/predict', methods=['POST'])
def predict_api():
    if 'file' not in request.files:
        return jsonify({"error": "No file part"}), 400

    file = request.files['file']

    if file.filename == '':
        return jsonify({"error": "No selected file"}), 400

    # Save the file temporarily
    file_path = os.path.join('uploads', file.filename)
    os.makedirs('uploads', exist_ok=True)
    file.save(file_path)

    # Run prediction
    result, confidence = predict(file_path)

    # Optionally, delete the file after processing
    os.remove(file_path)

    return jsonify({
        "predicted_class": result,
        "confidence": round(confidence, 4)
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5002, debug=True)

