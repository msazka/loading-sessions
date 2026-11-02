import { useCallback, useEffect, useState } from 'react'
import { listSessions, listTariffs, stopSession, type Session, type Tariff } from './api'
import SessionTable from './SessionTable'
import StartSessionForm from './StartSessionForm'
import './App.css'

function App() {
  const [tariffs, setTariffs] = useState<Tariff[]>([])
  const [sessions, setSessions] = useState<Session[]>([])
  const [customerFilter, setCustomerFilter] = useState('')
  const [error, setError] = useState<string | null>(null)

  const loadSessions = useCallback(() => {
    listSessions(customerFilter.trim() || undefined)
      .then((data) => {
        setSessions(data)
        setError(null)
      })
      .catch((err: Error) => setError(err.message))
  }, [customerFilter])

  useEffect(() => {
    listTariffs()
      .then(setTariffs)
      .catch((err: Error) => setError(err.message))
  }, [])

  useEffect(() => {
    loadSessions()
  }, [loadSessions])

  async function handleStop(id: string, energyKwh: number) {
    try {
      await stopSession(id, energyKwh)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to stop session')
    }
    loadSessions()
  }

  return (
    <main className="app">
      <h1>Charging Sessions</h1>
      <StartSessionForm tariffs={tariffs} onStarted={loadSessions} />

      <section className="card">
        <div className="list-header">
          <h2>Sessions</h2>
          <input
            placeholder="Filter by customer"
            value={customerFilter}
            onChange={(e) => setCustomerFilter(e.target.value)}
          />
        </div>
        {error && <p className="error">{error}</p>}
        <SessionTable sessions={sessions} onStop={handleStop} />
      </section>
    </main>
  )
}

export default App
