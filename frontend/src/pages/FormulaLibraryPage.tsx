import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { EquipmentType, FormulaTemplate } from '../api/types'
import { EQUIPMENT_TYPES, EQUIPMENT_TYPE_LABEL } from '../api/types'

interface FormState {
  equipmentType: EquipmentType
  name: string
  k1: string
  k2: string
  k3: string
}

const EMPTY_FORM: FormState = { equipmentType: 'HTX', name: '', k1: '', k2: '', k3: '' }

export function FormulaLibraryPage() {
  const queryClient = useQueryClient()
  const { data: formulas, isLoading, error } = useQuery({
    queryKey: ['formulas'],
    queryFn: () => api.listFormulas(),
  })

  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [formError, setFormError] = useState<string | null>(null)

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['formulas'] })

  const createMutation = useMutation({
    mutationFn: api.createFormula,
    onSuccess: () => {
      setForm(EMPTY_FORM)
      setFormError(null)
      void invalidate()
    },
    onError: () => setFormError('저장에 실패했습니다.'),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, ...data }: { id: number } & Parameters<typeof api.updateFormula>[1]) =>
      api.updateFormula(id, data),
    onSuccess: () => {
      setForm(EMPTY_FORM)
      setEditingId(null)
      setFormError(null)
      void invalidate()
    },
    onError: () => setFormError('저장에 실패했습니다.'),
  })

  const deleteMutation = useMutation({
    mutationFn: api.deleteFormula,
    onSuccess: () => void invalidate(),
    onError: () => window.alert('이 수식은 하나 이상의 프로젝트 설정에서 사용 중이라 삭제할 수 없습니다.'),
  })

  const startEdit = (formula: FormulaTemplate) => {
    setEditingId(formula.id)
    setForm({
      equipmentType: formula.equipmentType,
      name: formula.name,
      k1: String(formula.k1),
      k2: String(formula.k2),
      k3: String(formula.k3),
    })
  }

  const cancelEdit = () => {
    setEditingId(null)
    setForm(EMPTY_FORM)
    setFormError(null)
  }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    const k1 = Number(form.k1)
    const k2 = Number(form.k2)
    const k3 = Number(form.k3)
    if (!form.name.trim() || Number.isNaN(k1) || Number.isNaN(k2) || Number.isNaN(k3)) {
      setFormError('이름과 K1/K2/K3 값을 올바르게 입력하세요.')
      return
    }
    const data = { equipmentType: form.equipmentType, name: form.name.trim(), k1, k2, k3 }
    if (editingId !== null) {
      updateMutation.mutate({ id: editingId, ...data })
    } else {
      createMutation.mutate(data)
    }
  }

  const grouped = new Map<EquipmentType, FormulaTemplate[]>()
  for (const type of EQUIPMENT_TYPES) grouped.set(type, [])
  for (const formula of formulas ?? []) grouped.get(formula.equipmentType)?.push(formula)

  return (
    <div className="page">
      <h1>수식 라이브러리</h1>
      <p className="muted">
        장치비 계산에 쓰는 수식(EQUIPMENT COST = 10^(K1+K2+K3) × (Capacity/10)^0.6 × CEPCI비, COMP는
        로그식)의 계수를 자유롭게 추가/수정/삭제합니다. 여기서 관리하는 수식은 모든 프로젝트가
        공유하며, 프로젝트별로는 이 중 어떤 것을 쓸지만 선택합니다.
      </p>

      <form className="card create-form" onSubmit={handleSubmit}>
        <h2>{editingId !== null ? '수식 수정' : '새 수식 추가'}</h2>
        <div className="form-row">
          <label htmlFor="formula-type">장치 타입</label>
          <select
            id="formula-type"
            value={form.equipmentType}
            onChange={(e) => setForm({ ...form, equipmentType: e.target.value as EquipmentType })}
          >
            {EQUIPMENT_TYPES.filter((t) => t !== 'REACT').map((t) => (
              <option key={t} value={t}>
                {EQUIPMENT_TYPE_LABEL[t]}
              </option>
            ))}
          </select>
        </div>
        <div className="form-row">
          <label htmlFor="formula-name">이름</label>
          <input
            id="formula-name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            placeholder="예: U-tube"
            required
          />
        </div>
        <div className="form-row k-values">
          <label>
            K1
            <input value={form.k1} onChange={(e) => setForm({ ...form, k1: e.target.value })} />
          </label>
          <label>
            K2
            <input value={form.k2} onChange={(e) => setForm({ ...form, k2: e.target.value })} />
          </label>
          <label>
            K3
            <input value={form.k3} onChange={(e) => setForm({ ...form, k3: e.target.value })} />
          </label>
        </div>
        <div className="form-actions">
          <button
            type="submit"
            className="btn btn-primary"
            disabled={createMutation.isPending || updateMutation.isPending}
          >
            {editingId !== null ? '수정 저장' : '추가'}
          </button>
          {editingId !== null && (
            <button type="button" className="btn btn-ghost" onClick={cancelEdit}>
              취소
            </button>
          )}
        </div>
        {formError && <p className="error-text">{formError}</p>}
      </form>

      {isLoading && <p>불러오는 중...</p>}
      {error && <p className="error-text">수식 목록을 불러오지 못했습니다.</p>}

      {EQUIPMENT_TYPES.filter((t) => t !== 'REACT').map((type) => (
        <div className="card" key={type}>
          <h3>{EQUIPMENT_TYPE_LABEL[type]}</h3>
          <table className="formula-table">
            <thead>
              <tr>
                <th>이름</th>
                <th>K1</th>
                <th>K2</th>
                <th>K3</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {grouped.get(type)?.map((formula) => (
                <tr key={formula.id}>
                  <td>
                    {formula.name}
                    {formula.systemDefault && <span className="muted small"> (기본 제공)</span>}
                  </td>
                  <td>{formula.k1}</td>
                  <td>{formula.k2}</td>
                  <td>{formula.k3}</td>
                  <td className="run-actions">
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
                  </td>
                </tr>
              ))}
              {grouped.get(type)?.length === 0 && (
                <tr>
                  <td colSpan={5} className="muted">
                    등록된 수식이 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      ))}
    </div>
  )
}
