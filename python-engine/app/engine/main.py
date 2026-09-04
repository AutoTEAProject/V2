#!/opt/anaconda3/envs/myenv/bin/python
import sys
import json

from Parse import parseTEA, parseHEX, parseCOMP, parseCAPCOSTParam, parseUtility, parseLawMaterial, parseEQUIP, parseStreamNames, parseFlowFromConfig, parseMPSG, parseMPS, parseHotUtility
from Utility import calEquipmentCost, printout, inputRTX
from Calc import calCAPEX, calUtility, calOPEX, calProfitAnalysis
from ExcelParse import parseUtilityParam, parsereactorParam, parseEquipmentConfig
from data import equipmentConfig

inputData = {}
inputfile = "./input/input.xlsx"
inputrep = "./input/input.rep"

if "--parse-only" in sys.argv:
	# 원가 계산은 하지 않고 장치 이름+타입, stream 이름만 뽑아 parse_result.json으로 출력한다.
	# (프론트의 장치비/utility/원료·제품 설정 화면에 이 프로젝트에 뭐가 있는지 보여주기 위한 사전 단계)
	parseTEA(inputfile, inputData)
	parseEQUIP(inputrep, inputData)
	equipment = [{"name": name, "type": info["Type"]} for name, info in inputData.items()]
	streams = parseStreamNames(inputrep)
	result = {"equipment": equipment, "streams": streams}
	with open("parse_result.json", "w", encoding="utf-8") as f:
		json.dump(result, f)
	sys.exit(0)

lawMaterialData = {}
cost = {} # 2차원 딕셔너리로 "이름" : {딕셔너리} 이렇게 저장하고 각 유닛 종류별 인자와 계산 결과를 출력한다.
CAPEX = {}
OPEX = {}
utility =  {}
profitAnalysis = {}
flowData = {}
exceptCapacity = {}
try:
	parseUtilityParam()
	parseEquipmentConfig()
	parsereactorParam()
	parseFlowFromConfig(flowData, equipmentConfig.get("streams", {}))
except Exception as e:
	print("Error parsexlxs:", e)
try:
	parseTEA(inputfile, inputData)
	parseEQUIP(inputrep, inputData)
	parseHEX(inputfile, inputData)
	parseCOMP(inputfile, inputData)
	parseCAPCOSTParam(inputrep, inputData)
	parseUtility(inputData, inputrep, utility, exceptCapacity) # exceptCapacity는 지워야하는 코드
	parseMPSG(inputData, inputrep, utility)
	parseMPS(inputData, inputrep, utility)
	parseHotUtility(inputData, inputrep, utility, exceptCapacity)

except Exception as e:
	print("Error parserep:", e)

# try:
calEquipmentCost(inputData, cost, utility, exceptCapacity)
calCAPEX(inputData, cost, CAPEX)
calUtility(utility)
calOPEX(CAPEX, flowData, OPEX, utility)
calProfitAnalysis(CAPEX, OPEX, profitAnalysis, flowData)
printout(inputData, cost, utility, CAPEX, OPEX, profitAnalysis)

# 장치별/수식별로 실제 계산된 장치비(EQUIPMENT COST)를 프론트의 장치비 설정 화면에서
# 보여줄 수 있도록 별도 JSON으로도 남겨둔다. default=float는 pandas/numpy 숫자형을
# 표준 float으로 안전하게 변환하기 위함.
with open("cost_result.json", "w", encoding="utf-8") as f:
	json.dump(cost, f, default=float)

# except Exception as e:
# 	print("Error calc:", e)
