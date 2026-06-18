import sqlite3
import json
import os

db_path = r"d:\DACS3\n5_nihongo.db"
assets_path = r"d:\DACS3\app\src\main\assets\lessons"

# Remove existing database to ensure a clean export
if os.path.exists(db_path):
    try:
        os.remove(db_path)
    except Exception as e:
        print(f"Error removing old db: {e}")

conn = sqlite3.connect(db_path)
cursor = conn.cursor()

# 1. Create tables according to Room entity schemas
cursor.execute("""
CREATE TABLE IF NOT EXISTS lessons (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    orderIndex INTEGER NOT NULL
);
""")

cursor.execute("""
CREATE TABLE IF NOT EXISTS words (
    id INTEGER PRIMARY KEY,
    lessonId INTEGER NOT NULL,
    kanji TEXT NOT NULL,
    furigana TEXT NOT NULL,
    romaji TEXT NOT NULL,
    meaning TEXT NOT NULL,
    type TEXT NOT NULL,
    level TEXT NOT NULL,
    isFavorite INTEGER NOT NULL
);
""")

cursor.execute("""
CREATE TABLE IF NOT EXISTS grammar (
    id INTEGER PRIMARY KEY,
    lessonId INTEGER NOT NULL,
    title TEXT NOT NULL,
    structure TEXT NOT NULL,
    explanation TEXT NOT NULL,
    examples TEXT NOT NULL
);
""")

cursor.execute("""
CREATE TABLE IF NOT EXISTS kanji (
    id INTEGER PRIMARY KEY,
    lessonId INTEGER NOT NULL,
    character TEXT NOT NULL,
    onyomi TEXT NOT NULL,
    kunyomi TEXT NOT NULL,
    meaning TEXT NOT NULL,
    strokeCount INTEGER NOT NULL
);
""")

cursor.execute("""
CREATE TABLE IF NOT EXISTS user_progress (
    userId TEXT NOT NULL,
    lessonId INTEGER NOT NULL,
    score INTEGER NOT NULL,
    isCompleted INTEGER NOT NULL,
    lastUpdated INTEGER NOT NULL,
    PRIMARY KEY (userId, lessonId)
);
""")

# 2. Seed special lessons
special_lessons = [
    (1, "Bảng chữ cái Hiragana", "Làm quen với Hiragana", 1),
    (2, "Bảng chữ cái Katakana", "Làm quen với Katakana", 2),
    (3, "Số đếm & Thời gian", "Nền tảng số đếm", 3)
]
cursor.executemany("INSERT INTO lessons (id, title, description, orderIndex) VALUES (?, ?, ?, ?)", special_lessons)

# 3. Seed Alphabet Words
hiragana_data = [
    ("あ", "a"), ("い", "i"), ("う", "u"), ("え", "e"), ("お", "o"),
    ("か", "ka"), ("き", "ki"), ("ku", "ku"), ("け", "ke"), ("こ", "ko"),
    ("さ", "sa"), ("し", "shi"), ("す", "su"), ("せ", "se"), ("そ", "so"),
    ("た", "ta"), ("ち", "chi"), ("つ", "tsu"), ("て", "te"), ("と", "to"),
    ("な", "na"), ("ni", "ni"), ("ぬ", "nu"), ("ね", "ne"), ("の", "no"),
    ("は", "ha"), ("ひ", "hi"), ("ふ", "fu"), ("へ", "he"), ("ほ", "ho"),
    ("ま", "ma"), ("み", "mi"), ("む", "mu"), ("め", "me"), ("も", "mo"),
    ("や", "ya"), ("ゆ", "yu"), ("よ", "yo"),
    ("ら", "ra"), ("り", "ri"), ("る", "ru"), ("れ", "re"), ("ろ", "ro"),
    ("わ", "wa"), ("を", "wo"), ("ん", "n")
]
for index, (kana, romaji) in enumerate(hiragana_data):
    cursor.execute("""
        INSERT INTO words (id, lessonId, kanji, furigana, romaji, meaning, type, level, isFavorite)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (5000 + index, 1, "", kana, romaji, romaji, "hiragana", "n5", 0))

katakana_data = [
    ("ア", "a"), ("イ", "i"), ("ウ", "u"), ("エ", "e"), ("オ", "o"),
    ("カ", "ka"), ("キ", "ki"), ("ク", "ku"), ("ケ", "ke"), ("コ", "ko"),
    ("サ", "sa"), ("シ", "shi"), ("ス", "su"), ("セ", "se"), ("ソ", "so"),
    ("タ", "ta"), ("チ", "chi"), ("ツ", "tsu"), ("テ", "te"), ("ト", "to"),
    ("ナ", "na"), ("ニ", "ni"), ("ヌ", "nu"), ("ネ", "ne"), ("ノ", "no"),
    ("ハ", "ha"), ("ヒ", "hi"), ("フ", "fu"), ("ヘ", "he"), ("ホ", "ho"),
    ("マ", "ma"), ("ミ", "mi"), ("ム", "mu"), ("メ", "me"), ("モ", "mo"),
    ("ヤ", "ya"), ("ユ", "yu"), ("ヨ", "yo"),
    ("ラ", "ra"), ("リ", "ri"), ("ル", "ru"), ("レ", "re"), ("ロ", "ro"),
    ("ワ", "wa"), ("ヲ", "wo"), ("ン", "n")
]
for index, (kana, romaji) in enumerate(katakana_data):
    cursor.execute("""
        INSERT INTO words (id, lessonId, kanji, furigana, romaji, meaning, type, level, isFavorite)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (6000 + index, 2, "", kana, romaji, romaji, "katakana", "n5", 0))

# 4. Parse the 25 lesson JSON files and import into SQLite
for i in range(1, 26):
    json_path = os.path.join(assets_path, f"lesson_{i}.json")
    if not os.path.exists(json_path):
        print(f"File not found: {json_path}")
        continue
    
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)
        
    db_lesson_id = i + 3
    
    # Insert Lesson
    lesson_title = f"Bài {i}: {data['lesson']['title']}"
    lesson_desc = data['lesson'].get('description', '')
    cursor.execute("""
        INSERT INTO lessons (id, title, description, orderIndex)
        VALUES (?, ?, ?, ?)
    """, (db_lesson_id, lesson_title, lesson_desc, db_lesson_id))
    
    # Insert Vocabulary
    for v in data.get('vocabulary', []):
        word_id = v.get('wordId', 0) + (db_lesson_id * 1000)
        cursor.execute("""
            INSERT INTO words (id, lessonId, kanji, furigana, romaji, meaning, type, level, isFavorite)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (word_id, db_lesson_id, v.get('kanji', ''), v.get('furigana', ''), v.get('romaji', ''), v.get('meaning', ''), v.get('type', ''), 'n5', 0))
        
    # Insert Grammar
    for g in data.get('grammar', []):
        grammar_id = g.get('grammarId', 0) + (db_lesson_id * 1000)
        examples_json = json.dumps(g.get('examples', []), ensure_ascii=False)
        cursor.execute("""
            INSERT INTO grammar (id, lessonId, title, structure, explanation, examples)
            VALUES (?, ?, ?, ?, ?, ?)
        """, (grammar_id, db_lesson_id, g.get('title', ''), g.get('structure', ''), g.get('explanation', ''), examples_json))
        
    # Insert Kanji
    for k in data.get('kanji', []):
        kanji_id = k.get('kanjiId', 0) + (db_lesson_id * 1000)
        cursor.execute("""
            INSERT INTO kanji (id, lessonId, character, onyomi, kunyomi, meaning, strokeCount)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """, (kanji_id, db_lesson_id, k.get('character', ''), k.get('onyomi', ''), k.get('kunyomi', ''), k.get('meaning', ''), k.get('strokeCount', 0)))

conn.commit()
conn.close()

print("Database created and seeded successfully as 'n5_nihongo.db'.")
