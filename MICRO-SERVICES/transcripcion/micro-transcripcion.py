import os

from app import app


if __name__ == "__main__":
    import uvicorn

    host = os.getenv("HOST", "0.0.0.0")
    port = int(os.getenv("PORT", "8000"))
    reload_enabled = os.getenv("APP_ENV", "local").lower() == "local"

    uvicorn.run("app:app", host=host, port=port, reload=reload_enabled)