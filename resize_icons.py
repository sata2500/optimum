import os
from PIL import Image

def resize_image(input_path, output_path, size):
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with Image.open(input_path) as img:
        img = img.resize(size, Image.Resampling.LANCZOS)
        img.save(output_path, "PNG")

base_img = r"C:\Users\salih\.gemini\antigravity\brain\a6c8972a-08a3-4e9d-be7d-4a44e8e37026\app_logo.png"
out_dir = r"d:\Projelerim\optimum\app\src\main\res"

sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

# Generate legacy launcher icons
for folder, size in sizes.items():
    resize_image(base_img, os.path.join(out_dir, folder, "ic_launcher.png"), (size, size))
    resize_image(base_img, os.path.join(out_dir, folder, "ic_launcher_round.png"), (size, size))

# Generate adaptive icon foreground (108dp = 432px for xxxhdpi)
resize_image(base_img, os.path.join(out_dir, "mipmap-xxxhdpi", "ic_launcher_foreground.png"), (432, 432))

print("Icons generated successfully!")
