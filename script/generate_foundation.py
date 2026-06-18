import json
import os

assets_dir = r"d:\DACS3\app\src\main\assets\foundation"
os.makedirs(assets_dir, exist_ok=True)

# 1. numbers.json
numbers = []
# Basic 1-10
basics = {1: "いち", 2: "に", 3: "さん", 4: "よん", 5: "ご", 6: "ろく", 7: "なな", 8: "はち", 9: "きゅう", 10: "じゅう"}
for k, v in basics.items():
    numbers.append({"number": str(k), "reading": v, "romaji": "", "isException": False})

# Exceptions
exceptions = {
    "300": "さんびゃく", "600": "ろっぴゃく", "800": "はっぴゃく", 
    "3000": "さんぜん", "8000": "はっせん"
}
for k, v in exceptions.items():
    numbers.append({"number": str(k), "reading": v, "romaji": "", "isException": True})

with open(os.path.join(assets_dir, "numbers.json"), "w", encoding="utf-8") as f:
    json.dump(numbers, f, ensure_ascii=False, indent=2)

# 2. time.json
time_data = {"hours": [], "minutes": []}
for h in range(1, 13):
    is_ex = h in [4, 7, 9]
    reading = f"{basics.get(h, str(h))}じ"
    if h == 4: reading = "よじ"
    elif h == 7: reading = "しちじ"
    elif h == 9: reading = "くじ"
    time_data["hours"].append({"value": h, "reading": reading, "isException": is_ex})

for m in range(1, 61):
    # Just generating some, mostly exceptions
    is_ex = m % 10 in [1, 3, 4, 6, 8, 0] # pun instead of fun
    # simplify data
    time_data["minutes"].append({"value": m, "isException": is_ex})

with open(os.path.join(assets_dir, "time.json"), "w", encoding="utf-8") as f:
    json.dump(time_data, f, ensure_ascii=False, indent=2)

# 3. calendar.json
calendar = {"days": [], "months": [], "weekdays": []}
days_ex = {
    1: "ついたち", 2: "ふつか", 3: "みっか", 4: "よっか", 5: "いつか",
    6: "むいか", 7: "なのか", 8: "ようか", 9: "ここのか", 10: "とおか",
    14: "じゅうよっか", 20: "はつか", 24: "にじゅうよっか"
}
for d in range(1, 32):
    if d in days_ex:
        calendar["days"].append({"value": d, "reading": days_ex[d], "isException": True})
    else:
        calendar["days"].append({"value": d, "reading": f"{d}にち", "isException": False})

with open(os.path.join(assets_dir, "calendar.json"), "w", encoding="utf-8") as f:
    json.dump(calendar, f, ensure_ascii=False, indent=2)

print("Foundation JSON files created.")
