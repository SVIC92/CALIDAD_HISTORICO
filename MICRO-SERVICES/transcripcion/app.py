from __future__ import annotations

import asyncio
import json
import os
import sys
from pathlib import Path

import requests
import vosk
from fastapi import FastAPI, WebSocket, WebSocketDisconnect


app = FastAPI()

BASE_DIR = Path(__file__).resolve().parent
APP_ENV = os.getenv("APP_ENV", "local").lower()
MODEL_PATH = Path(os.getenv("VOSK_MODEL_PATH", str(BASE_DIR / "model")))

SPRING_BOOT_URL = (
    os.getenv("SPRING_BOOT_URL")
    or (
        os.getenv("SPRING_BOOT_URL_LOCAL")
        if APP_ENV == "local"
        else os.getenv("SPRING_BOOT_URL_PROD")
    )
    or "http://localhost:8080/api/subtitulos/interno"
)


if not MODEL_PATH.exists() or not MODEL_PATH.is_dir():
    print(
        "Error: No se encuentra la carpeta del modelo de Vosk. "
        "Define VOSK_MODEL_PATH o coloca la carpeta 'model' junto a este archivo."
    )
    sys.exit(1)


model = vosk.Model(str(MODEL_PATH))


async def enviar_a_spring_boot(payload: dict[str, str]) -> None:
    try:
        response = await asyncio.to_thread(
            requests.post,
            SPRING_BOOT_URL,
            json=payload,
            timeout=10,
        )
        response.raise_for_status()
    except Exception as exc:
        print(f"Error al enviar a Spring Boot ({SPRING_BOOT_URL}): {exc}")


@app.websocket("/ws/transcribir/{sala_uuid}/{usuario_id}")
async def transcribir_audio(websocket: WebSocket, sala_uuid: str, usuario_id: str) -> None:
    await websocket.accept()

    rec = vosk.KaldiRecognizer(model, 16000)

    try:
        while True:
            data = await websocket.receive_bytes()

            if rec.AcceptWaveform(data):
                resultado = json.loads(rec.Result())
                texto = resultado.get("text", "").strip()

                if texto:
                    payload = {
                        "salaUuid": sala_uuid,
                        "usuarioId": usuario_id,
                        "texto": texto,
                    }
                    await enviar_a_spring_boot(payload)
    except WebSocketDisconnect:
        print(f"Usuario {usuario_id} desconectado de la sala {sala_uuid}")