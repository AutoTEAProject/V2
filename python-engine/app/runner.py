import json
import shutil
import subprocess
import sys
from pathlib import Path

ENGINE_DIR = Path(__file__).resolve().parent / "engine"
DEFAULT_MATERIAL_DATA = ENGINE_DIR / "config" / "MaterialData.xlsx"


class CalculationError(Exception):
    def __init__(self, message: str, logs: str = ""):
        super().__init__(message)
        self.logs = logs


def parse_equipment(runs_base: Path, run_id: str, timeout: int = 300) -> tuple[list[dict], str]:
    """
    input.xlsx/input.rep만으로 장치 이름+타입 목록을 뽑아낸다(원가 계산은 하지 않음).
    Java 백엔드가 이 목록을 보여주고 장치비/utility 설정을 받은 뒤 execute()를 호출하는 흐름의 앞단.
    """
    target_dir = runs_base / run_id
    input_dir = target_dir / "input"

    if not (input_dir / "input.xlsx").exists() or not (input_dir / "input.rep").exists():
        raise CalculationError(f"run {run_id}에 input.xlsx / input.rep가 없습니다: {input_dir}")

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

    with open(parse_result_file) as f:
        equipment = json.load(f)

    return equipment, logs


def execute(runs_base: Path, run_id: str, timeout: int = 300) -> tuple[Path, str]:
    """
    runs_base/{run_id}/input/ 아래 input.xlsx, input.rep가 이미 있다고 가정하고
    (Java 백엔드가 공유 볼륨에 미리 저장해둔다), MaterialData.xlsx를 채워 넣은 뒤
    기존 v1 main.py를 그 디렉토리를 cwd로 서브프로세스 실행한다.
    v1 코드가 "./input/..." 상대경로와 "./output.xlsx" 상대경로를 그대로 쓰기 때문에,
    로직을 고치지 않고 실행 위치만 격리하는 방식이다.
    """
    target_dir = runs_base / run_id
    input_dir = target_dir / "input"

    if not (input_dir / "input.xlsx").exists() or not (input_dir / "input.rep").exists():
        raise CalculationError(f"run {run_id}에 input.xlsx / input.rep가 없습니다: {input_dir}")

    shutil.copy(DEFAULT_MATERIAL_DATA, input_dir / "MaterialData.xlsx")

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

    return output_file, logs
