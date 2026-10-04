import torch
import clip
from PIL import Image

device = "cuda" if torch.cuda.is_available() else "cpu"
print(f"Using device: {device}")

model, preprocess = clip.load("ViT-B/32", device=device)

image = preprocess(Image.open("test.jpg")).unsqueeze(0).to(device)

with torch.no_grad():
    image_features = model.encode_image(image)

print(f"Embedding shape: {image_features.shape}")
print(f"First 5 values: {image_features[0][:5]}")