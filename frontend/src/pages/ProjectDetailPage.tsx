import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import { RunTab } from './RunTab'
import { EquipmentCostSettingsTab } from './EquipmentCostSettingsTab'
import { UtilitySettingsTab } from './UtilitySettingsTab'
import { StreamSettingsTab } from './StreamSettingsTab'

type Tab = 'run' | 'cost' | 'utility' | 'stream'

export function ProjectDetailPage() {
  const { caseId } = useParams<{ caseId: string }>()
  const id = Number(caseId)
  const [tab, setTab] = useState<Tab>('run')

  const { data: teaCase } = useQuery({
    queryKey: ['case', id],
    queryFn: () => api.getCase(id),
    enabled: Number.isFinite(id),
  })

  if (!Number.isFinite(id)) return <p className="error-text">잘못된 프로젝트입니다.</p>

  return (
    <div className="page">
      <h1>{teaCase?.name ?? `프로젝트 #${id}`}</h1>
      {teaCase?.description && <p className="muted">{teaCase.description}</p>}

      <div className="tabs">
        <button
          type="button"
          className={tab === 'run' ? 'tab active' : 'tab'}
          onClick={() => setTab('run')}
        >
          실행
        </button>
        <button
          type="button"
          className={tab === 'cost' ? 'tab active' : 'tab'}
          onClick={() => setTab('cost')}
        >
          장치비 설정
        </button>
        <button
          type="button"
          className={tab === 'utility' ? 'tab active' : 'tab'}
          onClick={() => setTab('utility')}
        >
          Utility 설정
        </button>
        <button
          type="button"
          className={tab === 'stream' ? 'tab active' : 'tab'}
          onClick={() => setTab('stream')}
        >
          원료/제품 설정
        </button>
      </div>

      {tab === 'run' && <RunTab caseId={id} onGoToSettings={() => setTab('cost')} />}
      {tab === 'cost' && <EquipmentCostSettingsTab caseId={id} />}
      {tab === 'utility' && <UtilitySettingsTab caseId={id} />}
      {tab === 'stream' && <StreamSettingsTab caseId={id} />}
    </div>
  )
}
