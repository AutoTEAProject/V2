import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type {
  EquipmentSetting,
  EquipmentSettingItem,
  EquipmentType,
  FormulaTemplate,
  UtilityType,
} from '../api/types'
import {
  EQUIPMENT_TYPES,
  EQUIPMENT_TYPE_LABEL,
  UTILITY_TYPES,
  UTILITY_TYPE_LABEL,
  parseEquipmentSnapshot,
} from '../api/types'

const TYPE_DEFAULT = '*'

function rowKey(equipmentType: EquipmentType, instanceName: string): string {
  return `${equipmentType}|${instanceName}`
}

function toItem(setting: EquipmentSetting): EquipmentSettingItem {
  return {
    equipmentType: setting.equipmentType,
    instanceName: setting.instanceName,
    skipCost: setting.skipCost,
    defaultFormulaTemplateId: setting.defaultFormula?.id ?? null,
    selectedFormulaTemplateIds: setting.selectedFormulas.map((f) => f.id),
    utilityTypes: setting.utilityTypes,
  }
}

export function EquipmentSettingsTab({ caseId }: { caseId: number }) {
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

  if (settingsQuery.isLoading || formulasQuery.isLoading) return <p>불러오는 중...</p>
  if (settingsQuery.error || formulasQuery.error) {
    return <p className="error-text">설정을 불러오지 못했습니다.</p>
  }

  const formulasByType = groupFormulasByType(formulasQuery.data ?? [])

  const latestRunWithSnapshot = (runsQuery.data ?? []).find((r) => r.equipmentSnapshot)
  const instancesByType: Record<EquipmentType, string[]> = { HTX: [], HEX: [], COMP: [], REACT: [] }
  if (latestRunWithSnapshot) {
    for (const instance of parseEquipmentSnapshot(latestRunWithSnapshot)) {
      if (isEquipmentType(instance.type)) {
        instancesByType[instance.type].push(instance.name)
      }
    }
  }

  const updateRow = (next: EquipmentSettingItem) => {
    setRows((prev) => ({ ...prev, [rowKey(next.equipmentType, next.instanceName ?? TYPE_DEFAULT)]: next }))
  }

  const addOverride = (equipmentType: EquipmentType, instanceName: string) => {
    const base = rows[rowKey(equipmentType, TYPE_DEFAULT)]
    if (!base) return
    updateRow({ ...base, instanceName })
  }

  const handleSave = () => {
    saveMutation.mutate(Object.values(rows))
  }

  return (
    <div>
      <p className="muted">
        타입 기본값은 해당 타입의 모든 장치에 적용됩니다. 특정 장치만 다르게 하려면 아래에서 개별
        설정을 켜세요. (개별 장치 목록은 가장 최근 파싱된 실행 기준입니다)
      </p>

      {EQUIPMENT_TYPES.map((type) => {
        const typeRow = rows[rowKey(type, TYPE_DEFAULT)]
        if (!typeRow) return null
        const instances = instancesByType[type]
        return (
          <div className="card" key={type}>
            <h3>{EQUIPMENT_TYPE_LABEL[type]} — 타입 기본값</h3>
            <SettingEditor
              row={typeRow}
              formulas={formulasByType[type]}
              onChange={updateRow}
              showFormulas={type !== 'REACT'}
              showUtility={type !== 'REACT'}
            />

            {instances.length > 0 && (
              <details className="instance-list">
                <summary>개별 장치 ({instances.length}개)</summary>
                {instances.map((name) => {
                  const key = rowKey(type, name)
                  const overrideRow = rows[key]
                  return (
                    <div className="instance-row" key={name}>
                      <div className="instance-row-header">
                        <strong>{name}</strong>
                        {!overrideRow && (
                          <button
                            type="button"
                            className="btn btn-sm"
                            onClick={() => addOverride(type, name)}
                          >
                            개별 설정
                          </button>
                        )}
                        {overrideRow && <span className="muted small">타입 기본값과 다르게 설정됨</span>}
                      </div>
                      {overrideRow ? (
                        <SettingEditor
                          row={overrideRow}
                          formulas={formulasByType[type]}
                          onChange={updateRow}
                          showFormulas={type !== 'REACT'}
                          showUtility={type !== 'REACT'}
                        />
                      ) : (
                        <span className="muted small">타입 기본값 사용 중</span>
                      )}
                    </div>
                  )
                })}
              </details>
            )}
          </div>
        )
      })}

      <button
        type="button"
        className="btn btn-primary"
        disabled={saveMutation.isPending}
        onClick={handleSave}
      >
        {saveMutation.isPending ? '저장 중...' : '설정 저장'}
      </button>
      {saveMutation.isError && <p className="error-text">저장에 실패했습니다.</p>}
      {saveMutation.isSuccess && <p className="success-text">저장했습니다.</p>}
    </div>
  )
}

function SettingEditor({
  row,
  formulas,
  onChange,
  showFormulas,
  showUtility,
}: {
  row: EquipmentSettingItem
  formulas: FormulaTemplate[]
  onChange: (row: EquipmentSettingItem) => void
  showFormulas: boolean
  showUtility: boolean
}) {
  const toggleFormula = (formulaId: number, checked: boolean) => {
    const selected = checked
      ? [...row.selectedFormulaTemplateIds, formulaId]
      : row.selectedFormulaTemplateIds.filter((id) => id !== formulaId)
    let defaultId = row.defaultFormulaTemplateId
    if (!checked && defaultId === formulaId) {
      defaultId = selected[0] ?? null
    }
    if (checked && defaultId === null) {
      defaultId = formulaId
    }
    onChange({ ...row, selectedFormulaTemplateIds: selected, defaultFormulaTemplateId: defaultId })
  }

  const toggleUtility = (utility: UtilityType, checked: boolean) => {
    const utilityTypes = checked
      ? [...row.utilityTypes, utility]
      : row.utilityTypes.filter((u) => u !== utility)
    onChange({ ...row, utilityTypes })
  }

  return (
    <div className="setting-editor">
      <label className="checkbox-line">
        <input
          type="checkbox"
          checked={row.skipCost}
          onChange={(e) => onChange({ ...row, skipCost: e.target.checked })}
        />
        이 장치는 장치비 계산 안 함
      </label>

      {showFormulas && !row.skipCost && (
        <div className="formula-list">
          <span className="field-label">사용할 수식 (체크) / 기본 수식 (●)</span>
          {formulas.length === 0 && <p className="muted small">등록된 수식이 없습니다.</p>}
          {formulas.map((formula) => {
            const checked = row.selectedFormulaTemplateIds.includes(formula.id)
            return (
              <div className="formula-row" key={formula.id}>
                <label className="checkbox-line">
                  <input
                    type="checkbox"
                    checked={checked}
                    onChange={(e) => toggleFormula(formula.id, e.target.checked)}
                  />
                  {formula.name}
                  <span className="muted small">
                    {' '}
                    (K1={formula.k1}, K2={formula.k2}, K3={formula.k3})
                  </span>
                </label>
                <label className="radio-line">
                  <input
                    type="radio"
                    name={`default-${row.equipmentType}-${row.instanceName}`}
                    checked={row.defaultFormulaTemplateId === formula.id}
                    disabled={!checked}
                    onChange={() => onChange({ ...row, defaultFormulaTemplateId: formula.id })}
                  />
                  기본
                </label>
              </div>
            )
          })}
        </div>
      )}

      {showUtility && !row.skipCost && (
        <div className="utility-list">
          <span className="field-label">계산할 Utility (복수 선택, 0개 가능)</span>
          {UTILITY_TYPES.map((utility) => (
            <label className="checkbox-line" key={utility}>
              <input
                type="checkbox"
                checked={row.utilityTypes.includes(utility)}
                onChange={(e) => toggleUtility(utility, e.target.checked)}
              />
              {UTILITY_TYPE_LABEL[utility]}
            </label>
          ))}
        </div>
      )}
    </div>
  )
}

function groupFormulasByType(formulas: FormulaTemplate[]): Record<EquipmentType, FormulaTemplate[]> {
  const grouped: Record<EquipmentType, FormulaTemplate[]> = { HTX: [], HEX: [], COMP: [], REACT: [] }
  for (const formula of formulas) {
    grouped[formula.equipmentType].push(formula)
  }
  return grouped
}

function isEquipmentType(value: string): value is EquipmentType {
  return (EQUIPMENT_TYPES as string[]).includes(value)
}
