import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../api/client'

export function ProjectListPage() {
  const queryClient = useQueryClient()
  const { data: cases, isLoading, error } = useQuery({
    queryKey: ['cases'],
    queryFn: api.listCases,
  })

  const [name, setName] = useState('')
  const [description, setDescription] = useState('')

  const createMutation = useMutation({
    mutationFn: () => api.createCase(name.trim(), description.trim()),
    onSuccess: () => {
      setName('')
      setDescription('')
      void queryClient.invalidateQueries({ queryKey: ['cases'] })
    },
  })

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!name.trim()) return
    createMutation.mutate()
  }

  return (
    <div className="page">
      <h1>프로젝트</h1>

      <form className="card create-form" onSubmit={handleSubmit}>
        <h2>새 프로젝트</h2>
        <div className="form-row">
          <label htmlFor="case-name">이름</label>
          <input
            id="case-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="프로젝트 이름"
            required
          />
        </div>
        <div className="form-row">
          <label htmlFor="case-description">설명</label>
          <textarea
            id="case-description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="설명(선택)"
            rows={2}
          />
        </div>
        <button type="submit" className="btn btn-primary" disabled={createMutation.isPending}>
          {createMutation.isPending ? '생성 중...' : '프로젝트 생성'}
        </button>
        {createMutation.isError && <p className="error-text">프로젝트 생성에 실패했습니다.</p>}
      </form>

      {isLoading && <p>불러오는 중...</p>}
      {error && <p className="error-text">프로젝트 목록을 불러오지 못했습니다.</p>}

      <ul className="project-list">
        {cases?.map((c) => (
          <li key={c.id} className="card project-list-item">
            <Link to={`/projects/${c.id}`}>
              <strong>{c.name}</strong>
              {c.description && <span className="project-desc"> — {c.description}</span>}
            </Link>
            <span className="project-date">{new Date(c.createdAt).toLocaleDateString()}</span>
          </li>
        ))}
        {cases && cases.length === 0 && <p>아직 프로젝트가 없습니다.</p>}
      </ul>
    </div>
  )
}
