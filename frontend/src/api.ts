export type SessionStatus = 'ACTIVE' | 'COMPLETED'

export type Tariff = {
  code: string
  name: string
  pricePerKwh: number
  sessionFee: number
}

export type Session = {
  id: string
  customerId: string
  chargePointId: string
  tariffCode: string
  status: SessionStatus
  startedAt: string
  endedAt: string | null
  energyKwh: number | null
  cost: number | null
}

export type InvoiceLine = {
  sessionId: string
  endedAt: string
  tariffCode: string
  energyKwh: number
  cost: number
}

export type Invoice = {
  customerId: string
  month: string
  lines: InvoiceLine[]
  totalEnergyKwh: number
  totalGross: number
  vatRate: number
  totalNet: number
  totalVat: number
}

export type StartSessionRequest = {
  customerId: string
  chargePointId: string
  tariffCode: string
}

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string>) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, init)
  const body: unknown = await res.json().catch(() => null)
  if (!res.ok) {
    const error = (body ?? {}) as { message?: string; errors?: Record<string, string> }
    throw new ApiError(res.status, error.message ?? `HTTP ${res.status}`, error.errors ?? {})
  }
  return body as T
}

const json = { 'Content-Type': 'application/json' }

export function listTariffs(): Promise<Tariff[]> {
  return request<Tariff[]>('/api/tariffs')
}

export function listSessions(customerId?: string): Promise<Session[]> {
  const query = customerId ? `?customerId=${encodeURIComponent(customerId)}` : ''
  return request<Session[]>(`/api/sessions${query}`)
}

export function startSession(body: StartSessionRequest): Promise<Session> {
  return request<Session>('/api/sessions', { method: 'POST', headers: json, body: JSON.stringify(body) })
}

export function stopSession(id: string, energyKwh: number): Promise<Session> {
  return request<Session>(`/api/sessions/${id}/stop`, {
    method: 'POST',
    headers: json,
    body: JSON.stringify({ energyKwh }),
  })
}

export function getInvoice(customerId: string, month: string): Promise<Invoice> {
  return request<Invoice>(`/api/customers/${encodeURIComponent(customerId)}/invoices/${encodeURIComponent(month)}`)
}

export const formatEuro =(value: number | null) =>
  value == null ? '–' : new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value)
