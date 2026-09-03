import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ENGINE_DIR = Path(__file__).resolve().parent / "engine"
DEFAULT_MATERIAL_DATA = ENGINE_DIR / "config" / "MaterialData.xlsx"


class CalculationError(Exception):
    def __init__(self, message: str, logs: str = ""):
        super().__init__(message)
        self.logs = logs


def parse_equipment(xlsx_bytes: bytes, rep_bytes: bytes, timeout: int = 300) -> tuple[list[dict], str]:
    """
    input.xlsx/input.rep 바이트만으로 장치 이름+타입 목록을 뽑아낸다(원가 계산은 하지 않음).
    호출마다 새 임시 디렉터리를 만들어 격리하고 끝나면 지운다(backend와 디스크를 공유하지 않는다).
    """
    with tempfile.TemporaryDirectory(prefix="autotea-parse-") as tmp:
        target_dir = Path(tmp)
        input_dir = target_dir / "input"
        input_dir.mkdir()
        (input_dir / "input.xlsx").write_bytes(xlsx_bytes)
        (input_dir / "input.rep").write_bytes(rep_bytes)

        result = subprocess.run(
            [sys.executable, str(ENGINE_DIR / "main.py"), "--parse-only"],
            cwd=target_dir,
            capture_output=True,
            text=True,
            timeout=timeout,
        )
        logs = (result.stdout or "") + (result.stderr or "")

        parse_result_file = target_dir / "parse_result.json"
        if result.returncode != 0 or not parse_result_file.exists():
            raise CalculationError(f"장치 파싱 실패 (exit code {result.returncode})", logs=logs)

        with open(parse_result_file, encoding="utf-8") as f:
            equipment = json.load(f)

        return equipment, logs


def execute(xlsx_bytes: bytes, rep_bytes: bytes, equipment_config: dict, timeout: int = 300) -> tuple[bytes, dict, str]:
    """
    실제 TEA 계산을 실행하고 (output.xlsx 바이트, 장치별 계산 원가, 로그)를 돌려준다.
    parse_equipment와 마찬가지로 요청마다 격리된 임시 디렉터리를 쓰고 끝나면 지운다.
    """
    with tempfile.TemporaryDirectory(prefix="autotea-calc-") as tmp:
        target_dir = Path(tmp)
        input_dir = target_dir / "input"
        input_dir.mkdir()
        (input_dir / "input.xlsx").write_bytes(xlsx_bytes)
        (input_dir / "input.rep").write_bytes(rep_bytes)
        shutil.copy(DEFAULT_MATERIAL_DATA, input_dir / "MaterialData.xlsx")
        (input_dir / "equipment_config.json").write_text(json.dumps(equipment_config), encoding="utf-8")

        result = subprocess.run(
            [sys.executable, str(ENGINE_DIR / "main.py")],
            cwd=target_dir,
            capture_output=True,
            text=True,
            timeout=timeout,
        )
        logs = (result.stdout or "") + (result.stderr or "")

        output_file = target_dir / "output.xlsx"
        if result.returncode != 0 or not output_file.exists():
            raise CalculationError(f"계산 실패 (exit code {result.returncode})", logs=logs)

        cost_file = target_dir / "cost_result.json"
        cost_result = {}
        if cost_file.exists():
            with open(cost_file, encoding="utf-8") as f:
                cost_result = json.load(f)

        return output_file.read_bytes(), cost_result, logs
