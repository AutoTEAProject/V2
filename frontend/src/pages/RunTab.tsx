import { Fragment, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, triggerBrowserDownload } from '../api/client'
import type { CalculationRun, RunStatus } from '../api/types'
import { parseEquipmentSnapshot } from '../api/types'

const STATUS_LABEL: Record<RunStatus, string> = {
  DRAFT: '준비 중',
  PARSING: '장치 파싱 중...',
  PARSED: '설정 대기',
  RUNNING: '계산 중...',
  SUCCESS: '완료',
  FAILED: '실패',
}

const POLL_STATUSES: RunStatus[] = ['PARSING', 'RUNNING']
const RETRYABLE_STATUSES: RunStatus[] = ['PARSED', 'SUCCESS', 'FAILED']

export function RunTab({ caseId, onGoToSettings }: { caseId: number; onGoToSettings: () => void }) {
  const queryClient = useQueryClient()
  const xlsxInputRef = useRef<HTMLInputElement>(null)
  const repInputRef = useRef<HTMLInputElement>(null)
  const [runName, setRunName] = useState('')
  const [uploadError, setUploadError] = useState<string | null>(null)
  const [expandedLogRunId, setExpandedLogRunId] = useState<number | null>(null)

  const runsQuery = useQuery({
    queryKey: ['runs', caseId],
    queryFn: () => api.listRuns(caseId),
    refetchInterval: (query) => {
      const runs = query.state.data
      return runs?.some((r) => POLL_STATUSES.includes(r.status)) ? 2000 : false
    },
  })

  const draftMutation = useMutation({
    mutationFn: () => {
      const xlsxFile = xlsxInputRef.current?.files?.[0]
      const repFile = repInputRef.current?.files?.[0]
      if (!xlsxFile || !repFile) {
        throw new Error('input.xlsx와 input.rep 파일을 모두 선택하세요.')
      }
      return api.submitDraft(caseId, runName, xlsxFile, repFile)
    },
    onSuccess: () => {
      setUploadError(null)
      setRunName('')
      if (xlsxInputRef.current) xlsxInputRef.current.value = ''
      if (repInputRef.current) repInputRef.current.value = ''
      void queryClient.invalidateQueries({ queryKey: ['runs', caseId] })
    },
    onError: (e: unknown) => {
      setUploadError(e instanceof Error ? e.message : '업로드에 실패했습니다.')
    },
  })

  const executeMutation = useMutation({
    mutationFn: (runId: number) => api.executeRun(caseId, runId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['runs', caseId] })
    },
  })

  const downloadRun = async (run: CalculationRun) => {
    try {
      const blob = await api.downloadResult(run.id)
      triggerBrowserDownload(blob, `output-${run.id}.xlsx`)
    } catch {
      window.alert('결과 파일 다운로드에 실패했습니다.')
    }
  }

  const runs = runsQuery.data ?? []

  return (
    <div>
      <div className="card">
        <h2>새 실행: 파일 업로드</h2>
        <p className="muted">
          input.xlsx, input.rep를 올리면 먼저 장치 목록만 파싱합니다. 이후 "장치비 설정"/"Utility 설정"
          탭에서 설정을 확인/조정한 뒤 계산을 실행하세요. 같은 파일로 설정만 바꿔 다시 계산하고 싶으면
          아래 목록에서 재실행 버튼을 쓰면 되고, 파일을 다시 올릴 필요는 없습니다.
        </p>
        <div className="form-row">
          <label htmlFor="run-name">실행 이름 (선택)</label>
          <input
            id="run-name"
            value={runName}
            onChange={(e) => setRunName(e.target.value)}
            placeholder="예: 2026 베이스케이스"
          />
        </div>
        <div className="form-row">
          <label htmlFor="xlsx-file">input.xlsx</label>
          <input id="xlsx-file" type="file" accept=".xlsx" ref={xlsxInputRef} />
        </div>
        <div className="form-row">
          <label htmlFor="rep-file">input.rep</label>
          <input id="rep-file" type="file" accept=".rep,.txt" ref={repInputRef} />
        </div>
        <button
          type="button"
          className="btn btn-primary"
          disabled={draftMutation.isPending}
          onClick={() => draftMutation.mutate()}
        >
          {draftMutation.isPending ? '업로드 중...' : '업로드 및 장치 파싱'}
        </button>
        {uploadError && <p className="error-text">{uploadError}</p>}
      </div>

      <div className="card">
        <h2>실행 이력</h2>
        {runsQuery.isLoading && <p>불러오는 중...</p>}
        {runs.length === 0 && !runsQuery.isLoading && <p className="muted">아직 실행 이력이 없습니다.</p>}
        <table className="run-table">
          <thead>
            <tr>
              <th>이름</th>
              <th>상태</th>
              <th>장치 수</th>
              <th>생성 시각</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {runs.map((run) => {
              const equipment = parseEquipmentSnapshot(run)
              const isExecutingThis = executeMutation.isPending && executeMutation.variables === run.id
              const logExpanded = expandedLogRunId === run.id
              return (
                <Fragment key={run.id}>
                  <tr>
                    <td>{run.name ?? `실행 #${run.id}`}</td>
                    <td>
                      <span className={`status-badge status-${run.status.toLowerCase()}`}>
                        {STATUS_LABEL[run.status]}
                      </span>
                      {run.status === 'FAILED' && run.errorMessage && (
                        <div className="error-text small">{run.errorMessage}</div>
                      )}
                    </td>
                    <td>{equipment.length > 0 ? `${equipment.length}개` : '-'}</td>
                    <td>{new Date(run.createdAt).toLocaleString()}</td>
                    <td className="run-actions">
                      {RETRYABLE_STATUSES.includes(run.status) && (
                        <button
                          type="button"
                          className="btn btn-primary btn-sm"
                          disabled={isExecutingThis}
                          onClick={() => executeMutation.mutate(run.id)}
                        >
                          {isExecutingThis
                            ? '실행 중...'
                            : run.status === 'PARSED'
                              ? '설정 반영해서 계산 실행'
                              : '같은 파일로 재실행'}
                        </button>
                      )}
                      {run.status === 'FAILED' && (
                        <button type="button" className="btn btn-sm" onClick={onGoToSettings}>
                          설정 변경
                        </button>
                      )}
                      {run.status === 'SUCCESS' && (
                        <button type="button" className="btn btn-sm" onClick={() => void downloadRun(run)}>
                          결과 다운로드
                        </button>
                      )}
                      {run.logs && (
                        <button
                          type="button"
                          className="btn btn-sm"
                          onClick={() => setExpandedLogRunId(logExpanded ? null : run.id)}
                        >
                          {logExpanded ? '로그 닫기' : '로그 보기'}
                        </button>
                      )}
                    </td>
                  </tr>
                  {logExpanded && (
                    <tr>
                      <td colSpan={5}>
                        <pre className="run-log">{run.logs}</pre>
                      </td>
                    </tr>
                  )}
                </Fragment>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}
