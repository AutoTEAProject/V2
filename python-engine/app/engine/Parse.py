#!/opt/anaconda3/envs/myenv/bin/python
import pandas as pd
import datetime
import math
from Utility import checkType, normalizeName
from enums import Index
from data import lawMaterialCostData, utilityCostData

def parseFlowData(filename, flowName):
	fd2 = open(filename, mode='r')
	lines = fd2.readlines()
	data = {}
	inBlock = False
	start = False
	find = False
	for line in lines:
		if ("                                 STREAM SECTION                                 " in line):
			inBlock = True
			continue
		if (inBlock == True and "STREAM ID" in line):
			idx = 0
			streamId = list(line.split())
			for name in streamId:
				if (name == flowName):
					find = True
					idx = len(streamId) - idx - 1
					break
				idx += 1
			continue
		if (inBlock == True and "TOTAL FLOW:" in line):
			start = True
		if (start == True and find == True and "KG/HR" in line):
			materialData = list(line.split())
			material = materialData[0]
			weight = float(materialData[len(materialData) - 1 - idx].replace("-", "e-").replace("+", "e+")) # 단위 KG/HR
			data[material] = weight
			return (weight)


def parseFlowName(flowData):
	xlsxfilename = "./input/MaterialData.xlsx"
	repfilename = "./input/input.rep"
	df = pd.read_excel(io = xlsxfilename, sheet_name='FlowName', header=1, engine='openpyxl')
	length = len(df)
	inputFlow = {}
	outputFlow = {}
	
	for i in range(length):
		inputFlowName = df.iat[i, 1]
		outputFlowName = df.iat[i, 4]
		if (pd.isna(inputFlowName) and pd.isna(outputFlowName)):
			break;
		if (pd.isna(inputFlowName) == False):
			cost = float(df.iat[i, 2])
			if (pd.isna(cost)):
				raise TypeError("input flow의 가격을 입력해 주세요. : " + inputFlowName)
			inputFlow[inputFlowName] = {"amount" : parseFlowData(repfilename, inputFlowName), "cost" : cost} # 단위 KG/HR
		if (pd.isna(outputFlowName) == False):
			outputFlow[outputFlowName] = parseFlowData(repfilename, outputFlowName) # 단위 KG/HR
	flowData["inputFlow"] = inputFlow
	flowData["outputFlow"] = outputFlow
	
# def parseCapacity(exceptCapacity):
# 	xlsxfilename = "./input/MaterialData.xlsx"
# 	df = pd.read_excel(io = xlsxfilename, sheet_name='Capacity', header=1, engine='openpyxl')
# 	length = len(df)

# 	for i in range(length):
# 		EquipmentName = df.iat[i, 1]
# 		capacity = df.iat[i, 2]
# 		if (pd.isna(EquipmentName) and pd.isna(capacity)):
# 			break;
# 		if (pd.isna(EquipmentName) == False):
# 			exceptCapacity[EquipmentName] = float(capacity)

'''
                               FLOWSHEET SECTION                                

 OVERALL FLOWSHEET BALANCE (CONTINUED)               

                      ***  MASS AND ENERGY BALANCE  ***
                              IN          OUT       GENERATION   RELATIVE DIFF.
   CONVENTIONAL COMPONENTS  
           (KMOL/HR )
      WATER               0.00000       0.00000       0.00000       0.00000    
      CO2                 0.00000       0.00000       0.00000       0.00000    
      H2                  2530.33       689.009      -1841.32      0.128499E-13
      O2                  17.1253       17.1253       0.00000      0.439804E-13
      N2                  846.146       232.373      -613.773      0.130664E-13
      NH3                 0.00000       1227.55       1227.55     -0.361191E-13
      ARGON               0.00000       0.00000       0.00000       0.00000    
      AMMONIUM            0.00000       0.00000       0.00000       0.00000    
      UREA                0.00000       0.00000       0.00000       0.00000    
   TOTAL BALANCE
   MOLE(KMOL/HR )         3393.60       2166.05      -1227.55       0.00000    
   MASS(KG/HR   )         29352.3       29352.3                   -0.121463E-13
   ENTHALPY(CAL/SEC )     276112.     -0.582513E+07                 1.04740    
여기서 가져와야함
'''
def parseLawMaterial(filename, lawMaterialData):
	fd = open(filename, mode='r')
	lines = fd.readlines()

	inBlock = False
	for line in lines:
		if 'CONVENTIONAL COMPONENTS' in line:
			inBlock = True
			continue
		if (inBlock and "ASPEN PLUS" in line):
			inBlock = False
		if inBlock and 'TOTAL BALANCE' in line:
			return (lawMaterialData)
		if inBlock:
			line = line.strip()
			parts = line.split()
			if len(parts) == 5 and float(parts[3]) != 0.0:
				if float(parts[3]) < 0 :
					if (parts[0] in lawMaterialCostData):
						lawMaterialData[parts[0]] = float(parts[3])
					# print("%s 물질이 투입물이 맞습니까? (y/n) :" % parts[0], end='')
					# answer = input()
					# if answer == 'y':
					# 	lawMaterialData[parts[0]] = float(parts[3])
					'''
						lawMaterialData[parts[0]] = float(parts[3])
					if parts[0] in inputMaterialWeightData or parts[0] in outputMaterialWeightData:
						'''
				elif float(parts[3]) > 0 :
					if (parts[0] in lawMaterialCostData):
						lawMaterialData[parts[0]] = float(parts[3])
					# print("%s 물질이 생성물이 맞습니까? (y/n) :" % parts[0], end='')
					# answer = input()
					# if answer == 'y':
					# 	lawMaterialData[parts[0]] = float(parts[3])
# error
# 여기서 이제 각 원자재의 가격을 입력받을 것인지 아니면 코드에 박아넣을 것인지 결정해야함.



# inputData = [] # 이걸 밖에서 선언해둬야함.
def parseTEA(filename, inputData):
	
	pd.set_option("display.max_rows", None)       # 모든 행 보이기
	pd.set_option("display.max_columns", None)    # 모든 열 보이기
	pd.set_option("display.width", None)          # 줄바꿈 없이 가로로 다 보여주기
	pd.set_option("display.max_colwidth", None)   # 열 안 문자열도 끝까지 다 보여주기
	
	df = pd.read_excel(io = filename, sheet_name='Unit operation', usecols='C:H', header=3, engine='openpyxl')
	# 이 시트 세로열은 C:H가 고정이다. -> 5개의 value 가져오는게 고정이라서.
	for i in range(0, len(df), 1): #C랑 C++에 익숙해 2차원 딕셔너리를 생각을 못 했다 이게 더 간단할듯 근데 그거나 그거나 비슷함
		temp = {} # 저장할 데이터 길이가 8이다.
		NameIdx = 0;
		if (isinstance(df.iat[i, NameIdx], datetime.datetime)):
			continue
		temp["Name"] = normalizeName(df.iat[i, NameIdx]) # . 이후가 진짜 이름인 경우가 있어 이렇게 분리
		temp["EquipmentCost"] = float(df.iat[i, Index.EquipmentCostIdx]) # type 확인해서 <class 'numpy.int64'>면 int로, <class 'numpy.float64'>면 float으로 형변환하면 더 좋다.
		temp["InstalledCost"] = float(df.iat[i, Index.InstalledCostIdx])
		temp["EquipmentWeight"] = float(df.iat[i, Index.EquipmentWeightIdx])
		temp["InstalledWeight"] = float(df.iat[i, Index.InstalledWeightIdx])
		temp["UtilityCost"] = float(df.iat[i, Index.UtilityCostIdx])
		temp["HeatTransferArea"] = 0.0 
		temp["DriverPower"] = 0.0
		temp["Type"] = ""
		inputData[temp["Name"]] = temp
	return (inputData)

# 이제 REACT, HTX, HEX, COMP. FLASH, MIX 이렇게 종류별로 저장해둬야함
# 이름 + 다섯 종류의 가격을 나타내야함. -> 2차원 배열로 저장하자(파이썬의 배열은 자료형이 전부 달라도 한 배열에 저장 가능하다
# Name, EquipmentCost, InstalledCost, EquipmentWeight, InstalledWeight, UtilityCost 순서로 저장하고, 배열의 인덱스를 저 이름으로 접근가능하게 enum 설정해서 가독성 높이자

def parseHEX(filename, inputData):
	# TEMA HEX 시트의 각 Heat exchanger별 Heat transfer area [sqm]를 저장해둬야한다.
	df = pd.read_excel(io = filename, sheet_name='TEMA HEX', header=1, engine='openpyxl')
	df = df.iloc[:, 2:]  # C열(인덱스 2)부터 끝까지
	length = len(df.columns)
 
	for i in range(0, length):
		name = normalizeName(df.iat[1, i])
		area = df.iat[8, i]
		for key in inputData:
			if (inputData[key]["Name"] == name):
				if (area != "nan" and not math.isnan(area)):
					# print(area)
					inputData[key]["HeatTransferArea"] = float(area)
					# print(key + " " + str(area))
	    			# HTX는 이 값으로 구하는 거 아니라서 변경해야함.Capacity (kW)를 활용함.. 또 쿨러는 뭔가 다른 것 같은데.. 우선 계산된 값은 건드리지 말자.
				break

def parseCOMP(filename, inputData):
	# compressor의 power도 알아둬야함.
	df = pd.read_excel(io = filename, sheet_name='Centrif gas compr', header=1, engine='openpyxl')
	df = df.iloc[:, 3:]  # C열(인덱스 2)부터 끝까지
	length = len(df.columns) # 가로행 길이 구하기

	for i in range(0, length):
		name = normalizeName(df.iat[1, i])
		area = df.iat[8, i]
		power = df.iat[14, i] 
		for key in inputData:
			if (inputData[key]["Name"] == name):
				if (area != "nan"):
					inputData[key]["DriverPower"] = float(power)
				break



def parseCAPCOSTParam(refFilename, inputData):
	fd = open(refFilename, mode='r')
	lines = fd.readlines()

	for line in lines:
		if ("BLOCK:" in line):
			temp = list(line.split("  "))
			temp2 = temp[1].split(": ")
			temp2 = temp2[0].split(" ")
			name = temp2[0]
		if ("RATE OF CONSUMPTION" in line):
			temp = list(line.split())
			temp3 = list(temp[3].split('+'))
			rate = float(temp3[0])
			if (len(temp3) > 1):
				for i in range(0, int(temp3[1])):
					rate *=  10
			for key in inputData:
				if (inputData[key]["Name"] == name):
					if (inputData[key]["Type"] == "HTX"):
						inputData[key]["HeatTransferArea"] = rate
					elif (inputData[key]["Type"] == "COMP"):
						if (inputData[key]["DriverPower"] == 0.0):
							inputData[key]["DriverPower"] = rate

def parseEQUIP(refFilename, inputData):
	fd = open(refFilename, mode='r')
	lines = fd.readlines()
	flag = False

	for line in lines:
		if ("TABLE OF CONTENTS" in line):
			flag = True
		if ("  BLOCK:" in line):
			temp = list(line.split())
			temp2 = temp[3].split(".")
			parseName = normalizeName(temp[1])
			blockType = temp2[0]
			for key in inputData:
				if (key == parseName):
					inputData[key]["Type"] = checkType(blockType)
					# if (inputData[key]["Type"] == "REACT"):
					# 	reactorParam.append(inputData[key]["Name"])


'''
 BLOCK:  FURNACE  MODEL: RSTOIC          
 ------------------------------
   INLET STREAMS:         AIR         DIST1TOP    LIGHTPUR
   OUTLET STREAM:         COMBOUT 
   PROPERTY OPTION SET:   RKS-BM    REDLICH-KWONG-SOAVE EQUATION OF STATE       

                      ***  MASS AND ENERGY BALANCE  ***
                              IN          OUT       GENERATION   RELATIVE DIFF.
   TOTAL BALANCE
   MOLE(KMOL/HR )         12830.3       13120.1       289.853      0.138641E-15
   MASS(KG/HR   )         374568.       374568.                     0.00000    
   ENTHALPY(CAL/SEC )   -0.342655E+07 -0.342655E+07                 0.00000    
'''
# def parseReactor(inputData, repfFileName, utility):
# 	fd = open(repfFileName, mode='r')
# 	lines = fd.readlines()
# 	blockflag = 0
# 	startflag = False
# 	for line in lines:
# 		if (blockflag == 1):
# 			if ("-----------------------" in line):
# 				blockflag = 2
# 			else:
# 				blockflag = 0
# 			continue
# 		if (blockflag == 3):
# 			blockflag += 1
# 			continue
# 		if (blockflag == 4):
# 			blockflag = 0
# 			startflag = True
# 			continue;
# 		if (blockflag == 2 and "TOTAL BALANCE" in line):
# 			blockflag += 1
# 			continue;
# 		if (startflag == True):
# 			line = line.strip()
# 			parts = line.split()
# 			temp3 = list(parts[2].split('+'))
# 			temp = temp3[0][:-1]
# 			print(temp)
# 			num = float(temp)
# 			if (len(temp3) > 1):
# 				for i in range(int(temp3[1])):
# 					num *=  10
# 			print(num)
# 			capacity = num * -0.004184 # 단위 kW로 변환
# 			reactorParam[name]["Capacity_parsed KW"] = capacity
# 			print(name + " " + str(capacity) + " KW")
# 			startflag = False;
# 		if (" BLOCK:  " in line and "MODEL: " in line):
# 			line = line.strip()
# 			parts = line.split()
# 			name = parts[1]
# 			for key in reactorParam:
# 				if (key == name):
# 					print(line)
# 					if blockflag == 0:
# 						blockflag = 1
# 						break;
		
# 	print(reactorParam)

def parseMPSG(inputData, repfFileName, utility):
	fd = open(repfFileName, mode='r')
	lines = fd.readlines()
	blockflag = 0;
	startflag = False;
 
	for line in lines:
		if (startflag == True):
			endflag = True
			line = line.strip()
			parts = line.split()
			for key in inputData:
				if (inputData[key]["Name"] == parts[0]):
					endflag = False
			# 이제 MPSG 저장하면 됨
			name = normalizeName(parts[0])
			for key in inputData:
				if (inputData[key]["Name"] == name):
						temp3 = list(parts[3].split('+'))
						num = float(temp3[0])
						if (len(temp3) > 1):
							for i in range(int(temp3[1])):
								num *=  10
						MPSG_rate = num * -1
						utility[name].update({
							"MPSG_rate[KG/HR]": MPSG_rate
						})
			if (endflag == True):
				break;
		if ("UTILITY USAGE:" in line and "MPSG" in line):
			blockflag += 1
		if (blockflag == 2 and "  --------  " in line):
			startflag = True
			
def parseMPS(inputData, repfFileName, utility):
	fd = open(repfFileName, mode='r')
	lines = fd.readlines()
	blockflag = 0;
	startflag = False;
 
	for line in lines:
		if (startflag == True):
			endflag = True
			line = line.strip()
			parts = line.split()
			for key in inputData:
				if (inputData[key]["Name"] == parts[0]):
					endflag = False
			# 이제 MPS 저장하면 됨
			name = normalizeName(parts[0])
			for key in inputData:
				if (inputData[key]["Name"] == name):
						temp3 = list(parts[3].split('+'))
						num = float(temp3[0])
						if (len(temp3) > 1):
							for i in range(int(temp3[1])):
								num *=  10
						MPS_rate = num
						MPS_percost = num * utilityCostData["StreamPrice(MPS)"]
						utility[name].update({
							"MPS_rate[KG/HR]": MPS_rate, 
							"MPS UTILITY COST [USD/HR]": MPS_percost
						})
			if (endflag == True):
				break;
		if ("UTILITY USAGE:" in line and "MPS " in line):
			blockflag += 1
		if (blockflag == 2 and "  --------  " in line):
			startflag = True
			
'''
	이제 Utility값 파싱해서 저장하는 부분
	장치별로 COOLING UTILITY, HOT UTILITY, ELECTRICITY UTILITY를 저장해야한다.
	COOLING UTILITY는 UTILITY USAGE [kg/hr], ANNUAL USAGE [kg/year], UTILITY COST [USD/hr], ANNUAL COST [USD/year]로 이루어져 있고
	HOT UTILITY는 DUTY [kW], ANNUAL DUTY [kWh/year], REQUIRED Utility [kg/hr], ANNUAL COST [USD/year]로 이루어져 있고, 
	ELECTRICITY UTILITY는 UTILITY USAGE [kW], ANNUAL USAGE [kWh/year], ANNUAL COST [USD/year]로 이루어져 있다.
	기계 종류별로 정해져 있는 utility가 아니라 각 기계에서 어떤 일을 하는지에 따라 어떤 유틸리티를 저장하는지 달라진다.
'''

''' 
	Aspen에는             
 				 UTILITY SECTION........................................ 37
                 UTILITY USAGE:  COOLINGW  (WATER)................. 37
                 UTILITY USAGE:  ELECTRO   (ELECTRICITY)........... 38
                 UTILITY USAGE:  FIRE1000  (GENERAL)............... 39
                 UTILITY USAGE:  HOTOIL    (GENERAL)............... 40
    다음과같이 섹션이 나눠져 있다.
    
    BLOCK:  H-COMP-1 MODEL: COMPR (CONTINUED)  에
    UTILITY ID FOR ELECTRICITY               ELECTRO
  	RATE OF CONSUMPTION                    4153.8584  KW              
  	COST                                    321.9240  $/HR            
  	CO2 EQUIVALENT EMISSIONS               1296.6054  KG/HR  와 같이 나와있다.
   
   여기서 COMSUMPTION과 COST를 활용하여 원하는 utility값을 뽑아내면 완료
   
   1. COOLING의 Water 사용량은 COOLINGW 검색해서 아래에 나오는 사용량 뽑아내면 됨
     UTILITY ID FOR WATER                    COOLINGW
  	 RATE OF CONSUMPTION                    3.7253+05  KG/HR  
    
   2. HEATING의 DUTY 사용량은 HEAT DUTY  CAL/SEC  0.32239E+06
   	  에서 해당 CAL을 4.184 J을 활용하여 변환하고 KW 단위로 변경하면 된다.
                                 ***  RESULTS  ***
   	OUTLET TEMPERATURE    C                                    420.00    
   	OUTLET PRESSURE       BAR                                  274.00    
   	HEAT DUTY             CAL/SEC                             0.32239E+06
   	OUTLET VAPOR FRACTION                                      1.0000   
   
   3. ELECTRICITY는  UTILITY ID FOR ELECTRICITY 에서 다음 줄인
   	  RATE OF CONSUMPTION  의 값을 읽어오면 된다.
      
      UTILITY ID FOR ELECTRICITY               ELECTRO
  	  RATE OF CONSUMPTION                    4153.8584  KW 
    
    ** HEX의 유틸리티값은 파일에서도 제외되어 있어서 일단 뺌
    ** COOLER는 HEAT DUTY + WATER CONSUMPTION 둘 다 있어서 COOLING있는 애면 DUTY 파싱하지 않기
    -> 이제 이걸로 Utility값 읽어와서 파일에 출력하면 끝
'''

def	parseUtility(inputData, repfFileName, utility, exceptCapacity):
	fd = open(repfFileName, mode='r')
	lines = fd.readlines()
	parseflag = 1
	for line in lines:
		if ("BLOCK:" in line):
			temp = list(line.split("  "))
			temp2 = temp[1].split(": ")
			temp2 = temp2[0].split(" ")
			name = normalizeName(temp2[0])
			# print(name)
			for key in inputData:
				if (inputData[key]["Name"] == name):
					if (inputData[key]["Type"] == "COMP"):
						if (inputData[key]["DriverPower"] > 0):
							utility[name] = {
								"ELECTRICITY UTILITY[kW]": inputData[key]["DriverPower"]
							}
						if name in exceptCapacity:
							utility[name] = {
							"ELECTRICITY UTILITY[kW]": exceptCapacity[name]
							}
					# error
					# COMP TEA에서 계산해주면 그걸 우선으로 사용
						
					if (inputData[name]["Type"] == "HEX"):
						parseflag = 0
					else:
						parseflag = 1
		if ("UTILITY ID FOR WATER" in line):
			parseflag = 2
		if ("UTILITY ID FOR ELECTRICITY" in line):
			parseflag = 3
		if (" UTILITY USAGE:  CW        (WATER)       " in line):
			parseflag = 4
		
		if (parseflag == 4 and " TOTAL: " in line):
			temp = list(line.split())
			temp3 = list(temp[1].split('+'))
			num = float(temp3[0])
			if (len(temp3) > 1):
				for i in range(int(temp3[1])):
					num *=  10
			if (num < 0):
				num = num * -1
			utility[name] = {
				"COOLING UTILITY[kg/hr]": num
			}
			if name in exceptCapacity:
				utility[name] = {
				"COOLING UTILITY[kg/hr]": exceptCapacity[name]
				}
				# print(name + " " + str(exceptCapacity[name]) + " KG/HR")
			parseflag = 0
	
		if ((parseflag == 2 or parseflag == 3)and "RATE OF CONSUMPTION" in line):
			temp = list(line.split())
			temp3 = list(temp[3].split('+'))
			num = float(temp3[0])
			if (len(temp3) > 1):
				for i in range(int(temp3[1])):
					num *=  10
			if (num < 0):
				num = num * -1
			if (parseflag == 2):
				utility[name] = {
					"COOLING UTILITY[kg/hr]": num
				}
				if name in exceptCapacity:
					utility[name] = {
					"COOLING UTILITY[kg/hr]": exceptCapacity[name]
					}
			if (parseflag == 3):
				num = num * (0.0041868 / 0.004184) # 단위 kW로 변환
				utility[name] = {
					"ELECTRICITY UTILITY[kW]": num
				}
				if name in exceptCapacity:
					utility[name] = {
					"ELECTRICITY UTILITY[kW]": exceptCapacity[name]
					}
				
	
		if (parseflag == 1 and "HEAT DUTY" in line and "CAL/SEC" in line):
			temp = list(line.split("CAL/SEC"))
			for i in range(0, len(temp)):
				temp[i] = temp[i].replace(" ", "").replace('\n', "").replace(',', '')
			if (len(temp) > 1):
				temp3 = list(temp[1].split('E'))
				num = float(temp3[0])
				if (len(temp3) > 1):
					count = int(temp3[1])
					if (count < 0):
						for i in range(-count):
							num /=  10
					else:	
						for i in range(int(temp3[1])):
							num *=  10
			else:
				num = temp[0]
			# num 단위변환
			num *= 0.0041868 # CAL/SEC -> KW
			if (num < 0):
				num = num * -1
			utility[name] = {
				"HOT UTILITY[kW]":num
			}
			if name in exceptCapacity:
				utility[name] = {
				"HOT UTILITY[kW]": exceptCapacity[name]
				}
				# print(name + " " + str(exceptCapacity[name]) + " KW")


#이제 예쁘게 출력만 하면 완성이다~

def parseHotUtility(inputData, repfFileName, utility, exceptCapacity):
	fd = open(repfFileName, mode='r')
	lines = fd.readlines()
	blockflag = 0;
	startflag = False;
 
	for line in lines:
		if (startflag == True):
			endflag = True
			line = line.strip()
			parts = line.split()
			if ("================" in line):
				endflag = True
				startflag = False
				blockflag = 0
				return;
			for key in inputData:
				if (inputData[key]["Name"] == parts[0] and "HOT UTILITY[kW]" not in utility.get(key, {})):
					name = normalizeName(parts[0])
					temp3 = list(parts[2].split('+'))
					num = float(temp3[0])
					if (len(temp3) > 1):
						count = int(temp3[1])
						if (count < 0):
							for i in range(-count):
								num /=  10
						else:	
							for i in range(int(temp3[1])):
								num *=  10
					# num 단위변환
					num *= 0.0041868 # CAL/SEC -> KW
					if (num < 0):
						num = num * -1
					if name not in utility:
						utility[name] = {}
					utility[name]["HOT UTILITY[kW]"] = num
					if name in exceptCapacity:
						utility[name]["HOT UTILITY[kW]"] = exceptCapacity[name]
		if (" UTILITY USAGE:  FH1000    (GENERAL) " in line):
			blockflag += 1
			continue
		if (blockflag == 1 and "BLOCK ID" in line):
			startflag = True

