import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { EquipmentSetting, EquipmentSettingItem, EquipmentType } from '../../api/types'
import { EQUIPMENT_TYPES, parseEquipmentSnapshot } from '../../api/types'

export const TYPE_DEFAULT = '*'

export function rowKey(equipmentType: EquipmentType, instanceName: string): string {
  return `${equipmentType}|${instanceName}`
}

function toItem(setting: EquipmentSetting): EquipmentSettingItem {
  return {
    equipmentType: setting.equipmentType,
    instanceName: setting.instanceName,
    skipCost: setting.skipCost,
    costSource: setting.costSource,
    defaultFormulaTemplateId: setting.defaultFormula?.id ?? null,
    selectedFormulaTemplateIds: setting.selectedFormulas.map((f) => f.id),
    utilityTypes: setting.utilityTypes,
  }
}

export function isEquipmentType(value: string): value is EquipmentType {
  return (EQUIPMENT_TYPES as string[]).includes(value)
}

/**
 * 장치비/Utility 설정 화면 두 탭이 공유하는 데이터/편집 상태.
 * 두 탭 다 같은 EquipmentSettingItem 행을 저장하므로, 화면에 안 보이는 필드도 항상
 * 원래 값 그대로 들고 있다가 저장해야 다른 탭에서 편집한 값을 덮어쓰지 않는다.
 */
export function useEquipmentSettingsRows(caseId: number) {
  const queryClient = useQueryClient()

  const settingsQuery = useQuery({
    queryKey: ['equipment-settings', caseId],
    queryFn: () => api.listEquipmentSettings(caseId),
  })
  const formulasQuery = useQuery({
    queryKey: ['formulas'],
    queryFn: () => api.listFormulas(),
  })
  const runsQuery = useQuery({
    queryKey: ['runs', caseId],
    queryFn: () => api.listRuns(caseId),
  })

  const [rows, setRows] = useState<Record<string, EquipmentSettingItem>>({})

  useEffect(() => {
    if (!settingsQuery.data) return
    const next: Record<string, EquipmentSettingItem> = {}
    for (const setting of settingsQuery.data) {
      next[rowKey(setting.equipmentType, setting.instanceName)] = toItem(setting)
    }
    setRows(next)
  }, [settingsQuery.data])

  const saveMutation = useMutation({
    mutationFn: (items: EquipmentSettingItem[]) => api.saveEquipmentSettings(caseId, items),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['equipment-settings', caseId] })
    },
  })

  const instancesByType: Record<EquipmentType, string[]> = { HTX: [], HEX: [], COMP: [], REACT: [] }
  const latestRunWithSnapshot = (runsQuery.data ?? []).find((r) => r.equipmentSnapshot)
  if (latestRunWithSnapshot) {
    for (const instance of parseEquipmentSnapshot(latestRunWithSnapshot)) {
      if (isEquipmentType(instance.type)) {
        instancesByType[instance.type].push(instance.name)
      }
    }
  }
  const latestSuccessRun = (runsQuery.data ?? []).find((r) => r.status === 'SUCCESS')

  const updateRow = (next: EquipmentSettingItem) => {
    setRows((prev) => ({ ...prev, [rowKey(next.equipmentType, next.instanceName ?? TYPE_DEFAULT)]: next }))
  }

  const addOverride = (equipmentType: EquipmentType, instanceName: string) => {
    const base = rows[rowKey(equipmentType, TYPE_DEFAULT)]
    if (!base) return
    updateRow({ ...base, instanceName })
  }

  return {
    isLoading: settingsQuery.isLoading || formulasQuery.isLoading,
    error: settingsQuery.error ?? formulasQuery.error,
    formulas: formulasQuery.data ?? [],
    rows,
    updateRow,
    addOverride,
    instancesByType,
    latestSuccessRunId: latestSuccessRun?.id ?? null,
    save: () => saveMutation.mutate(Object.values(rows)),
    saveMutation,
  }
}
