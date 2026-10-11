import torch
import clip
from PIL import Image
from io import BytesIO
from flask import Flask, request, jsonify
from pinecone import Pinecone

PINECONE_API_KEY = "pcsk_3gLRzk_PSCVZceSQxkFiYcyc5QF37FkoMAyxzC55QVsdY2zE3c7G2Kf13ujZ1bFtgunuFL"
PINECONE_INDEX_NAME = "artsight-artworks"
CONFIDENCE_THRESHOLD = 0.5

app = Flask(__name__)

device = "cuda" if torch.cuda.is_available() else "cpu"
print(f"Loading CLIP on {device}...")
model, preprocess = clip.load("ViT-B/32", device=device)

pc = Pinecone(api_key=PINECONE_API_KEY)
index = pc.Index(PINECONE_INDEX_NAME)
print("Ready.")


@app.route("/recognize", methods=["POST"])
def recognize():
    file = request.files["file"]
    image = Image.open(BytesIO(file.read())).convert("RGB")
    image_input = preprocess(image).unsqueeze(0).to(device)

    with torch.no_grad():
        embedding = model.encode_image(image_input)

    query_vector = embedding[0].cpu().numpy().tolist()

    results = index.query(vector=query_vector, top_k=5, include_metadata=True)
    matches = results["matches"]

    if not matches or matches[0]["score"] < CONFIDENCE_THRESHOLD:
        return jsonify({"matched": False, "message": "No confident match found"})

    top = matches[0]
    return jsonify({
        "matched": True,
        "artwork_id": int(top["id"]),
        "score": top["score"],
        "title": top["metadata"]["title"],
        "artist": top["metadata"]["artist"]
    })


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000)