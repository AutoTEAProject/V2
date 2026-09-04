import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { StreamDirection, StreamSetting } from '../api/types'
import { STREAM_DIRECTION_LABEL, parseStreamSnapshot } from '../api/types'

interface Row {
  included: boolean
  direction: StreamDirection
  cost: string
}

const EMPTY_ROW: Row = { included: false, direction: 'IN', cost: '' }

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

  const [rows, setRows] = useState<Record<string, Row>>({})

  useEffect(() => {
    if (!settingsQuery.data) return
    setRows((prev) => {
      const next = { ...prev }
      for (const setting of settingsQuery.data) {
        next[setting.streamName] = {
          included: true,
          direction: setting.direction,
          cost: setting.cost != null ? String(setting.cost) : '',
        }
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
  const allNames = Array.from(new Set([...discovered, ...Object.keys(rows)])).sort()

  const rowOf = (name: string): Row => rows[name] ?? EMPTY_ROW

  const updateRow = (name: string, patch: Partial<Row>) => {
    setRows((prev) => ({ ...prev, [name]: { ...rowOf(name), ...patch } }))
  }

  const handleSave = () => {
    const items: StreamSetting[] = Object.entries(rows)
      .filter(([, row]) => row.included)
      .map(([streamName, row]) => ({
        streamName,
        direction: row.direction,
        cost: row.direction === 'IN' ? Number(row.cost) || 0 : null,
      }))
    saveMutation.mutate(items)
  }

  return (
    <div>
      <p className="muted">
        업로드한 input.rep에서 발견된 stream 중, 원료비 계산에 쓸 원료(입력)와 생산량 계산에 쓸
        제품(출력)을 선택합니다. 체크하지 않은 stream은 계산에서 빠집니다.
      </p>

      {allNames.length === 0 && (
        <p className="muted">아직 파싱된 실행이 없습니다. "실행" 탭에서 먼저 파일을 업로드하세요.</p>
      )}

      {allNames.length > 0 && (
        <div className="card">
          <table className="stream-table">
            <thead>
              <tr>
                <th>포함</th>
                <th>Stream</th>
                <th>구분</th>
                <th>원료비 [USD/kg]</th>
              </tr>
            </thead>
            <tbody>
              {allNames.map((name) => {
                const row = rowOf(name)
                return (
                  <tr key={name}>
                    <td>
                      <input
                        type="checkbox"
                        checked={row.included}
                        onChange={(e) => updateRow(name, { included: e.target.checked })}
                      />
                    </td>
                    <td>{name}</td>
                    <td>
                      {row.included && (
                        <select
                          value={row.direction}
                          onChange={(e) => updateRow(name, { direction: e.target.value as StreamDirection })}
                        >
                          <option value="IN">{STREAM_DIRECTION_LABEL.IN}</option>
                          <option value="OUT">{STREAM_DIRECTION_LABEL.OUT}</option>
                        </select>
                      )}
                    </td>
                    <td>
                      {row.included && row.direction === 'IN' && (
                        <input
                          value={row.cost}
                          onChange={(e) => updateRow(name, { cost: e.target.value })}
                          placeholder="0.00"
                        />
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
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
