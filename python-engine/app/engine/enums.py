from enum import IntEnum

class EquipLen(IntEnum):
	MIXER = 5
	FSPLIT = 6
	SSPLIT = 6
	FLASH2 = 6
	FLASH3 = 6
	SEP = 3
	SEP2 = 4
	HEATER = 6
	HEATX = 5
	MHEATX = 6
	HXFLUX = 6
	DSTWU = 5
	DISTL = 5
	RADFRAC = 7
	PETROFRAC = 9
	RSTOIC = 6
	RYIELD = 6
	REQUIL = 6
	RGIBBS = 6
	RCSTR = 5
	RPLUG = 5
	PUMP = 4
	COMPR = 5
	COMP = 4
	MCOMPR = 6
	VALVE = 5
	CYCLONE = 7
	HYCYC = 5

class Index(IntEnum):
	COMPTYPE = 0
	K1 = 1
	K2 = 2
	K3 = 3
	FBMCS = 4
	QMIN = 4
	FBMSS = 5
	QMAX = 5
	FBMNI = 6
	PMAX = 6
	WMIN = 7
	C1 = 7
	WMAX = 8
	C2 = 8
	C3 = 9
	FBM = 10
	
	EquipmentCostIdx = 1
	InstalledCostIdx = 2
	EquipmentWeightIdx = 3
	InstalledWeightIdx = 4
	UtilityCostIdx = 5
	HeatTransferAreaIdx = 6
	DriverPowerIdx = 7
	TypeIdx = 8
