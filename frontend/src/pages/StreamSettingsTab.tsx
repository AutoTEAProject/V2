import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { StreamSetting } from '../api/types'
import { parseStreamSnapshot } from '../api/types'

function selectedValues(e: React.ChangeEvent<HTMLSelectElement>): string[] {
  return Array.from(e.target.selectedOptions).map((o) => o.value)
}

export function StreamSettingsTab({ caseId }: { caseId: number }) {
  const queryClient = useQueryClient()

  const settingsQuery = useQuery({
    queryKey: ['stream-settings', caseId],
    queryFn: () => api.listStreamSettings(caseId),
  })
  const runsQuery = useQuery({
    queryKey: ['runs', caseId],
    queryFn: () => api.listRuns(caseId),
  })

  const [inputNames, setInputNames] = useState<string[]>([])
  const [outputNames, setOutputNames] = useState<string[]>([])
  const [costs, setCosts] = useState<Record<string, string>>({})

  useEffect(() => {
    if (!settingsQuery.data) return
    setInputNames(settingsQuery.data.filter((s) => s.direction === 'IN').map((s) => s.streamName))
    setOutputNames(settingsQuery.data.filter((s) => s.direction === 'OUT').map((s) => s.streamName))
    setCosts((prev) => {
      const next = { ...prev }
      for (const s of settingsQuery.data) {
        if (s.direction === 'IN') next[s.streamName] = s.cost != null ? String(s.cost) : ''
      }
      return next
    })
  }, [settingsQuery.data])

  const saveMutation = useMutation({
    mutationFn: (items: StreamSetting[]) => api.saveStreamSettings(caseId, items),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['stream-settings', caseId] })
    },
  })

  if (settingsQuery.isLoading || runsQuery.isLoading) return <p>불러오는 중...</p>
  if (settingsQuery.error || runsQuery.error) return <p className="error-text">설정을 불러오지 못했습니다.</p>

  const latestRunWithStreams = (runsQuery.data ?? []).find((r) => r.streamSnapshot)
  const discovered = latestRunWithStreams ? parseStreamSnapshot(latestRunWithStreams) : []
  // 예전 설정에 있었는데 최신 스냅샷엔 없는 stream도 놓치지 않도록 합쳐서 보여준다.
  const allNames = Array.from(new Set([...discovered, ...inputNames, ...outputNames])).sort()

  // 같은 stream이 입력/출력 양쪽에 동시에 선택되지 않도록, 한쪽에서 고르면 다른 쪽 목록에서는 뺀다.
  const inputOptions = allNames.filter((n) => !outputNames.includes(n))
  const outputOptions = allNames.filter((n) => !inputNames.includes(n))

  const handleSave = () => {
    const items: StreamSetting[] = [
      ...inputNames.map((streamName) => ({
        streamName,
        direction: 'IN' as const,
        cost: Number(costs[streamName]) || 0,
      })),
      ...outputNames.map((streamName) => ({
        streamName,
        direction: 'OUT' as const,
        cost: null,
      })),
    ]
    saveMutation.mutate(items)
  }

  return (
    <div>
      <PlantParametersCard caseId={caseId} />

      <p className="muted">
        업로드한 input.rep에서 발견된 stream 중, 원료비 계산에 쓸 원료(입력)와 생산량 계산에 쓸
        제품(출력)을 각각 여러 개 선택합니다(Ctrl/Cmd+클릭으로 다중 선택). 선택하지 않은 stream은
        계산에서 빠집니다.
      </p>

      {allNames.length === 0 ? (
        <p className="muted">아직 파싱된 실행이 없습니다. "실행" 탭에서 먼저 파일을 업로드하세요.</p>
      ) : (
        <div className="card stream-columns">
          <div className="stream-column">
            <h3>입력 stream (원료)</h3>
            <select
              multiple
              size={12}
              value={inputNames}
              onChange={(e) => setInputNames(selectedValues(e))}
              className="stream-multiselect"
            >
              {inputOptions.map((name) => (
                <option key={name} value={name}>
                  {name}
                </option>
              ))}
            </select>

            {inputNames.length > 0 && (
              <div className="stream-cost-list">
                <span className="field-label">원료 단가 [USD/kg]</span>
                {inputNames.map((name) => (
                  <div className="form-row stream-cost-row" key={name}>
                    <label htmlFor={`cost-${name}`}>{name}</label>
                    <input
                      id={`cost-${name}`}
                      value={costs[name] ?? ''}
                      onChange={(e) => setCosts((prev) => ({ ...prev, [name]: e.target.value }))}
                      placeholder="0.00"
                    />
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="stream-column">
            <h3>출력 stream (제품)</h3>
            <select
              multiple
              size={12}
              value={outputNames}
              onChange={(e) => setOutputNames(selectedValues(e))}
              className="stream-multiselect"
            >
              {outputOptions.map((name) => (
                <option key={name} value={name}>
                  {name}
                </option>
              ))}
            </select>
          </div>
        </div>
      )}

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

function PlantParametersCard({ caseId }: { caseId: number }) {
  const queryClient = useQueryClient()
  const caseQuery = useQuery({ queryKey: ['case', caseId], queryFn: () => api.getCase(caseId) })

  const [hours, setHours] = useState('')
  const [lifetime, setLifetime] = useState('')

  useEffect(() => {
    if (!caseQuery.data) return
    setHours(String(caseQuery.data.plantOperationHours))
    setLifetime(String(caseQuery.data.depreciationLifetime))
  }, [caseQuery.data])

  const saveMutation = useMutation({
    mutationFn: () => api.updatePlantParameters(caseId, Number(hours), Number(lifetime)),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['case', caseId] })
    },
  })

  if (caseQuery.isLoading) return null
  if (caseQuery.error || !caseQuery.data) return <p className="error-text">플랜트 파라미터를 불러오지 못했습니다.</p>

  const hoursValid = Number(hours) > 0
  const lifetimeValid = Number(lifetime) > 0

  return (
    <div className="card">
      <h2>플랜트 파라미터</h2>
      <p className="muted">
        연간 가동 시간은 utility 연간 사용량/비용과 생산량 계산에, 감가상각 내용연수는 Profitability
        Analysis의 Depreciation 계산에 쓰입니다.
      </p>
      <div className="plant-parameter-fields">
        <div className="form-row">
          <label htmlFor="plant-operation-hours">연간 가동 시간 [hours/year]</label>
          <input
            id="plant-operation-hours"
            value={hours}
            onChange={(e) => setHours(e.target.value)}
          />
        </div>
        <div className="form-row">
          <label htmlFor="depreciation-lifetime">감가상각 내용연수 [years]</label>
          <input
            id="depreciation-lifetime"
            value={lifetime}
            onChange={(e) => setLifetime(e.target.value)}
          />
        </div>
      </div>
      <button
        type="button"
        className="btn btn-primary btn-sm"
        disabled={!hoursValid || !lifetimeValid || saveMutation.isPending}
        onClick={() => saveMutation.mutate()}
      >
        {saveMutation.isPending ? '저장 중...' : '저장'}
      </button>
      {saveMutation.isError && <p className="error-text">저장에 실패했습니다.</p>}
      {saveMutation.isSuccess && <p className="success-text">저장했습니다.</p>}
    </div>
  )
}
