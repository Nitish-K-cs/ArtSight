import torch
import clip
from PIL import Image
from pinecone import Pinecone

PINECONE_API_KEY = "pcsk_3gLRzk_PSCVZceSQxkFiYcyc5QF37FkoMAyxzC55QVsdY2zE3c7G2Kf13ujZ1bFtgunuFL"
PINECONE_INDEX_NAME = "artsight-artworks"

device = "cuda" if torch.cuda.is_available() else "cpu"
model, preprocess = clip.load("ViT-B/32", device=device)

pc = Pinecone(api_key=PINECONE_API_KEY)
index = pc.Index(PINECONE_INDEX_NAME)

# Embed the query image
image = preprocess(Image.open("test3.jpg")).unsqueeze(0).to(device)
with torch.no_grad():
    embedding = model.encode_image(image)

query_vector = embedding[0].cpu().numpy().tolist()

# Search Pinecone for the closest matches
results = index.query(
    vector=query_vector,
    top_k=5,
    include_metadata=True
)

print("\nTop matches:\n")
for match in results["matches"]:
    print(f"Score: {match['score']:.4f} | {match['metadata']['title']} by {match['metadata']['artist']}")