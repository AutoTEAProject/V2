from typing import Any

from pydantic import BaseModel


class CalculateResponse(BaseModel):
    status: str
    outputXlsxBase64: str | None = None
    costResult: dict[str, dict[str, dict[str, Any]]] | None = None
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
