import { useState, type FormEvent } from 'react'
import { ApiError, startSession, type Tariff } from './api'

type Props = {
  tariffs: Tariff[]
  onStarted: () => void
}

function StartSessionForm({ tariffs, onStarted }: Props) {
  const [customerId, setCustomerId] = useState('')
  const [chargePointId, setChargePointId] = useState('DE*DCS*E0001')
  const [tariffCode, setTariffCode] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [message, setMessage] = useState<string | null>(null)

  const selectedTariff = tariffCode || tariffs[0]?.code || ''

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setErrors({})
    setMessage(null)
    try {
      await startSession({ customerId, chargePointId, tariffCode: selectedTariff })
      setCustomerId('')
      onStarted()
    } catch (err) {
      if (err instanceof ApiError) setErrors(err.fieldErrors)
      setMessage(err instanceof Error ? err.message : 'Request failed')
    }
  }

  return (
    <form className="card" onSubmit={handleSubmit} noValidate>
      <h2>Start session</h2>
      <label>
        Customer
        <input value={customerId} placeholder="c-100" onChange={(e) => setCustomerId(e.target.value)} />
        {errors.customerId && <span className="field-error">{errors.customerId}</span>}
      </label>
      <label>
        Charge point
        <input value={chargePointId} onChange={(e) => setChargePointId(e.target.value)} />
        {errors.chargePointId && <span className="field-error">{errors.chargePointId}</span>}
      </label>
      <label>
        Tariff
        <select value={selectedTariff} onChange={(e) => setTariffCode(e.target.value)}>
          {tariffs.map((t) => (
            <option key={t.code} value={t.code}>
              {t.name} ({t.pricePerKwh} €/kWh)
            </option>
          ))}
        </select>
      </label>
      {message && <p className="error">{message}</p>}
      <button type="submit">Start</button>
    </form>
  )
}

export default StartSessionForm
