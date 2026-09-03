from pathlib import Path

import pandas as pd

CONFIG_FILE = Path(__file__).resolve().parent / "engine" / "config" / "MaterialData.xlsx"

# UtilityType(프론트/백엔드) -> MaterialData.xlsx 'Utility Parameter' 시트의 utilityCostData 키
UTILITY_TYPE_SOURCE_KEY = {
    "COOLING": "CoolingWaterPrice",
    "HOT": "NGprice",
    "ELECTRICITY": "electricityCostPerKWH",
    "MPSG": "StreamPrice(MPS)",
}


def read_utility_prices() -> dict:
    """
    Utility 설정 화면에 보여줄, 현재 설정된 utility 단가(단위 포함)를 돌려준다.
    이 값은 케이스/run과 무관하게 python-engine 도커 이미지에 고정된 전역 설정이다.
    """
    df = pd.read_excel(io=CONFIG_FILE, sheet_name="Utility Parameter", header=1, engine="openpyxl")

    values: dict[str, dict] = {}
    for i in range(len(df)):
        key = df.iat[i, 1]
        value = df.iat[i, 2]
        if pd.isna(key) or pd.isna(value):
            continue
        unit = df.iat[i, 3] if df.shape[1] > 3 else None
        values[str(key)] = {"value": float(value), "unit": None if pd.isna(unit) else str(unit)}

    result = {}
    for utility_type, source_key in UTILITY_TYPE_SOURCE_KEY.items():
        entry = values.get(source_key)
        if entry:
            result[utility_type] = entry
    return result
