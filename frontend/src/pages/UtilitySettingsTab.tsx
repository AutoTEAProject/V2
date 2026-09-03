import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { EquipmentSettingItem, UtilityPrice, UtilityType } from '../api/types'
import { EQUIPMENT_TYPE_LABEL, UTILITY_TYPES, UTILITY_TYPE_LABEL } from '../api/types'
import { TYPE_DEFAULT, rowKey, useEquipmentSettingsRows } from './equipmentSettings/useEquipmentSettingsRows'

const COST_TYPES: Array<'HTX' | 'HEX' | 'COMP'> = ['HTX', 'HEX', 'COMP']

export function UtilitySettingsTab({ caseId }: { caseId: number }) {
  const { isLoading, error, rows, updateRow, addOverride, instancesByType, save, saveMutation } =
    useEquipmentSettingsRows(caseId)
  const pricesQuery = useQuery({ queryKey: ['utility-prices'], queryFn: () => api.utilityPrices() })

  if (isLoading || pricesQuery.isLoading) return <p>불러오는 중...</p>
  if (error || pricesQuery.error) return <p className="error-text">설정을 불러오지 못했습니다.</p>

  const prices = pricesQuery.data ?? {}

  return (
    <div>
      <p className="muted">
        장치가 어떤 utility를 소비하는 것으로 계산할지 선택합니다. 여러 개 선택 가능하고, 아예 계산하지
        않으려면 전부 해제하면 됩니다. REACT(반응기)는 utility 계산 대상이 아닙니다.
      </p>

      {COST_TYPES.map((type) => {
        const typeRow = rows[rowKey(type, TYPE_DEFAULT)]
        if (!typeRow) return null
        const instances = instancesByType[type]
        return (
          <div className="card" key={type}>
            <h3>{EQUIPMENT_TYPE_LABEL[type]} — 타입 기본값</h3>
            <UtilityEditor row={typeRow} prices={prices} onChange={updateRow} />

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
                        <UtilityEditor row={overrideRow} prices={prices} onChange={updateRow} />
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
        onClick={save}
      >
        {saveMutation.isPending ? '저장 중...' : '설정 저장'}
      </button>
      {saveMutation.isError && <p className="error-text">저장에 실패했습니다.</p>}
      {saveMutation.isSuccess && <p className="success-text">저장했습니다.</p>}
    </div>
  )
}

function UtilityEditor({
  row,
  prices,
  onChange,
}: {
  row: EquipmentSettingItem
  prices: Record<string, UtilityPrice>
  onChange: (row: EquipmentSettingItem) => void
}) {
  const toggle = (utility: UtilityType, checked: boolean) => {
    const utilityTypes = checked
      ? [...row.utilityTypes, utility]
      : row.utilityTypes.filter((u) => u !== utility)
    onChange({ ...row, utilityTypes })
  }

  return (
    <div className="setting-editor">
      <div className="utility-list">
        {UTILITY_TYPES.map((utility) => {
          const price = prices[utility]
          return (
            <label className="checkbox-line" key={utility}>
              <input
                type="checkbox"
                checked={row.utilityTypes.includes(utility)}
                onChange={(e) => toggle(utility, e.target.checked)}
              />
              {UTILITY_TYPE_LABEL[utility]}
              {price && (
                <span className="muted small">
                  {' '}
                  — {price.value} {price.unit}
                </span>
              )}
            </label>
          )
        })}
      </div>
    </div>
  )
}
