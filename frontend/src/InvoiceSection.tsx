import { useState, type FormEvent } from 'react'
import { formatEuro, getInvoice, type Invoice } from './api'

// Invoice months are German calendar months, so end times are shown in German time too.
const formatBerlinTime = (value: string) => new Date(value).toLocaleString(undefined, { timeZone: 'Europe/Berlin' })

type Props = {
  /** Goes up whenever a session is stopped, which can change an invoice. */
  stopCount: number
}

function InvoiceSection({ stopCount }: Props) {
  const [customerId, setCustomerId] = useState('')
  const [month, setMonth] = useState('')
  const [loaded, setLoaded] = useState<{ invoice: Invoice; stopCount: number } | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const customer = customerId.trim()
  // An invoice requested before the latest stop may be out of date, so it is no longer shown.
  const invoice = loaded?.stopCount === stopCount ? loaded.invoice : null

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError(null)
    setLoaded(null)
    setLoading(true)
    try {
      setLoaded({ invoice: await getInvoice(customer, month), stopCount })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load invoice')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <form className="card" onSubmit={handleSubmit} noValidate>
        <h2>Invoice</h2>
        <label>
          Customer
          <input
            name="invoice-customer"
            value={customerId}
            placeholder="c-100"
            onChange={(e) => setCustomerId(e.target.value)}
          />
        </label>
        <label>
          Month
          <input
            name="invoice-month"
            type="month"
            value={month}
            placeholder="2026-11"
            onChange={(e) => setMonth(e.target.value)}
          />
        </label>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={!customer || !month || loading}>
          Show invoice
        </button>
      </form>

      {invoice && (
        <section className="card" aria-label="Invoice details">
          <h2>
            {invoice.customerId}, {invoice.month}
          </h2>
          {invoice.lines.length === 0 ? (
            <p className="muted">No completed sessions in this month.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Ended (German time)</th>
                  <th>Tariff</th>
                  <th>Energy</th>
                  <th>Cost</th>
                </tr>
              </thead>
              <tbody>
                {invoice.lines.map((line) => (
                  <tr key={line.sessionId}>
                    <td>{formatBerlinTime(line.endedAt)}</td>
                    <td>{line.tariffCode}</td>
                    <td>{line.energyKwh.toFixed(3)} kWh</td>
                    <td>{formatEuro(line.cost)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <dl className="totals">
            <dt>Total energy</dt>
            <dd>{invoice.totalEnergyKwh.toFixed(3)} kWh</dd>
            <dt>Net</dt>
            <dd>{formatEuro(invoice.totalNet)}</dd>
            <dt>VAT ({Math.round(invoice.vatRate * 100)}%)</dt>
            <dd>{formatEuro(invoice.totalVat)}</dd>
            <dt className="total-gross">Total (gross)</dt>
            <dd className="total-gross">{formatEuro(invoice.totalGross)}</dd>
          </dl>
        </section>
      )}
    </>
  )
}

export default InvoiceSection
