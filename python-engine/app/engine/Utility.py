import math
from copy import deepcopy
from enums import EquipLen
import pandas as pd
from openpyxl import Workbook
from openpyxl.utils.dataframe import dataframe_to_rows
from openpyxl.styles import Font
from data import HeaterParam, HeatExchangerParam, CompressorParam, reactorParam, ReactParam, atomicWeight, utilityCostData, equipmentConfig, HTX_CAPACITY_PARAM
from openpyxl import load_workbook


def checkType(name): 
	length = len(name)
	
	# 임시 예외처리
	if (name == "OLINHEA"):
		return "HTX"
	for i in range(0, length):
		# Mixer part
		if (length - i >= EquipLen.MIXER and name[i:i + EquipLen.MIXER] == "MIXER"):
			return "MIX"
		elif (length - i >= EquipLen.SSPLIT and name[i:i + EquipLen.SSPLIT] == "SSPLIT"):
			return "MIX"
		elif (length - i >= EquipLen.FSPLIT and name[i:i + EquipLen.FSPLIT] == "FSPLIT"):
			return "MIX"
		# Seperate part
		elif (length - i >= EquipLen.FLASH2 and name[i:i + EquipLen.FLASH2] == "FLASH2"):
			return "FLASH"
		elif (length - i >= EquipLen.FLASH3 and name[i:i + EquipLen.FLASH3] == "FLASH3"):
			return "FLASH"
		elif (length - i >= EquipLen.SEP and name[i:i + EquipLen.SEP] == "SEP"):
			return "SEP"
		elif (length - i >= EquipLen.SEP2 and name[i:i + EquipLen.SEP2] == "SEP2"):
			return "SEP"
		# Exchanger part
		elif (length - i >= EquipLen.HEATER and name[i:i + EquipLen.HEATER] == "HEATER"):
			return "HTX"
		elif (length - i >= EquipLen.HEATX and name[i:i + EquipLen.HEATX] == "HEATX"):
			return "HEX"
		elif (length - i >= EquipLen.MHEATX and name[i:i + EquipLen.MHEATX] == "MHEATX"):
			return "HEX"
		elif (length - i >= EquipLen.HXFLUX and name[i:i + EquipLen.HXFLUX] == "HXFLUX"):
			return "HEX"
		# Column part
		elif (length - i >= EquipLen.DSTWU and name[i:i + EquipLen.DSTWU] == "DSTWU"):
			return "DIST"
		elif (length - i >= EquipLen.DISTL and name[i:i + EquipLen.DISTL] == "DISTL"):
			return "DIST"
		elif (length - i >= EquipLen.RADFRAC and name[i:i + EquipLen.RADFRAC] == "RADFRAC"):
			return "DIST"
		elif (length - i >= EquipLen.PETROFRAC and name[i:i + EquipLen.PETROFRAC] == "PETROFRAC"):
			return "DIST"
		# Reactor Part
		elif (length - i >= EquipLen.RSTOIC and name[i:i + EquipLen.RSTOIC] == "RSTOIC"):
			return "REACT"
		elif (length - i >= EquipLen.RSTOIC and name[i:i + EquipLen.RSTOIC] == "RYIELD"):
			return "REACT"
		elif (length - i >= EquipLen.RSTOIC and name[i:i + EquipLen.RSTOIC] == "REQUIL"):
			return "REACT"
		elif (length - i >= EquipLen.RSTOIC and name[i:i + EquipLen.RSTOIC] == "RGIBBS"):
			return "REACT"
		elif (length - i >= EquipLen.RSTOIC and name[i:i + EquipLen.RSTOIC] == "RCSTR"):
			return "REACT"
		elif (length - i >= EquipLen.RSTOIC and name[i:i + EquipLen.RSTOIC] == "RPLUG"):
			return "REACT"
		# Pressure Changer Part
		elif (length - i >= EquipLen.COMPR and name[i:i + EquipLen.COMPR] == "COMPR"):
			return "COMP"
		elif (length - i >= EquipLen.COMP and name[i:i + EquipLen.COMP] == "COMP"):
			return "COMP"
		elif (length - i >= EquipLen.MCOMPR and name[i:i + EquipLen.MCOMPR] == "MCOMPR"):
			return "COMP"
		# 아래 두 개는 계산 아스펜에서 해줘서 원래 벨브 처리 어떻게 했는지 확인필요
		elif (length - i >= EquipLen.PUMP and name[i:i + EquipLen.PUMP] == "PUMP"):
			return "VALVE"
		elif (length - i >= EquipLen.VALVE and name[i:i + EquipLen.VALVE] == "VALVE"):
			return "VALVE"
		# Solid Seperator Part -> aspen에서 계산해줌
		elif (length - i >= EquipLen.CYCLONE and name[i:i + EquipLen.CYCLONE] == "CYCLONE"):
			return "DIST"
		elif (length - i >= EquipLen.HYCYC and name[i:i + EquipLen.HYCYC] == "HYCYC"):
			return "DIST"
	# print("Type을 확인할 수 없는 장비가 있습니다. : " + name)
	return "ETC"

def selectedFormulaNames(key, paramDict):
	"""
	장치 key에 대해 체크박스로 선택된 후보 수식 이름들을 돌려준다.
	설정이 없는 장치(예: 파싱 스냅샷에 없던 예외 상황)는 방어적으로 해당 타입의 모든 후보를 계산한다.
	"""
	names = equipmentConfig.get("equipment", {}).get(key, {}).get("selectedFormulas")
	if names:
		return [name for name in names if name in paramDict]
	return list(paramDict.keys())

def normalizeName(name):
	# 이름이 .으로 구분되어 있으면 . 이후의 문자열을 반환, 아니면 원래 이름 반환
	if "." in name:
		return name.split(".")[-1]
	return name

def calMaterialWeight(material):
	atom = ""
	count = 0
	weight = 0
	for i in range(len(material)):
		if material[i].isalpha() and material[i].isupper():
			if atom == "":
				atom = material[i]
			else:
				if (count == 0):
					count += 1
				if atom in atomicWeight:
					weight += atomicWeight[atom] * count
				else :
					raise ValueError(f"{atom} is not in atomicWeight Data")
				count = 0
				atom = material[i]
		elif material[i].isalpha() and material[i].islower():
			atom += material[i]
		elif material[i].isdigit():
			count = count * 10 + int(material[i])
	if (atom != ""):
		if (count == 0):
			count += 1
		weight += atomicWeight[atom] * count
	return (weight)
	

# cost = {} # 2차원 딕셔너리로 "이름" : {딕셔너리} 이렇게 저장하고 각 유닛 종류별 인자와 계산 결과를 출력한다.
def calEquipmentCost(inputData, cost, utility, exceptCapacity): #react도 추가해야함.
	for key in inputData:
		temp = {}
		type = inputData[key]["Type"]
		exceptflag = False
 		# 여기서 이제 cost의 값들을 하나씩 이름, 인자 순으로 저장해야함.
		'''
		1. EQUIPMENT COST
		- HEX, HTX : ((10^(K1+K2+K3))*(Capacity / 10)^(0.6)) * (CEPCI(June 2024) / CEPCI(Sept 2001))
		- COMP : (10^(K1 + K2 * log(Capacity) + K3 * ((log(Capacity))^2))) * (CEPCI(June 2024) / CEPCI(Sept 2001))
		2. C_BM 
		- HEX, HTX : EQUIPMENT COST * (B1 + B2*FM)
		'''

		if (type == "HTX"):
			if ("HOT UTILITY[kW]" in utility[key]):
				utilityKey = "HOT UTILITY[kW]"
			elif ("ELECTRICITY UTILITY[kW]" in utility[key]):
				utilityKey = "ELECTRICITY UTILITY[kW]"
			else:
				utilityKey = "COOLING UTILITY[kg/hr]"
			capacity = utility[key][utilityKey]
		elif (type == "HEX"):
			capacity = inputData[key]["HeatTransferArea"]
		elif (type == "COMP"):
			capacity = inputData[key]["DriverPower"]
		if (type == "HTX" or checkType(key) == "HTX"):
			capacity = utility[key][utilityKey] / HTX_CAPACITY_PARAM
			for formulaName in selectedFormulaNames(key, HeaterParam):
				params = HeaterParam[formulaName]
				temp[formulaName] = deepcopy(params)
				temp[formulaName]["EQUIPMENT COST"] = ((10**(params["K1"]+params["K2"]+params["K3"]))*(capacity / 10)**(0.6)) * (798.8 / 397)
		elif (type == "HEX"):
			for formulaName in selectedFormulaNames(key, HeatExchangerParam):
				params = HeatExchangerParam[formulaName]
				temp[formulaName] = deepcopy(params)
				temp[formulaName]["EQUIPMENT COST"] = ((10**(params["K1"]+params["K2"]+params["K3"]))*(capacity / 10)**(0.6)) * (798.8 / 397)
		elif (checkType(key) == "COMP"):
			if (inputData[key]["DriverPower"] == 0):
				print("Driver Power이 0인 Compressor가 있습니다. : " + key)
				continue
				#error
				# 나중에 여기에 에러 처리 코드 넣기
			capacity = inputData[key]["DriverPower"]
			for formulaName in selectedFormulaNames(key, CompressorParam):
				params = CompressorParam[formulaName]
				temp[formulaName] = deepcopy(params)
				temp[formulaName]["EQUIPMENT COST"] = (10**(params["K1"] + params["K2"] * (math.log(capacity, 10)) + (params["K3"] * ((math.log(capacity, 10))**2)))) * (798.8 / 397)
		elif (type == "REACT"):
			if key in reactorParam:
				temp["input"] = deepcopy(reactorParam[key])
				temp["input"]["EQUIPMENT COST"] = (reactorParam[key]["Equipment Cost"] * (reactorParam[key]["Capacity_parsed KW"] / reactorParam[key]["Capacity [KW]"]) ** reactorParam[key]["Scaling Factor"]) * reactorParam[key]["Additional Param"] * (reactorParam[key]["Cepci_recent"] / reactorParam[key]["Cepci_before"])
			else:
				temp["input"] = deepcopy(ReactParam["Nan"])
				temp["input"]["EQUIPMENT COST"] = 0
		# 여기는 이미 가격 계산 되어있으면 계산 안 하는 부분
		if (key not in exceptCapacity and inputData[key]["EquipmentCost"] != 0 and checkType(key) not in ["HTX", "HEX", "COMP"]):
			# print("엑셀에 이미 가격이 입력되어 있습니다. 계산된 가격과 비교해서 확인해주세요. : " + key)
			# print(checkType(key))
			temp["ATEA"] = {}
			temp["ATEA"]["EQUIPMENT COST"] = inputData[key]["EquipmentCost"]
		# if (key == "CO2H2MC.1MCCOMP2"):
		# 	print("CO2H2MC.1MCCOMP2 찾음")
		# 	print(temp)
		# 	print(type)
		# 	print(checkType(key))
		cost[key] = deepcopy(temp)
		# print(cost[key])
	 
# 이제 여기서 Capacity 값은 각 모듈별로 파싱해서 저장해둬야함.
def safe_df(df):
    # 인덱스가 RangeIndex(0,1,2,...)가 아니거나, 인덱스 이름이 있다면 컬럼으로 복구
    if not isinstance(df.index, pd.RangeIndex) or df.index.name is not None:
        return df.reset_index()
    return df

def write_block(ws, df, title):
    ws.append([title])
    ws.cell(row=ws.max_row, column=1).font = Font(bold=True, color="004085")
    for r in dataframe_to_rows(df, index=True, header=True):
        ws.append(r)
    ws.append([])

def autofit(ws):
    for col in ws.columns:
        max_len = max((len(str(cell.value)) if cell.value else 0) for cell in col)
        ws.column_dimensions[col[0].column_letter].width = min(max_len + 2, 60)

def printout(inputData, cost, utility, CAPEX, OPEX, profitAnalysis):
    
    columns = ["Name","EquipmentCost","InstalledCost","EquipmentWeight",
           "InstalledWeight","UtilityCost","HeatTransferArea","DriverPower"]
    wb = Workbook()
    ws = wb.active
    ws.title = "parse"
    # # 데이터 추가
    ws.append(columns)

# 2️⃣ 데이터 추가
    for name, row in inputData.items():
        ws.append([name] + [row[col] for col in columns[1:]])  # 첫 열은 name, 나머지는 columns 순서대로
    autofit(ws)

    # 시트 2: UTILITY
    ws2 = wb.create_sheet("UTILITY")
    for r in dataframe_to_rows(pd.DataFrame(utility), header=True): ws2.append(r)
    autofit(ws2)
    
    # 시트 3: CAPCOST (블럭 스타일)
    ws3 = wb.create_sheet("Equipment Cost")
    for k,v in cost.items():
        # print(k, v)
        df = pd.DataFrame(v).T   # 행열 전치 추가!
        write_block(ws3, df, k)
    autofit(ws3)

    ws4 = wb.create_sheet("CAPEX")

    for key, values in CAPEX.items():
        ws4.append([key] + values)

    autofit(ws4)
    
    # 시트 5: OPEX
    ws5 = wb.create_sheet("OPEX")
    for key, values in OPEX.items():
        ws5.append([key] + values)
    autofit(ws5)
    
    ws6 = wb.create_sheet("Profitability Analysis")
    opex_df = pd.DataFrame([profitAnalysis]).T  if isinstance(profitAnalysis, dict) else pd.DataFrame(profitAnalysis).T 
    for r in dataframe_to_rows(opex_df, header=False):
        ws6.append(r)
    autofit(ws6)
    
    # 마지막에 한 번만 저장
    wb.save("output.xlsx")

def parseReact(excelReactorData):
	filename = "./input/MaterialData.xlsx"
	df = pd.read_excel(io = filename, sheet_name='Reactor', header=1, engine='openpyxl')
	length = len(df)
	for i in range(length):
		name = df.iat[i, 1]
		cost = df.iat[i, 2]
		excelReactorData[name] = cost

def inputReactorName(inputData, cost):
	filename = "./input/MaterialData.xlsx"

	# 기존 엑셀 로드
	wb = load_workbook(filename)
	ws = wb["Reactor"]

	# pandas로 DF 로드 (기존 데이터 확인용)
	df = pd.read_excel(filename, sheet_name='Reactor', header=1, engine='openpyxl')

	# name 컬럼이 실제 몇 번째인지 찾기
	name_col_index = list(df.columns).index("Reactor Name") + 1  # excel column index (+1 필요)

	current_row = 3  # 데이터 시작 row

	for key in cost:
		if inputData[key]["Type"] == "REACT":
			ws.cell(row=current_row, column=name_col_index).value = key
			current_row += 1

	wb.save(filename)
			

def inputRTX(inputData, cost):
	excelReactorData = {};
	parseReact(excelReactorData)
	for key in cost:
		if (inputData[key]["Type"] == "REACT"):
			if key in excelReactorData:
				cost[key]["input"]["EQUIPMENT COST"] = excelReactorData[key]
			else:
				inputReactorName(inputData, cost)
				raise Exception("reactor의 가격을 엑셀 시트에 이름에 맞게 입력해주세요")
