import base64
import json
import os

from fastapi import FastAPI, File, Form, UploadFile

from app.runner import CalculationError, execute, parse_equipment
from app.schemas import CalculateResponse, EquipmentInfo, ParseResponse
from app.utility_prices import read_utility_prices

CALC_TIMEOUT_SECONDS = int(os.environ.get("CALC_TIMEOUT_SECONDS", "300"))

app = FastAPI(title="AutoTEA Python Engine")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/parse", response_model=ParseResponse)
async def parse(xlsxFile: UploadFile = File(...), repFile: UploadFile = File(...)) -> ParseResponse:
    try:
        xlsx_bytes = await xlsxFile.read()
        rep_bytes = await repFile.read()
        equipment, logs = parse_equipment(xlsx_bytes, rep_bytes, timeout=CALC_TIMEOUT_SECONDS)
        return ParseResponse(status="SUCCESS", equipment=[EquipmentInfo(**e) for e in equipment], logs=logs)
    except CalculationError as e:
        return ParseResponse(status="FAILED", errorMessage=str(e), logs=e.logs)


@app.post("/calculate", response_model=CalculateResponse)
async def calculate(
    xlsxFile: UploadFile = File(...),
    repFile: UploadFile = File(...),
    equipmentConfig: str = Form(...),
) -> CalculateResponse:
    try:
        xlsx_bytes = await xlsxFile.read()
        rep_bytes = await repFile.read()
        config = json.loads(equipmentConfig)
        output_bytes, cost_result, logs = execute(xlsx_bytes, rep_bytes, config, timeout=CALC_TIMEOUT_SECONDS)
        output_base64 = base64.b64encode(output_bytes).decode("ascii")
        return CalculateResponse(status="SUCCESS", outputXlsxBase64=output_base64, costResult=cost_result, logs=logs)
    except CalculationError as e:
        return CalculateResponse(status="FAILED", errorMessage=str(e), logs=e.logs)


@app.get("/config/utility-prices")
def utility_prices() -> dict:
    return read_utility_prices()
