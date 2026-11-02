# Notes: other bugs found

Found while working on Ticket 1. None of these are fixed. "Reproduced" means seen in the running
app; "from reading the code" means it has not been reproduced.

## Bugs

### 1. Failed stops show no error in the UI (reproduced)
`handleStop` in `frontend/src/App.tsx` sets the error, then calls `loadSessions()`, and a successful
reload runs `setError(null)`. So the message is cleared right away.

- **Seen:** in an out-of-date tab, stopping with 99 kWh got a 409. No error appeared and the row
  changed to `COMPLETED`, so the user isn't told their value was discarded.
- **Affects:** the loser of a simultaneous stop (Ticket 1), and any other failed stop.
- **Possible fix:** don't clear the error on the reload that follows a failed stop.

### 2. Cost is calculated from energy that is then rounded differently (reproduced)
`ChargingSessionService.stop` prices the energy as sent, but `energyKwh` is stored with 3 decimals.
Nothing rejects or rounds extra decimals first.

- **Seen:** stopping with `0.2115` kWh on `AC_STANDARD` (0.59 €/kWh) bills 0.12 € (0.124785), but
  the stored energy is `0.212`, which would cost 0.13 € (0.12508).
- **Also:** the stop response returns `0.2115` while later reads return `0.212`.
- **Affects:** Ticket 2, because an invoice line's energy and cost may not match.
- **Possible fix:** reject more than 3 decimals with `@Digits`, or round before pricing.

### 3. `customerId` keeps surrounding whitespace (reproduced)
`StartSessionForm` and the API store the value as sent, but the UI filter in `App.tsx` trims its
input.

- **Seen:** a session started for `" n-ws "` is not found by filtering on `n-ws`.
- **Affects:** Ticket 2, because that session wouldn't appear on the customer's invoice.
- **Possible fix:** trim on the backend when a session is started.

### 4. A charge point can have several active sessions (reproduced)
`ChargingSessionService.start` doesn't check for an active session on the same charge point.

- **Seen:** two different customers both got `ACTIVE` sessions on `CP-C`.
- **Needs a decision:** this may be intended, so check with the team before changing it.

### 5. Session list can show results for the wrong filter (from reading the code)
`loadSessions` in `App.tsx` runs on every keystroke in the filter and doesn't cancel earlier
requests. If responses arrive out of order, the table can show the results for an older filter value.

## Setup and configuration

### 6. The README's backend commands fail on JDK 25 (reproduced)
Kotlin Gradle plugin 2.1.21 fails with a bare `What went wrong: 25` when Gradle runs on JDK 25.
Workaround: run Gradle on JDK 21. `CLAUDE.md` documents this, but the README doesn't.

### 7. H2 console is always on (reproduced)
`application.yml` enables it for every run, with user `sa` and a blank password, and
`/h2-console/` returns 200. There is no separate dev profile. Fine for a demo, but it should be
turned off before any shared deployment. Access from another machine was not tested.

### 8. Minor
- The README API table doesn't list the 409 from `POST /api/sessions/{id}/stop`.
- `/favicon.ico` returns 404, which logs one console error on page load (reproduced).
