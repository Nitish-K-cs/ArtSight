import torch
import clip
import psycopg2
from PIL import Image
import requests
from io import BytesIO
from pinecone import Pinecone

# --- Config ---
PG_CONFIG = {
    "host": "localhost",
    "port": 5432,
    "dbname": "artsight",
    "user": "postgres",
    "password": "kannan@123"
}
PINECONE_API_KEY = "pcsk_3gLRzk_PSCVZceSQxkFiYcyc5QF37FkoMAyxzC55QVsdY2zE3c7G2Kf13ujZ1bFtgunuFL"
PINECONE_INDEX_NAME = "artsight-artworks"

# --- Setup ---
device = "cuda" if torch.cuda.is_available() else "cpu"
print(f"Using device: {device}")

model, preprocess = clip.load("ViT-B/32", device=device)

pc = Pinecone(api_key=PINECONE_API_KEY)
index = pc.Index(PINECONE_INDEX_NAME)

conn = psycopg2.connect(**PG_CONFIG)
cur = conn.cursor()
cur.execute("SELECT id, title, artist, image_url FROM artworks")
rows = cur.fetchall()
print(f"Found {len(rows)} artworks to embed")

# --- Embed + upsert ---
success_count = 0
for artwork_id, title, artist, image_url in rows:
    try:
        response = requests.get(image_url, timeout=10)
        image = Image.open(BytesIO(response.content)).convert("RGB")
        image_input = preprocess(image).unsqueeze(0).to(device)

        with torch.no_grad():
            embedding = model.encode_image(image_input)

        vector = embedding[0].cpu().numpy().tolist()

        index.upsert(vectors=[{
            "id": str(artwork_id),
            "values": vector,
            "metadata": {"title": title, "artist": artist}
        }])

        success_count += 1
        print(f"Embedded [{artwork_id}] {title} by {artist}")

    except Exception as e:
        print(f"Failed on [{artwork_id}] {title}: {e}")

print(f"\nDone. {success_count}/{len(rows)} artworks embedded and upserted.")

cur.close()
conn.close()