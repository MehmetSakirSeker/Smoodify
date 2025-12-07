import os
import pandas as pd

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

ROOT_DIR = os.path.dirname(BASE_DIR)

input_file = os.path.join(ROOT_DIR, "datasets", "dataset.csv")
output_file = os.path.join(ROOT_DIR, "datasets", "smoodify_cleaned_data.csv")

print(f"Loading {input_file}...")
df = pd.read_csv(input_file)
original_rows = len(df)

# ==========================================
# STEP 1: FILTER ROWS
# ==========================================

df.drop_duplicates(subset=['track_id'], inplace=True)
df.drop_duplicates(subset=['track_name', 'artists'], keep='first', inplace=True)

df = df[df['speechiness'] < 0.66]
df.dropna(inplace=True)

# ==========================================
# STEP 2: SELECT RELEVANT COLUMNS
# ==========================================

columns_to_keep = [
    'track_id', 'track_name', 'artists', 'album_name', 'track_genre',
    'popularity', 'danceability', 'energy', 'valence', 'tempo',
    'instrumentalness', 'acousticness', 'mode'
]

df_clean = df[columns_to_keep]

# ==========================================
# STEP 3: SAVE AND REPORT
# ==========================================

df_clean.to_csv(output_file, index=False)

print("-" * 30)
print(f"Processing Complete!")
print(f"Original Rows: {original_rows}")
print(f"Cleaned Rows:  {len(df_clean)}")
print(f"Saved to: {output_file}")
print("-" * 30)
