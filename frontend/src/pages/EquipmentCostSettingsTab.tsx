import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { CostSource, EquipmentSettingItem, EquipmentType, FormulaTemplate } from '../api/types'
import { COST_SOURCE_LABEL, EQUIPMENT_TYPES, EQUIPMENT_TYPE_LABEL } from '../api/types'
import {
  TYPE_DEFAULT,
  rowKey,
  useEquipmentSettingsRows,
} from './equipmentSettings/useEquipmentSettingsRows'

const COST_FORMATTER = new Intl.NumberFormat('en-US', { maximumFractionDigits: 0 })

export function EquipmentCostSettingsTab({ caseId }: { caseId: number }) {
  const {
    isLoading,
    error,
    formulas,
    rows,
    updateRow,
    addOverride,
    instancesByType,
    latestSuccessRunId,
    save,
    saveMutation,
  } = useEquipmentSettingsRows(caseId)

  if (isLoading) return <p>불러오는 중...</p>
  if (error) return <p className="error-text">설정을 불러오지 못했습니다.</p>

  const formulasByType = groupFormulasByType(formulas)
  const costTypes = EQUIPMENT_TYPES // HTX/HEX/COMP/REACT 전부 skipCost 대상

  return (
    <div>
      <p className="muted">
        장치비를 계산할 때 쓸 수식을 타입 기본값 + 개별 장치 오버라이드로 설정합니다. REACT(반응기)는
        후보 수식이 아니라 반응기별 직접 입력값을 쓰므로 수식 선택 UI가 없고, 계산 제외 여부만
        설정합니다.
      </p>

      {costTypes.map((type) => {
        const typeRow = rows[rowKey(type, TYPE_DEFAULT)]
        if (!typeRow) return null
        const instances = instancesByType[type]
        return (
          <div className="card" key={type}>
            <h3>{EQUIPMENT_TYPE_LABEL[type]} — 타입 기본값</h3>
            <SkipCostToggle row={typeRow} onChange={updateRow} />
            {!typeRow.skipCost && <CostSourceToggle row={typeRow} onChange={updateRow} />}
            {!typeRow.skipCost && type !== 'REACT' && (
              <FormulaSection
                row={typeRow}
                formulas={formulasByType[type]}
                equipmentType={type}
                onChange={updateRow}
                allowCreate
              />
            )}

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
                        <>
                          <SkipCostToggle row={overrideRow} onChange={updateRow} />
                          {!overrideRow.skipCost && <CostSourceToggle row={overrideRow} onChange={updateRow} />}
                          {!overrideRow.skipCost && type !== 'REACT' && (
                            <FormulaSection
                              row={overrideRow}
                              formulas={formulasByType[type]}
                              equipmentType={type}
                              onChange={updateRow}
                              costsForInstance={latestSuccessRunId ? name : undefined}
                              runId={latestSuccessRunId}
                            />
                          )}
                        </>
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

function SkipCostToggle({
  row,
  onChange,
}: {
  row: EquipmentSettingItem
  onChange: (row: EquipmentSettingItem) => void
}) {
  return (
    <label className="checkbox-line">
      <input
        type="checkbox"
        checked={row.skipCost}
        onChange={(e) => onChange({ ...row, skipCost: e.target.checked })}
      />
      이 장치는 장치비 계산 안 함
    </label>
  )
}

const COST_SOURCES: CostSource[] = ['FORMULA', 'ASPEN']

function CostSourceToggle({
  row,
  onChange,
}: {
  row: EquipmentSettingItem
  onChange: (row: EquipmentSettingItem) => void
}) {
  return (
    <div className="cost-source-toggle">
      <span className="field-label">장치비 계산 방식</span>
      {COST_SOURCES.map((source) => (
        <label className="radio-line" key={source}>
          <input
            type="radio"
            name={`cost-source-${row.equipmentType}-${row.instanceName}`}
            checked={row.costSource === source}
            onChange={() => onChange({ ...row, costSource: source })}
          />
          {COST_SOURCE_LABEL[source]}
        </label>
      ))}
      {row.costSource === 'ASPEN' && (
        <span className="muted small">
          Aspen이 이 장치의 값을 제공하지 않으면 자동으로 식으로 계산한 값을 씁니다.
        </span>
      )}
    </div>
  )
}

interface FormState {
  name: string
  k1: string
  k2: string
  k3: string
}

const EMPTY_FORM: FormState = { name: '', k1: '', k2: '', k3: '' }

function FormulaSection({
  row,
  formulas,
  equipmentType,
  onChange,
  allowCreate = false,
  costsForInstance,
  runId,
}: {
  row: EquipmentSettingItem
  formulas: FormulaTemplate[]
  equipmentType: EquipmentType
  onChange: (row: EquipmentSettingItem) => void
  allowCreate?: boolean
  /** 값이 있으면 이 장치 이름 기준으로 계산된 실제 원가를 보여준다 (최근 SUCCESS run 기준) */
  costsForInstance?: string
  runId?: number | null
}) {
  const queryClient = useQueryClient()
  const costsQuery = useQuery({
    queryKey: ['equipment-costs', runId],
    queryFn: () => api.equipmentCosts(runId as number),
    enabled: !!runId,
  })

  const [creating, setCreating] = useState(false)
  const [createForm, setCreateForm] = useState<FormState>(EMPTY_FORM)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editForm, setEditForm] = useState<FormState>(EMPTY_FORM)
  const [formError, setFormError] = useState<string | null>(null)

  const invalidateFormulas = () => queryClient.invalidateQueries({ queryKey: ['formulas'] })

  const createMutation = useMutation({
    mutationFn: api.createFormula,
    onSuccess: () => {
      setCreating(false)
      setCreateForm(EMPTY_FORM)
      setFormError(null)
      void invalidateFormulas()
    },
    onError: () => setFormError('저장에 실패했습니다.'),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, ...data }: { id: number } & Parameters<typeof api.updateFormula>[1]) =>
      api.updateFormula(id, data),
    onSuccess: () => {
      setEditingId(null)
      setFormError(null)
      void invalidateFormulas()
    },
    onError: () => setFormError('저장에 실패했습니다.'),
  })

  const deleteMutation = useMutation({
    mutationFn: api.deleteFormula,
    onSuccess: () => void invalidateFormulas(),
    onError: () => window.alert('이 수식은 하나 이상의 프로젝트 설정에서 사용 중이라 삭제할 수 없습니다.'),
  })

  const toggleFormula = (formulaId: number, checked: boolean) => {
    const selected = checked
      ? [...row.selectedFormulaTemplateIds, formulaId]
      : row.selectedFormulaTemplateIds.filter((id) => id !== formulaId)
    let defaultId = row.defaultFormulaTemplateId
    if (!checked && defaultId === formulaId) defaultId = selected[0] ?? null
    if (checked && defaultId === null) defaultId = formulaId
    onChange({ ...row, selectedFormulaTemplateIds: selected, defaultFormulaTemplateId: defaultId })
  }

  const startEdit = (formula: FormulaTemplate) => {
    setEditingId(formula.id)
    setEditForm({ name: formula.name, k1: String(formula.k1), k2: String(formula.k2), k3: String(formula.k3) })
  }

  const submitCreate = (e: FormEvent) => {
    e.preventDefault()
    const parsed = parseForm(createForm)
    if (!parsed) {
      setFormError('이름과 K1/K2/K3 값을 올바르게 입력하세요.')
      return
    }
    createMutation.mutate({ equipmentType, ...parsed })
  }

  const submitEdit = (e: FormEvent, formula: FormulaTemplate) => {
    e.preventDefault()
    const parsed = parseForm(editForm)
    if (!parsed) {
      setFormError('이름과 K1/K2/K3 값을 올바르게 입력하세요.')
      return
    }
    updateMutation.mutate({ id: formula.id, equipmentType: formula.equipmentType, ...parsed })
  }

  const instanceCosts = costsForInstance ? costsQuery.data?.[costsForInstance] : undefined

  return (
    <div className="formula-list">
      <span className="field-label">
        사용할 수식 (체크) / 기본 수식 (●){instanceCosts && ' / 최근 실행 기준 실제 계산 금액'}
      </span>
      {formulas.length === 0 && <p className="muted small">등록된 수식이 없습니다.</p>}
      {formulas.map((formula) => {
        const checked = row.selectedFormulaTemplateIds.includes(formula.id)
        const cost = instanceCosts?.[formula.name]
        if (editingId === formula.id) {
          return (
            <form className="formula-edit-form" key={formula.id} onSubmit={(e) => submitEdit(e, formula)}>
              <input
                value={editForm.name}
                onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
              />
              <input value={editForm.k1} onChange={(e) => setEditForm({ ...editForm, k1: e.target.value })} placeholder="K1" />
              <input value={editForm.k2} onChange={(e) => setEditForm({ ...editForm, k2: e.target.value })} placeholder="K2" />
              <input value={editForm.k3} onChange={(e) => setEditForm({ ...editForm, k3: e.target.value })} placeholder="K3" />
              <button type="submit" className="btn btn-sm btn-primary" disabled={updateMutation.isPending}>
                저장
              </button>
              <button type="button" className="btn btn-sm" onClick={() => setEditingId(null)}>
                취소
              </button>
            </form>
          )
        }
        return (
          <div className="formula-row" key={formula.id}>
            <label className="checkbox-line">
              <input type="checkbox" checked={checked} onChange={(e) => toggleFormula(formula.id, e.target.checked)} />
              {formula.name}
              <span className="muted small">
                {' '}
                (K1={formula.k1}, K2={formula.k2}, K3={formula.k3})
              </span>
              {cost !== undefined && (
                <span className="cost-badge">${COST_FORMATTER.format(cost)}</span>
              )}
            </label>
            <div className="formula-row-actions">
              <label className="radio-line">
                <input
                  type="radio"
                  name={`default-${equipmentType}-${row.instanceName}`}
                  checked={row.defaultFormulaTemplateId === formula.id}
                  disabled={!checked}
                  onChange={() => onChange({ ...row, defaultFormulaTemplateId: formula.id })}
                />
                기본
              </label>
              <button type="button" className="btn btn-sm" onClick={() => startEdit(formula)}>
                수정
              </button>
              <button
                type="button"
                className="btn btn-sm btn-danger"
                onClick={() => deleteMutation.mutate(formula.id)}
              >
                삭제
              </button>
            </div>
          </div>
        )
      })}

      {allowCreate && (
        <div className="formula-create">
          {!creating ? (
            <button type="button" className="btn btn-sm" onClick={() => setCreating(true)}>
              + 새 수식 추가
            </button>
          ) : (
            <form className="formula-edit-form" onSubmit={submitCreate}>
              <input
                value={createForm.name}
                onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })}
                placeholder="수식 이름"
              />
              <input value={createForm.k1} onChange={(e) => setCreateForm({ ...createForm, k1: e.target.value })} placeholder="K1" />
              <input value={createForm.k2} onChange={(e) => setCreateForm({ ...createForm, k2: e.target.value })} placeholder="K2" />
              <input value={createForm.k3} onChange={(e) => setCreateForm({ ...createForm, k3: e.target.value })} placeholder="K3" />
              <button type="submit" className="btn btn-sm btn-primary" disabled={createMutation.isPending}>
                추가
              </button>
              <button
                type="button"
                className="btn btn-sm"
                onClick={() => {
                  setCreating(false)
                  setCreateForm(EMPTY_FORM)
                }}
              >
                취소
              </button>
            </form>
          )}
        </div>
      )}
      {formError && <p className="error-text small">{formError}</p>}
    </div>
  )
}

function parseForm(form: FormState): { name: string; k1: number; k2: number; k3: number } | null {
  const k1 = Number(form.k1)
  const k2 = Number(form.k2)
  const k3 = Number(form.k3)
  if (!form.name.trim() || Number.isNaN(k1) || Number.isNaN(k2) || Number.isNaN(k3)) return null
  return { name: form.name.trim(), k1, k2, k3 }
}

function groupFormulasByType(formulas: FormulaTemplate[]): Record<EquipmentType, FormulaTemplate[]> {
  const grouped: Record<EquipmentType, FormulaTemplate[]> = { HTX: [], HEX: [], COMP: [], REACT: [] }
  for (const formula of formulas) grouped[formula.equipmentType].push(formula)
  return grouped
}
