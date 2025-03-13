from fastapi import FastAPI, UploadFile, File
from .recognition import recognize_license_plate

app = FastAPI()

@app.post("/recognize/")
async def recognize(file: UploadFile = File(...)):
    # Dosyayı kaydet ve plaka tanıma işlemini gerçekleştir
    return {"message": "Plaka tanındı"}