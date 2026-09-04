import type {
  AuthResponse,
  CalculationRun,
  EquipmentCosts,
  EquipmentSetting,
  EquipmentSettingItem,
  EquipmentType,
  FormulaTemplate,
  StreamSetting,
  TeaCase,
  UtilityPrice,
} from './types'

const TOKEN_KEY = 'autotea_token'
const BASE_URL = import.meta.env.VITE_API_BASE_URL

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null): void {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken()
  const headers = new Headers(options.headers)
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const res = await fetch(`${BASE_URL}${path}`, { ...options, headers })
  if (!res.ok) {
    let message = res.statusText
    try {
      const data = (await res.json()) as { message?: string }
      if (data.message) message = data.message
    } catch {
      // 응답 본문이 JSON이 아닌 경우 statusText를 그대로 사용
    }
    throw new ApiError(res.status, message)
  }
  if (res.status === 204) return undefined as T
  const contentType = res.headers.get('content-type') ?? ''
  if (contentType.includes('application/json')) {
    return (await res.json()) as T
  }
  return undefined as T
}

async function requestBlob(path: string): Promise<Blob> {
  const token = getToken()
  const headers = new Headers()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  const res = await fetch(`${BASE_URL}${path}`, { headers })
  if (!res.ok) throw new ApiError(res.status, res.statusText)
  return res.blob()
}

export interface FormulaTemplateInput {
  equipmentType: EquipmentType
  name: string
  k1: number
  k2: number
  k3: number
}

export const api = {
  loginWithGoogle: (idToken: string) =>
    request<AuthResponse>('/api/auth/google', {
      method: 'POST',
      body: JSON.stringify({ idToken }),
    }),

  listCases: () => request<TeaCase[]>('/api/cases'),
  createCase: (name: string, description: string) =>
    request<TeaCase>('/api/cases', {
      method: 'POST',
      body: JSON.stringify({ name, description }),
    }),
  getCase: (id: number) => request<TeaCase>(`/api/cases/${id}`),

  listRuns: (caseId: number) => request<CalculationRun[]>(`/api/cases/${caseId}/runs`),
  getRun: (id: number) => request<CalculationRun>(`/api/runs/${id}`),
  submitDraft: (caseId: number, name: string, xlsxFile: File, repFile: File) => {
    const form = new FormData()
    if (name.trim()) form.append('name', name.trim())
    form.append('xlsxFile', xlsxFile)
    form.append('repFile', repFile)
    return request<CalculationRun>(`/api/cases/${caseId}/runs/draft`, {
      method: 'POST',
      body: form,
    })
  },
  executeRun: (caseId: number, runId: number) =>
    request<CalculationRun>(`/api/cases/${caseId}/runs/${runId}/execute`, { method: 'POST' }),
  downloadResult: (runId: number) => requestBlob(`/api/runs/${runId}/result`),
  equipmentCosts: (runId: number) => request<EquipmentCosts>(`/api/runs/${runId}/equipment-costs`),

  listEquipmentSettings: (caseId: number) =>
    request<EquipmentSetting[]>(`/api/cases/${caseId}/equipment-settings`),
  saveEquipmentSettings: (caseId: number, items: EquipmentSettingItem[]) =>
    request<EquipmentSetting[]>(`/api/cases/${caseId}/equipment-settings`, {
      method: 'PUT',
      body: JSON.stringify({ items }),
    }),

  listFormulas: (equipmentType?: EquipmentType) =>
    request<FormulaTemplate[]>(
      `/api/formulas${equipmentType ? `?equipmentType=${equipmentType}` : ''}`,
    ),
  createFormula: (data: FormulaTemplateInput) =>
    request<FormulaTemplate>('/api/formulas', { method: 'POST', body: JSON.stringify(data) }),
  updateFormula: (id: number, data: FormulaTemplateInput) =>
    request<FormulaTemplate>(`/api/formulas/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteFormula: (id: number) => request<void>(`/api/formulas/${id}`, { method: 'DELETE' }),

  utilityPrices: () => request<UtilityPrice[]>('/api/utility-prices'),
  updateUtilityPrice: (utilityType: string, value: number) =>
    request<UtilityPrice>(`/api/utility-prices/${utilityType}`, {
      method: 'PUT',
      body: JSON.stringify({ value }),
    }),

  listStreamSettings: (caseId: number) =>
    request<StreamSetting[]>(`/api/cases/${caseId}/stream-settings`),
  saveStreamSettings: (caseId: number, items: StreamSetting[]) =>
    request<StreamSetting[]>(`/api/cases/${caseId}/stream-settings`, {
      method: 'PUT',
      body: JSON.stringify({ items }),
    }),
}

export function triggerBrowserDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
