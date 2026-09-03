from Utility import checkType
from data import lawMaterialCostData, lawMaterialWeightData, utilityCostData, calcOPEXdata, profitAnalysisData, outputFlowData, equipmentConfig
from enums import Index

def equipmentSetting(key):
	return equipmentConfig.get("equipment", {}).get(key, {})

def defaultFormulaCost(key, cost):
	"""
	장치 key에 대해 프로젝트에서 지정한 '기본 수식'의 계산된 원가를 돌려준다.
	기본 수식이 지정되지 않았거나 계산된 후보 목록에 없으면(설정이 비어있는 예외 상황),
	계산된 후보 중 아무거나 방어적으로 사용한다.
	"""
	formulaName = equipmentSetting(key).get("defaultFormula")
	candidates = cost.get(key, {})
	if formulaName and formulaName in candidates:
		return candidates[formulaName]["EQUIPMENT COST"]
	for name, values in candidates.items():
		if name != "ATEA" and "EQUIPMENT COST" in values:
			return values["EQUIPMENT COST"]
	return 0

def calCAPEX(inputData, cost, CAPEX):
	#CAPEX 출력 순서 지정을 위한 전방선언
	CAPEX["CAPEX"] = ["  ", "  "]
	CAPEX[" "] = ["  ", "  "]
	CAPEX["CLASSIFICATION"] = ["% of FCI", "[USD/yr]"]
	CAPEX[" "] = ["  ", "  "]
	CAPEX["Direct cost"] = ["  ", "  "]
	CAPEX["ISBL (Inside battery limit, 전체공사구역 중 주 공정시설)"] = ["  ", "  "]
	CAPEX["Equipment cost"] = ["20-40", 0]
	CAPEX["Installation of equipment "] = ["7.3-26"]
	CAPEX["Instrument and control"] = ["2.5-7.0"]
	CAPEX["Piping"] = ["3.0-15"]
	CAPEX["Electrical"] = ["2.5-9.0"]
	CAPEX["  "] = ["  ", "  "]
 
	CAPEX["OSBL(Outside bettery limit,주공정시설 외 부대시설)"] = ["  ", "  "]
	CAPEX["Building and building services"] = ["6.0-20"]
	CAPEX["Yard improvements"] = ["1.5-5.0"]
	CAPEX["Services facilities"] = ["8.0-35"]
	CAPEX["Land"] = ["1.0-2.0"]
	CAPEX[" "] = ["  ", "  "]

	CAPEX["Total direct cost"] = ["  "]
	CAPEX["   "] = ["  ", "  "]

	CAPEX["Indirect cost"] = ["  ", "  "]
	CAPEX["Engineering"] = ["4.0-21"]
	CAPEX["Construction expenses"] = ["4.8-22"]
	CAPEX["Contractor's fee"] = ["1.5-5.0"]
	CAPEX["Contingency"] = ["5.0-20"]
	CAPEX["    "] = ["  ", "  "]
 
	CAPEX["Total indirect cost"] = ["  "]
	CAPEX["     "] = ["  ", "  "]
 
	CAPEX["Fixed capital investment (FCI)"] = ["100"]
	CAPEX["Start up cost (SUC)"] = ["10"]
	CAPEX["      "] = ["  ", "  "]
 
	CAPEX["Total capital investment (Capex)"] = ["TCI"]
	CAPEX["Annualized capital cost (r=5%, t=30 year)"] = ["EAC", 0]

	for key in cost:
		if equipmentSetting(key).get("skipCost", False):
			# print(key, "은(는) 설비비 계산에서 제외된 장비입니다.")
			continue
		if (inputData[key]["Type"] == "REACT"):
			# print(key, inputData[key]["Type"], cost[key]["input"]["EQUIPMENT COST"])
			CAPEX["Equipment cost"][1] += int(cost[key]["input"]["EQUIPMENT COST"])
		elif(inputData[key]["Type"] == "HEX"):
			CAPEX["Equipment cost"][1] += int(defaultFormulaCost(key, cost))
		elif(inputData[key]["Type"] == "HTX"):
			if ("ATEA" in cost[key] and key != "OLINHEA"):
				# print(key, cost[key]["ATEA"]["EQUIPMENT COST"])
				CAPEX["Equipment cost"][1] += int(cost[key]["ATEA"]["EQUIPMENT COST"])
			else:
				CAPEX["Equipment cost"][1] += int(defaultFormulaCost(key, cost))
		elif (checkType(key) == "COMP"):
			CAPEX["Equipment cost"][1] += int(defaultFormulaCost(key, cost))
		elif ("ATEA" in cost[key]):
			# print(key, cost[key]["ATEA"]["EQUIPMENT COST"])
			CAPEX["Equipment cost"][1] += int(cost[key]["ATEA"]["EQUIPMENT COST"])
	CAPEX["Fixed capital investment (FCI)"].append(CAPEX["Equipment cost"][1] * 100 / 40)
	CAPEX["Start up cost (SUC)"].append(CAPEX["Fixed capital investment (FCI)"][1] * 0.1)

	CAPEX["Installation of equipment "].append(CAPEX["Fixed capital investment (FCI)"][1] * 0.08)
	CAPEX["Instrument and control"].append(CAPEX["Fixed capital investment (FCI)"][1] * 0.05)
	CAPEX["Piping"].append(CAPEX["Fixed capital investment (FCI)"][1] * 0.03)
	CAPEX["Electrical"].append(CAPEX["Fixed capital investment (FCI)"][1] * 0.05)
	
	CAPEX["Building and building services"].append(CAPEX["Fixed capital investment (FCI)"][1] * 0.07)
	CAPEX["Yard improvements"].append(CAPEX["Fixed capital investment (FCI)"][1] * 0.02)
	CAPEX["Services facilities"].append(CAPEX["Fixed capital investment (FCI)"][1] * 0.08)
	CAPEX["Land"].append(CAPEX["Fixed capital investment (FCI)"][1] * 0.02)
 
	CAPEX["Total direct cost"].append(CAPEX["Equipment cost"][1] + CAPEX["Installation of equipment "][1] + CAPEX["Instrument and control"][1] + CAPEX["Piping"][1] + CAPEX["Electrical"][1] + CAPEX["Building and building services"][1] + CAPEX["Yard improvements"][1] + CAPEX["Services facilities"][1] + CAPEX["Land"][1])
 
	CAPEX["Engineering"].append(CAPEX["Fixed capital investment (FCI)"][1] * 0.05)
	CAPEX["Construction expenses"].append(CAPEX["Fixed capital investment (FCI)"][1] * 0.05)
	CAPEX["Contractor's fee"].append(CAPEX["Fixed capital investment (FCI)"][1] * 0.05)
	CAPEX["Contingency"].append(CAPEX["Fixed capital investment (FCI)"][1] * 0.05)

	CAPEX["Total indirect cost"].append(CAPEX["Engineering"][1] + CAPEX["Construction expenses"][1] + CAPEX["Contractor's fee"][1] + CAPEX["Contingency"][1])
	
	CAPEX["Total capital investment (Capex)"].append(CAPEX["Start up cost (SUC)"][1] + CAPEX["Fixed capital investment (FCI)"][1])
	CAPEX["Annualized capital cost (r=5%, t=30 year)"][1] = (CAPEX["Total capital investment (Capex)"][1] / ((1 - (1 / ((1.05)**30)))/0.05))

def calUtility(utility):
	for key in utility:
		selectedTypes = equipmentSetting(key).get("utilityTypes")
		if selectedTypes is None:
			# 설정이 없는 장치(예: 파싱 스냅샷에 없던 예외 상황)는 방어적으로 전부 계산
			selectedTypes = ["COOLING", "HOT", "ELECTRICITY", "MPSG"]

		if ("MPSG_rate[KG/HR]" in utility[key]):
			if "MPSG" in selectedTypes:
				usage = utility[key]["MPSG_rate[KG/HR]"]
				annualUsage = usage * int(calcOPEXdata["plantOperationHours"]) #kg/year
				utility[key]["MPSG UTILITY ANNUAL USAGE [kg/year]"] = int(annualUsage)
				annualCost = utilityCostData["StreamPrice(MPS)"] * utility[key]["MPSG UTILITY ANNUAL USAGE [kg/year]"]
				utility[key]["MPSG UTILITY ANNUAL COST [USD/year]"] = int(annualCost)
				print(key, "MPSG UTILITY ANNUAL COST [USD/year]", utility[key]["MPSG UTILITY ANNUAL COST [USD/year]"])
			continue # MPSG 데이터가 있는 장치는 이중계산 방지를 위해 선택 여부와 무관하게 HOT UTILITY를 계산하지 않음.
		if "COOLING UTILITY[kg/hr]" in utility[key] and "COOLING" in selectedTypes:
			usage = utility[key]["COOLING UTILITY[kg/hr]"]
			if (usage < 0):
				usage = -1 * usage
			annualUsage = usage * calcOPEXdata["plantOperationHours"] #kg/year
			utility[key]["COOLING UTILITY ANNUAL USAGE [kg/year]"] = int(annualUsage)
			utilityCost = usage * utilityCostData["CoolingWaterPrice"] #USD/hr
			utility[key]["COOLING UTILITY UTILITY COST [USD/hr]"] = int(utilityCost)
			annualCost = utilityCost * calcOPEXdata["plantOperationHours"] #USD/year
			utility[key]["COOLING UTILITY ANNUAL COST [USD/year]"] = int(annualCost)
			print(key, "COOLING UTILITY ANNUAL COST [USD/year]", utility[key]["COOLING UTILITY ANNUAL COST [USD/year]"])

		if "HOT UTILITY[kW]" in utility[key] and "HOT" in selectedTypes:
			duty = utility[key]["HOT UTILITY[kW]"] / 0.7 # 열교환기의 효율을 0.7로 가정
			if (duty < 0):
				duty = -1 * duty
			annualDuty = duty * calcOPEXdata["plantOperationHours"] #kWh/year
			utility[key]["HOT UTILITY ANNUAL DUTY [kWh/year]"] = int(annualDuty)
			annualCost = annualDuty * utilityCostData["NGprice"]  #USD/year
			utility[key]["HOT UTILITY ANNUAL COST [USD/year]"] = int(annualCost)
			print(key, "HOT UTILITY ANNUAL COST [USD/year]", utility[key]["HOT UTILITY ANNUAL COST [USD/year]"])

		if "ELECTRICITY UTILITY[kW]" in utility[key] and "ELECTRICITY" in selectedTypes:
			usage = utility[key]["ELECTRICITY UTILITY[kW]"]
			annualUsage = usage * calcOPEXdata["plantOperationHours"] #kWh/year
			utility[key]["ELECTRICITY UTILITY ANNUAL USAGE [kWh/year]"] = int(annualUsage)
			annualCost = annualUsage * utilityCostData["electricityCostPerKWH"] #USD/year
			utility[key]["ELECTRICITY UTILITY ANNUAL COST [USD/year]"] = int(annualCost)
			print(key, "ELECTRICITY UTILITY ANNUAL COST [USD/year]", utility[key]["ELECTRICITY UTILITY ANNUAL COST [USD/year]"])


def calOPEX(CAPEX, flowData, OPEX, utility):

	#OPEX 출력 순서 지정을 위한 전방선언
	OPEX["OPEX (Total product costm TPC)"] = [" " ," "]
	OPEX[" "] = [" " ," "]
	OPEX["CLASSIFICATION"] = ["(REF Range)", "[USD/yr]"]
	OPEX[" "] = [" " ," "]
	OPEX["Fixed charge(FC)"] = [" "]
	OPEX["Local taxes, Insurance"] = ["1~4% of FCI"]
	OPEX[" "] = [" " ," "]
 
	OPEX["Direct production cost (DPC)"] = [" "]
	OPEX["Raw materials"] = [" "]
	OPEX["Utility"] = [" "]
	OPEX["Matinenenance (M)"] = ["1~10% of FCI"]
	OPEX["Operating labor (OL)"] = ["10~20% of OPEX"]
	OPEX["Supervision and support labor (S)"] = ["30 or 15(peter)% of OL"]
	OPEX["Operating supplies"] = ["10~20% of M"]
	OPEX["Laboratory charges"] = ["10~20% of OL"]
	OPEX["  "] = [" " ," "]
 
	OPEX["Plant overhead cost(OVHD)"] = ["50~70% of M+OL+S"]
	OPEX["   "] = [" " ," "]
 
	OPEX["General expenses"] = [" "]
	OPEX["Admistrative cost"] = ["15~20% of OL"]
	OPEX["Distribution and marketing"] = ["2~20% of OPEX"]
	OPEX["R&D cost"] = ["2~5% of OPEX"]
 
	OPEX["    "] = [" " ," "]
	OPEX["OPEX"] = [" "]

	OPEX["Fixed charge(FC)"].append(CAPEX["Fixed capital investment (FCI)"][1] * 0.01)
	OPEX["Local taxes, Insurance"].append(CAPEX["Fixed capital investment (FCI)"][1] * 0.01)

	OPEX["Raw materials"] = [" ", 0] # 이거 raw material key에 따른 알맞은 값 넣어야함.
	inputFlow = flowData["inputFlow"]
	for key in inputFlow:
		OPEX["Raw materials"][1] += inputFlow[key]["amount"] * inputFlow[key]["cost"] * calcOPEXdata["plantOperationHours"] / 1000  # ton 단위로 바꿔주기 위해 1000으로 나눔
			# OPEX["Raw materials"][1] += flowData[key] * lawMaterialCostData[key] * calcOPEXdata["plantOperationHours"] * -1 * lawMaterialWeightData[key]  # kg 단위로 바꿔주기 위해 1000으로 나눔
	OPEX["Utility"] = [" ", 0]
	for key in utility:
		if "ELECTRICITY UTILITY ANNUAL COST [USD/year]" in utility[key] and utility[key]["ELECTRICITY UTILITY ANNUAL COST [USD/year]"] > 0:
			OPEX["Utility"][1] += utility[key]["ELECTRICITY UTILITY ANNUAL COST [USD/year]"]
			# print(key, utility[key]["ELECTRICITY UTILITY ANNUAL COST [USD/year]"])
		if "COOLING UTILITY ANNUAL COST [USD/year]" in utility[key] and utility[key]["COOLING UTILITY ANNUAL COST [USD/year]"] > 0:
			OPEX["Utility"][1] += utility[key]["COOLING UTILITY ANNUAL COST [USD/year]"]
			# print(key, utility[key]["COOLING UTILITY ANNUAL COST [USD/year]"])
		if "HOT UTILITY ANNUAL COST [USD/year]" in utility[key] and utility[key]["HOT UTILITY ANNUAL COST [USD/year]"] > 0:
			if ("MPSG UTILITY ANNUAL COST [USD/year]" not in utility[key]):
				OPEX["Utility"][1] += utility[key]["HOT UTILITY ANNUAL COST [USD/year]"]
				# print(key, utility[key]["HOT UTILITY ANNUAL COST [USD/year]"])
		if "MPSG UTILITY ANNUAL COST [USD/year]" in utility[key] :
			OPEX["Utility"][1] += utility[key]["MPSG UTILITY ANNUAL COST [USD/year]"]
			# print(key, utility[key]["MPSG UTILITY ANNUAL COST [USD/year]"])
		# elif "MPS UTILITY ANNUAL COST [USD/year]" in utility[key] and utility[key]["MPS UTILITY ANNUAL COST [USD/year]"] > 0:
		# 	OPEX["Utility"][1] += utility[key]["MPS UTILITY ANNUAL COST [USD/year]"]

	OPEX["Matinenenance (M)"].append(CAPEX["Fixed capital investment (FCI)"][1] * 0.01)
	OPEX["Operating supplies"].append(OPEX["Matinenenance (M)"][1] * 0.1)



	OPEX["OPEX"].append(1.35135135135 * (CAPEX["Fixed capital investment (FCI)"][1] * 0.026 + (OPEX["Utility"][1] + OPEX["Raw materials"][1])))
	OPEX["Operating labor (OL)"].append(OPEX["OPEX"][1] * 0.1)
	OPEX["Supervision and support labor (S)"].append(OPEX["Operating labor (OL)"][1] * 0.3)
	OPEX["Laboratory charges"].append(OPEX["Operating labor (OL)"][1] * 0.1)
	OPEX["Plant overhead cost(OVHD)"].append(0.5 * (OPEX["Matinenenance (M)"][1] + OPEX["Operating labor (OL)"][1] + OPEX["Supervision and support labor (S)"][1]))
	# OPEX["Direct production cost (DPC)"].append(OPEX["Operating labor (OL)"][1] * 0.1)
	OPEX["Direct production cost (DPC)"].append(OPEX["Raw materials"][1] + OPEX["Utility"][1] + OPEX["Matinenenance (M)"][1] + OPEX["Operating labor (OL)"][1] + OPEX["Supervision and support labor (S)"][1] + OPEX["Operating supplies"][1] + OPEX["Laboratory charges"][1])

	OPEX["Admistrative cost"].append(OPEX["Operating labor (OL)"][1] * 0.15)
	OPEX["Distribution and marketing"].append(OPEX["OPEX"][1] * 0.02)
	OPEX["R&D cost"].append(OPEX["OPEX"][1] * 0.02)
	OPEX["General expenses"].append(OPEX["Admistrative cost"][1] + OPEX["Distribution and marketing"][1] + OPEX["R&D cost"][1])

def calProfitAnalysis(CAPEX, OPEX, profitAnalysis, flowData):
	product = ""

	outputFlow = flowData["outputFlow"]
	profitAnalysis[" "] = product
	profitAnalysis["OPEX"] = OPEX["OPEX"][1]
	profitAnalysis["Depreciation [USD/yr]"] = CAPEX["Fixed capital investment (FCI)"][1] / profitAnalysisData["depreciationLifetime"]
	for output_stream in outputFlow:
		annualAmount = outputFlow[output_stream] * calcOPEXdata["plantOperationHours"] / 1000
		profitAnalysis[output_stream + " annual amount of product [ton/yr]"] = annualAmount
		# for key in material:
			# profitAnalysis[output_stream + " annual amount of product [ton/yr]"] += flowData[key] * calcOPEXdata["plantOperationHours"] * lawMaterialWeightData[key] / 1000  # ton/yr
		# profitAnalysis[output_stream + " manufacturing cost [USD/ton]"] = 0
		if annualAmount == 0:
			# input.rep에서 stream을 못 찾아 생산량이 0으로 처리된 경우(Parse.py의 parseFlowName 참고) 0으로 나눌 수 없어 건너뜀
			profitAnalysis[output_stream + " manufacturing cost [USD/ton]"] = None
		else:
			profitAnalysis[output_stream + " manufacturing cost [USD/ton]"] = (profitAnalysis["OPEX"] + profitAnalysis["Depreciation [USD/yr]"]) / annualAmount
