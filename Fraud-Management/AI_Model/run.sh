#!/bin/bash

echo "Installing dependencies..."
pip install -r requirements.txt

echo "Training model..."
python train_model.py

echo "Starting AI server..."
uvicorn app:app --reload --port 8000