from pydantic import BaseModel


class CalculateRequest(BaseModel):
    runId: str


class CalculateResponse(BaseModel):
    status: str
    resultPath: str | None = None
    logs: str | None = None
    errorMessage: str | None = None


class EquipmentInfo(BaseModel):
    name: str
    type: str


class ParseResponse(BaseModel):
    status: str
    equipment: list[EquipmentInfo] | None = None
    logs: str | None = None
    errorMessage: str | None = None
