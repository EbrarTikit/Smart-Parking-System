from fastapi import FastAPI
from app.routes import router

app = FastAPI(
    title="Chatbot Service",
    description="Gemini API ile otopark chatbot servisi",
    version="1.0"
)

app.include_router(router)

@app.get("/")
async def root():
    return {"message": "Chatbot Service is running"}
