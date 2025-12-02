# This script is used to download the dataset using kagglehub
# You must run this script once before start working on the data

import kagglehub
import os
import shutil

path = kagglehub.dataset_download("maharshipandya/-spotify-tracks-dataset")

print(f"Dataset downloaded in cache! Copying into the project folder...\nCache path: {path}")

if not os.path.exists("./datasets"):
    os.mkdir("./datasets")

shutil.copy2(f"{path}/dataset.csv", "./datasets/dataset.csv")

print("Dataset downloaded successfully!")
