const statePattern = /<!-- <WF_STATE>([\s\S]*?)<\/WF_STATE> -->/
const queuePattern = /<!-- BUMP_QUEUE -->[\s\S]*?<!-- \/BUMP_QUEUE -->/

function readState (body = '') {
  body = body || ''
  const match = statePattern.exec(body)
  if (!match) {
    if (body.includes('<WF_STATE>')) throw new Error('Malformed WF_STATE block')
    return null
  }
  const state = JSON.parse(match[1])
  if (state.schema !== 1) throw new Error(`Unsupported bump state schema: ${state.schema}`)
  return state
}

function writeState (body, state) {
  const pending = state.pending.map(request => {
    const links = []
    if (request.pr) links.push(`[PR #${request.pr}](https://github.com/PrismarineJS/minecraft-data/pull/${request.pr})`)
    if (request.issue) links.push(`[issue #${request.issue}](https://github.com/PrismarineJS/minecraft-data/issues/${request.issue})`)
    return `- ${request.version}${links.length ? ` (${links.join(', ')})` : ''}`
  }).join('\n')
  const summary = [
    '<!-- BUMP_QUEUE -->',
    '**Version bump queue**',
    state.successor
      ? `Queue handed off to #${state.successor}.`
      : `${pending || 'No versions waiting.'}\n\nMerging or closing this PR advances the queue. Closing without merging skips this version.`,
    state.predecessor ? `Continues the queue from #${state.predecessor}.` : '',
    '<!-- /BUMP_QUEUE -->'
  ].filter(Boolean).join('\n\n')
  const updated = (body || '').replace(queuePattern, '').replace(statePattern, '').trimEnd()
  return `${updated}\n\n${summary}\n\n<!-- <WF_STATE>${JSON.stringify(state)}</WF_STATE> -->`
}

function initialState (version, pending = [], predecessor = null) {
  return { schema: 1, version, pending, predecessor, successor: null }
}

// All callers must share the workflow's concurrency group. Reconcile the latest
// PR, rather than the event snapshot: close events and dispatches can arrive late.
async function runQueue ({ api, supported, request }) {
  const allPulls = await api.list()
  for (const pr of allPulls) {
    pr.queue = readState(pr.body)
    if (!pr.queue && pr.state === 'open') throw new Error(`Initialize WF_STATE on bump PR #${pr.number} before continuing`)
  }
  // Old closed PRs predate this queue. Migration is a one-time state update.
  const pulls = allPulls.filter(pr => pr.queue).sort((a, b) => a.number - b.number)
  if (pulls.filter(pr => pr.state === 'open').length > 1) throw new Error('Multiple open bump PRs')

  async function save (pr) {
    // Preserve description edits made since the list request.
    const current = await api.get(pr.number)
    await api.update(pr.number, writeState(current.body, pr.queue))
  }

  // Recover a creation that succeeded before the predecessor could be updated.
  for (const pr of pulls) {
    if (!pr.queue.predecessor) continue
    const previous = pulls.find(candidate => candidate.number === pr.queue.predecessor)
    if (!previous) throw new Error(`Missing predecessor #${pr.queue.predecessor}`)
    if (previous.queue.successor && previous.queue.successor !== pr.number) throw new Error('Conflicting bump successors')
    if (!previous.queue.successor) {
      previous.queue.successor = pr.number
      await save(previous)
    }
  }

  const handled = new Set([...supported, ...pulls.map(pr => pr.queue.version)])
  if (request && handled.has(request.version)) request = null
  const latest = pulls.at(-1)
  if (!latest) {
    if (request) {
      const refreshed = await api.prepare()
      if (!refreshed.includes(request.version)) await api.create(request, initialState(request.version))
    }
    return
  }
  if (pulls.some(pr => pr.state === 'open' && pr !== latest)) throw new Error('An older bump PR is still open')
  if (latest.queue.successor) throw new Error(`Missing successor #${latest.queue.successor}`)

  if (request && !latest.queue.pending.some(item => item.version === request.version)) {
    latest.queue.pending.push(request)
  }
  latest.queue.pending = latest.queue.pending
    .filter(item => !handled.has(item.version))
    .sort((a, b) => a.version.localeCompare(b.version, 'en', { numeric: true }))

  // Persist arrivals before attempting any git operations or PR creation.
  const currentState = readState(latest.body)
  if (JSON.stringify(currentState) !== JSON.stringify(latest.queue)) await save(latest)
  if (latest.state === 'open' || !latest.queue.pending.length) return

  // A PR may have merged after the workflow's initial checkout. Only now that
  // closure is observed, refresh main and reconcile against its actual manifest.
  const refreshed = await api.prepare()
  const pending = latest.queue.pending.filter(item => !refreshed.includes(item.version))
  if (pending.length !== latest.queue.pending.length) {
    latest.queue.pending = pending
    await save(latest)
  }
  if (!pending.length) return

  const [next, ...remaining] = latest.queue.pending
  const successor = await api.create(next, initialState(next.version, remaining, latest.number))
  latest.queue.successor = successor.number
  await save(latest)
}

module.exports = { readState, writeState, initialState, runQueue }
