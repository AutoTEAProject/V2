import os
from pathlib import Path

from fastapi import FastAPI

from app.runner import CalculationError, execute, parse_equipment
from app.schemas import CalculateRequest, CalculateResponse, EquipmentInfo, ParseResponse
from app.utility_prices import read_utility_prices

RUNS_DIR = Path(os.environ.get("RUNS_DIR", "/data/runs"))
CALC_TIMEOUT_SECONDS = int(os.environ.get("CALC_TIMEOUT_SECONDS", "300"))

app = FastAPI(title="AutoTEA Python Engine")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/calculate", response_model=CalculateResponse)
def calculate(request: CalculateRequest) -> CalculateResponse:
    try:
        output_file, logs = execute(RUNS_DIR, request.runId, timeout=CALC_TIMEOUT_SECONDS)
        return CalculateResponse(status="SUCCESS", resultPath=str(output_file), logs=logs)
    except CalculationError as e:
        return CalculateResponse(status="FAILED", errorMessage=str(e), logs=e.logs)


@app.post("/parse", response_model=ParseResponse)
def parse(request: CalculateRequest) -> ParseResponse:
    try:
        equipment, logs = parse_equipment(RUNS_DIR, request.runId, timeout=CALC_TIMEOUT_SECONDS)
        return ParseResponse(status="SUCCESS", equipment=[EquipmentInfo(**e) for e in equipment], logs=logs)
    except CalculationError as e:
        return ParseResponse(status="FAILED", errorMessage=str(e), logs=e.logs)


@app.get("/config/utility-prices")
def utility_prices() -> dict:
    return read_utility_prices()
