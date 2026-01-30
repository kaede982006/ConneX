#!/bin/bash
# scripts/run_dev.sh

# Backend
cd backend
# Check if .env exists
if [ ! -f .env ]; then
    cp .env.example .env
    echo "Created .env from .env.example"
fi

# Run docker compose if needed (optional)
# docker-compose up -d

# Install dependecies
pip install -e .

# Run Uvicorn
uvicorn connex.main:app --reload --host 0.0.0.0 --port 8000
