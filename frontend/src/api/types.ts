export type EquipmentType = 'HTX' | 'HEX' | 'COMP' | 'REACT'

export type UtilityType = 'COOLING' | 'HOT' | 'ELECTRICITY' | 'MPSG'

/** 장치비를 계산할 때 어떤 값을 우선 쓸지: 직접 등록한 수식(FORMULA, 기본값) vs Aspen이 준 값(ASPEN) */
export type CostSource = 'FORMULA' | 'ASPEN'

export const EQUIPMENT_TYPES: EquipmentType[] = ['HTX', 'HEX', 'COMP', 'REACT']
export const UTILITY_TYPES: UtilityType[] = ['COOLING', 'HOT', 'ELECTRICITY', 'MPSG']

export const COST_SOURCE_LABEL: Record<CostSource, string> = {
  FORMULA: '직접 작성한 식으로 계산',
  ASPEN: 'Aspen Plus 계산값 사용',
}

export const EQUIPMENT_TYPE_LABEL: Record<EquipmentType, string> = {
  HTX: '가열기 (HTX)',
  HEX: '열교환기 (HEX)',
  COMP: '압축기 (COMP)',
  REACT: '반응기 (REACT)',
}

export const UTILITY_TYPE_LABEL: Record<UtilityType, string> = {
  COOLING: '냉각수 (Cooling)',
  HOT: '가열 (Hot)',
  ELECTRICITY: '전력 (Electricity)',
  MPSG: '스팀 (MPSG)',
}

export interface AuthResponse {
  token: string
  userId: number
  email: string
  displayName: string
  pictureUrl: string | null
}

export interface TeaCase {
  id: number
  name: string
  description: string | null
  plantOperationHours: number
  depreciationLifetime: number
  createdAt: string
}

export type RunStatus = 'DRAFT' | 'PARSING' | 'PARSED' | 'RUNNING' | 'SUCCESS' | 'FAILED'

export interface CalculationRun {
  id: number
  caseId: number
  name: string | null
  status: RunStatus
  inputXlsxName: string | null
  inputRepName: string | null
  equipmentSnapshot: string | null
  streamSnapshot: string | null
  errorMessage: string | null
  logs: string | null
  createdAt: string
  updatedAt: string
}

export interface EquipmentInstance {
  name: string
  type: string
}

export interface FormulaTemplate {
  id: number
  equipmentType: EquipmentType
  name: string
  k1: number
  k2: number
  k3: number
  systemDefault: boolean
}

export interface EquipmentSetting {
  equipmentType: EquipmentType
  instanceName: string
  typeDefault: boolean
  skipCost: boolean
  costSource: CostSource
  defaultFormula: FormulaTemplate | null
  selectedFormulas: FormulaTemplate[]
  utilityTypes: UtilityType[]
}

export interface EquipmentSettingItem {
  equipmentType: EquipmentType
  instanceName: string | null
  skipCost: boolean
  costSource: CostSource
  defaultFormulaTemplateId: number | null
  selectedFormulaTemplateIds: number[]
  utilityTypes: UtilityType[]
}

export interface UtilityPrice {
  utilityType: UtilityType
  value: number
  unit: string
}

/** 장치 이름 -> (수식 이름 -> 실제 계산된 EQUIPMENT COST[USD]) */
export type EquipmentCosts = Record<string, Record<string, number>>

export function parseEquipmentSnapshot(run: CalculationRun): EquipmentInstance[] {
  if (!run.equipmentSnapshot) return []
  try {
    return JSON.parse(run.equipmentSnapshot) as EquipmentInstance[]
  } catch {
    return []
  }
}

export type StreamDirection = 'IN' | 'OUT'

export const STREAM_DIRECTION_LABEL: Record<StreamDirection, string> = {
  IN: '원료 (입력)',
  OUT: '제품 (출력)',
}

export interface StreamSetting {
  streamName: string
  direction: StreamDirection
  cost: number | null
}

export function parseStreamSnapshot(run: CalculationRun): string[] {
  if (!run.streamSnapshot) return []
  try {
    return JSON.parse(run.streamSnapshot) as string[]
  } catch {
    return []
  }
}
