import os
import urllib.request

font_dir = r"d:\DACS3\app\src\main\res\font"
os.makedirs(font_dir, exist_ok=True)

# We will download a few weights of Inter from Google Fonts Github
fonts = {
    "inter_regular.ttf": "https://raw.githubusercontent.com/google/fonts/main/ofl/inter/static/Inter-Regular.ttf",
    "inter_medium.ttf": "https://raw.githubusercontent.com/google/fonts/main/ofl/inter/static/Inter-Medium.ttf",
    "inter_semibold.ttf": "https://raw.githubusercontent.com/google/fonts/main/ofl/inter/static/Inter-SemiBold.ttf",
    "inter_bold.ttf": "https://raw.githubusercontent.com/google/fonts/main/ofl/inter/static/Inter-Bold.ttf",
}

for filename, url in fonts.items():
    filepath = os.path.join(font_dir, filename)
    print(f"Downloading {filename}...")
    urllib.request.urlretrieve(url, filepath)

print("Fonts downloaded successfully.")
