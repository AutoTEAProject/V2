#!/opt/anaconda3/envs/myenv/bin/python
import json
import pandas as pd
from data import utilityCostData, calcOPEXdata, profitAnalysisData, HeaterParam, HeatExchangerParam, CompressorParam, reactorParam, equipmentConfig

# UtilityType(백엔드) -> utilityCostData 키. equipment_config.json의 "utilityPrices"로 덮어쓸 때 쓴다.
UTILITY_TYPE_TO_KEY = {
	"COOLING": "CoolingWaterPrice",
	"HOT": "NGprice",
	"ELECTRICITY": "electricityCostPerKWH",
	"MPSG": "StreamPrice(MPS)",
}

def parseUtilityParam():
	filename = "./input/MaterialData.xlsx"
	df = pd.read_excel(io = filename, sheet_name='Utility Parameter', header=1, engine='openpyxl')

	for i in range(0, 5):
		key = df.iat[i, 1]
		value = df.iat[i, 2]
		utilityCostData[key] = float(value)

	for i in range(6, 8):
		key = df.iat[i, 1]
		value = df.iat[i, 2]
		calcOPEXdata[key] = float(value)
 
	key = df.iat[11, 1]
	value = df.iat[11, 2]
	profitAnalysisData[key] = float(value)

def parseEquipmentConfig():
	"""
	Java 백엔드가 run 입력 폴더에 써주는 input/equipment_config.json 을 읽어
	1) 장치별(skipCost/defaultFormula/selectedFormulas/utilityTypes) 설정을 equipmentConfig 전역에 채우고
	2) 프로젝트에서 선택된 수식들의 K1/K2/K3 계수를 HeaterParam/HeatExchangerParam/CompressorParam에 채운다.
	예전에는 이 값들이 도커 이미지에 고정된 MaterialData.xlsx의 'Equipment Cost Parameter' 시트(행 번호 고정)에서 왔지만,
	이제는 프로젝트별로 자유롭게 추가/수정/삭제 가능한 DB 기반 수식 라이브러리에서 온다.
	"""
	filename = "./input/equipment_config.json"
	with open(filename, encoding="utf-8") as f:
		config = json.load(f)

	equipmentConfig["equipment"] = config.get("equipment", {})
	equipmentConfig["streams"] = config.get("streams", {})
	coefficients = config.get("formulaCoefficients", {})
	equipmentConfig["formulaCoefficients"] = coefficients

	for name, params in coefficients.get("HTX", {}).items():
		HeaterParam[name] = {"K1": params["K1"], "K2": params["K2"], "K3": params["K3"]}
	for name, params in coefficients.get("HEX", {}).items():
		HeatExchangerParam[name] = {"K1": params["K1"], "K2": params["K2"], "K3": params["K3"]}
	for name, params in coefficients.get("COMP", {}).items():
		CompressorParam[name] = {"K1": params["K1"], "K2": params["K2"], "K3": params["K3"]}

	# utility 단가는 MaterialData.xlsx가 기본값이고, 프로젝트에서 따로 설정했으면 그 값으로 덮어쓴다.
	for utilityType, price in config.get("utilityPrices", {}).items():
		key = UTILITY_TYPE_TO_KEY.get(utilityType)
		if key and "value" in price:
			utilityCostData[key] = float(price["value"])

def parsereactorParam():
	xlsxfilename = "./input/MaterialData.xlsx"
	df = pd.read_excel(io = xlsxfilename, sheet_name='Reactor Parameter', header=1, engine='openpyxl')
	cepci_recent = df.iat[0, 9]
	for i in range(0, 4):
		ReactorName = df.iat[i, 1]
		capacity = df.iat[i, 2]
		equipment_cost = df.iat[i, 3]
		cepci_before = df.iat[i, 4]
		scaling_factor = df.iat[i, 5]
		capacity_parsed = df.iat[i, 6]
		additional_param = df.iat[i, 7]
		if (pd.isna(ReactorName)):
			raise TypeError("Reactor Parameter의 Reactor Name을 입력해주세요.")
		if (pd.isna(ReactorName) == False):
			if (pd.isna(capacity) or pd.isna(equipment_cost) or pd.isna(cepci_before)):
				raise TypeError("Reactor Parameter의 Parameter를 입력해주세요. : " + ReactorName)
			reactorParam[ReactorName] = {"Capacity [KW]" : capacity, "Equipment Cost" : equipment_cost,  "Cepci_before" : cepci_before, "Cepci_recent" : cepci_recent, "Scaling Factor" : scaling_factor, "Additional Param" : additional_param}
			reactorParam[ReactorName]["Capacity_parsed KW"] = capacity_parsed
			# print("Reactor Parameter parsed:", ReactorName, reactorParam[ReactorName])

