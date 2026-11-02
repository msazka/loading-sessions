import { useState } from 'react'
import { formatEuro, type Session } from './api'

type Props = {
  sessions: Session[]
  onStop: (id: string, energyKwh: number) => Promise<void>
}

function SessionTable({ sessions, onStop }: Props) {
  // Energy input per active session, keyed by session id.
  const [energy, setEnergy] = useState<Record<string, string>>({})

  if (sessions.length === 0) return <p className="muted">No sessions yet.</p>

  return (
    <table>
      <thead>
        <tr>
          <th>Customer</th>
          <th>Charge point</th>
          <th>Tariff</th>
          <th>Status</th>
          <th>Started</th>
          <th>Energy</th>
          <th>Cost</th>
          <th />
        </tr>
      </thead>
      <tbody>
        {sessions.map((s) => (
          <tr key={s.id}>
            <td>{s.customerId}</td>
            <td>{s.chargePointId}</td>
            <td>{s.tariffCode}</td>
            <td>
              <span className={`status status-${s.status.toLowerCase()}`}>{s.status}</span>
            </td>
            <td>{new Date(s.startedAt).toLocaleString()}</td>
            <td>{s.energyKwh == null ? '–' : `${s.energyKwh} kWh`}</td>
            <td>{formatEuro(s.cost)}</td>
            <td className="actions">
              <input
                className="kwh"
                type="number"
                min="0"
                step="0.001"
                placeholder="kWh"
                value={energy[s.id] ?? ''}
                onChange={(e) => setEnergy((prev) => ({ ...prev, [s.id]: e.target.value }))}
              />
              <button disabled={!energy[s.id]} onClick={() => onStop(s.id, Number(energy[s.id]))}>
                Stop
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export default SessionTable
