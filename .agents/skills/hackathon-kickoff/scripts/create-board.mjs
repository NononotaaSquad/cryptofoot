#!/usr/bin/env node
// Creates (or resumes) the GitHub Project, labels, milestones, domain epics and task issues
// described by .agents/context/backlog.json. Writes issue/project numbers back into the file,
// so a re-run skips everything already created.
import { execFileSync, spawnSync } from 'node:child_process'
import { readFileSync, writeFileSync } from 'node:fs'

const PHASES = {
  1: { field: 'P1 Minimum', label: 'phase:1-minimum', milestone: 'Phase 1: Minimum', color: 'd73a4a',
    description: 'Shortest end-to-end path through the demo scenario, happy path only.' },
  2: { field: 'P2 Ready product', label: 'phase:2-product', milestone: 'Phase 2: Ready product', color: 'fbca04',
    description: 'Full demo scenario with invariants, validation, edge cases and deployment.' },
  3: { field: 'P3 Enhancements', label: 'phase:3-enhancement', milestone: 'Phase 3: Enhancements', color: '0e8a16',
    description: 'Independent, cuttable improvements ordered by judging-score gain per hour.' },
}
const AREAS = { backend: 'Backend', frontend: 'Frontend', fullstack: 'Full stack', infra: 'Infra' }
const SIZES = { S: 'S (≤1h)', M: 'M (≤3h)' }
const VAGUE = /\b(properly|correctly|appropriate(ly)?|as needed|if needed|etc|and so on|various|some|nice|good|better|user[- ]friendly|intuitive|seamless(ly)?|robust|should work|works?|handles?|handling|supports?|improve[sd]?|optimi[sz]e[sd]?|TBD|TODO|maybe|possibly)\b/i

const args = process.argv.slice(2)
const dryRun = args.includes('--dry-run')
const planPath = args.find((a) => !a.startsWith('--')) ?? '.agents/context/backlog.json'
const plan = JSON.parse(readFileSync(planPath, 'utf8'))
const taskByKey = new Map((plan.tasks ?? []).map((t) => [t.key, t]))
const slicesByTask = new Map()
const ref = (key) => (taskByKey.get(key)?.issue ? `#${taskByKey.get(key).issue}` : key)

const save = () => writeFileSync(planPath, `${JSON.stringify(plan, null, 2)}\n`)
const gh = (...a) => execFileSync('gh', a, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'inherit'] }).trim()
const ghJson = (...a) => JSON.parse(gh(...a))
const fail = (msg) => {
  console.error(`✗ ${msg}`)
  process.exit(1)
}

function validate() {
  const errors = []
  const domains = new Map((plan.domains ?? []).map((d) => [d.name, d]))
  if (!plan.project?.title) errors.push('project.title is required')
  if (domains.size === 0) errors.push('domains must list at least one domain')
  for (const d of plan.domains ?? []) {
    if (!/^[a-z0-9-]+$/.test(d.slug ?? '')) errors.push(`domain "${d.name}": slug must be kebab-case`)
    if (!d.description) errors.push(`domain "${d.name}": description is required`)
  }
  const seen = new Map()
  for (const t of plan.tasks ?? []) {
    const where = `task ${t.key ?? '?'}`
    if (!t.key) errors.push('every task needs a key')
    if (seen.has(t.key)) errors.push(`${where}: duplicate key`)
    if (!PHASES[t.phase]) errors.push(`${where}: phase must be 1, 2 or 3`)
    if (!domains.has(t.domain)) errors.push(`${where}: unknown domain "${t.domain}"`)
    if (!AREAS[t.area]) errors.push(`${where}: area must be one of ${Object.keys(AREAS).join(', ')}`)
    if (!SIZES[t.size]) errors.push(`${where}: size must be S or M (split anything bigger)`)
    if (!t.title?.startsWith(`${t.domain}: `)) errors.push(`${where}: title must start with "${t.domain}: "`)
    if ((t.title?.length ?? 0) > 70) errors.push(`${where}: title longer than 70 characters`)
    if (/[.!?]$/.test(t.title ?? '')) errors.push(`${where}: title must not end with punctuation`)
    if (!/^As (a|an|the) [^,]+, I can .+ so that .+\.$/.test(t.goal ?? '')) {
      errors.push(`${where}: goal must be one sentence "As a/an/the <actor>, I can <capability> so that <value>."`)
    }
    const criteria = t.acceptance ?? []
    if (criteria.length < 1 || criteria.length > 5) errors.push(`${where}: needs 1-5 acceptance criteria (more means split the task)`)
    for (const c of criteria) {
      if (!/^Given .+, when .+, then .+[^.]$/.test(c)) errors.push(`${where}: criterion must be "Given …, when …, then …" without a final period: "${c}"`)
    }
    if (t.phase < 3 && !/^(Step \d+|All steps)/.test(t.demoStep ?? '')) {
      errors.push(`${where}: phase ${t.phase} tasks need demoStep starting with "Step <n>" or "All steps"`)
    }
    for (const key of ['rules', 'edgeCases']) {
      if (t[key]) errors.push(`${where}: "${key}" is not supported: write each rule or edge case as an acceptance criterion`)
    }
    const needs = { backend: ['backend'], frontend: ['frontend'], fullstack: ['backend', 'frontend'] }[t.area] ?? []
    for (const side of needs) {
      if (!t.scope?.[side]?.length) errors.push(`${where}: area ${t.area} needs scope.${side} naming concrete artifacts`)
    }
    const text = [t.title, t.goal, ...criteria, ...(t.scope?.backend ?? []), ...(t.scope?.frontend ?? [])].join('\n')
    const vague = text.match(VAGUE)
    if (vague) errors.push(`${where}: vague wording "${vague[0]}": state the exact behavior, value or artifact`)
    for (const dep of t.dependsOn ?? []) {
      const before = seen.get(dep)
      if (!before) errors.push(`${where}: dependency ${dep} must be an earlier task`)
      else if (before.phase > t.phase) errors.push(`${where}: depends on later-phase task ${dep}`)
    }
    seen.set(t.key, t)
  }
  if (!plan.hamburgers?.length) errors.push('hamburgers must list at least one story')
  for (const h of plan.hamburgers ?? []) {
    const where = `hamburger "${h.story}"`
    if (!domains.has(h.domain)) errors.push(`${where}: unknown domain "${h.domain}"`)
    if (!h.steps?.length) errors.push(`${where}: needs at least one step`)
    for (const s of h.steps ?? []) {
      const at = `${where} › "${s.step}"`
      if (!s.options?.length) errors.push(`${at}: needs options`)
      if (!s.options?.some((o) => o.phase === 1 && !o.discarded)) {
        errors.push(`${at}: the first bite needs a phase 1 option for every step`)
      }
      for (const o of s.options ?? []) {
        if (o.discarded) continue
        const t = taskByKey.get(o.task)
        if (!PHASES[o.phase]) errors.push(`${at} › "${o.option}": phase must be 1, 2 or 3, or set "discarded"`)
        else if (!t) errors.push(`${at} › "${o.option}": unknown task "${o.task}"`)
        else if (t.phase !== o.phase) errors.push(`${at} › "${o.option}": phase ${o.phase} but ${o.task} is phase ${t.phase}`)
        else slicesByTask.set(o.task, [...(slicesByTask.get(o.task) ?? []), { story: h.story, step: s.step, option: o.option }])
      }
    }
  }
  for (const t of plan.tasks ?? []) {
    if (t.domain !== 'Delivery' && !slicesByTask.has(t.key)) {
      errors.push(`task ${t.key}: not an option of any hamburger step (only Delivery tasks may be outside)`)
    }
  }
  if (!plan.tasks?.length) errors.push('tasks must not be empty')
  for (const p of [1, 2]) {
    if (!plan.tasks?.some((t) => t.phase === p)) errors.push(`phase ${p} has no tasks`)
  }
  if (errors.length) fail(`backlog is invalid:\n  - ${errors.join('\n  - ')}`)
}

const list = (items) => items.map((i) => `- ${i}`).join('\n')
const checklist = (items) => items.map((i) => `- [ ] ${i}`).join('\n')

function renderHamburger(h) {
  const live = (s, p) => s.options.filter((o) => !o.discarded && o.phase === p)
  const cell = (s, p) => live(s, p).map((o) => `${o.option} (${ref(o.task)})`).join('<br>') || '—'
  const dropped = (s) => s.options.filter((o) => o.discarded).map((o) => `~~${o.option}~~ ${o.discarded}`).join('<br>') || '—'
  const rows = h.steps.map((s) => `| ${s.step} | ${cell(s, 1)} | ${cell(s, 2)} | ${cell(s, 3)} | ${dropped(s)} |`)
  return [
    `### ${h.story}`,
    '',
    `| Step | ${Object.values(PHASES).map((p) => p.field).join(' | ')} | Discarded |`,
    '|---|---|---|---|---|',
    ...rows,
  ].join('\n')
}

function renderTask(t) {
  const slices = slicesByTask.get(t.key) ?? []
  const facts = [
    `**Goal:** ${t.goal}`,
    `**Demo:** ${t.demoStep ?? 'not in the core demo'}`,
    ...slices.map((s) => `**Slice:** ${s.story} › ${s.step}: ${s.option}`),
    ...(t.dependsOn?.length ? [`**Blocked by:** ${t.dependsOn.map(ref).join(', ')}`] : []),
  ]
  const sections = [
    `\`${PHASES[t.phase].field}\` · \`${t.domain}\` · ${AREAS[t.area]} · ${SIZES[t.size]}`,
    facts.join('<br>\n'),
    `## Acceptance criteria\n\n${checklist(t.acceptance)}`,
  ]
  const scope = [['Backend', t.scope?.backend], ['Frontend', t.scope?.frontend]].filter(([, items]) => items?.length)
  if (scope.length) sections.push(`## Scope\n\n${scope.map(([side, items]) => `**${side}**\n${list(items)}`).join('\n\n')}`)
  if (t.outOfScope?.length) sections.push(`## Out of scope\n\n${list(t.outOfScope)}`)
  sections.push('Done = every criterion has a passing spec or test, checks are green, verified in the running app (AGENTS.md › Definition of done).')
  return sections.join('\n\n')
}

function renderEpic(d) {
  const tasks = plan.tasks.filter((t) => t.domain === d.name)
  const hamburgers = (plan.hamburgers ?? []).filter((h) => h.domain === d.name)
  return [
    `> Domain epic · tracks every task of the **${d.name}** domain as sub-issues.`,
    `## Domain\n\n${d.description}`,
    '## Context\n\n- Model: `.agents/context/domain-model.md`\n- Terms: `.agents/context/glossary.md`\n- Product rules: `.agents/context/product.md`',
    `## Tasks by phase\n\n${[1, 2, 3].map((p) => `${PHASES[p].field}: ${tasks.filter((t) => t.phase === p).length}`).join(' · ')}`,
    ...(hamburgers.length ? [`## Hamburgers\n\n${hamburgers.map(renderHamburger).join('\n\n')}`] : []),
  ].join('\n\n')
}

function refreshEpics(repo) {
  for (const d of plan.domains) gh('issue', 'edit', String(d.epic), '--repo', repo, '--body', renderEpic(d))
  console.log('✓ epic hamburgers linked to issue numbers')
}

function checkAuth() {
  const status = spawnSync('gh', ['auth', 'status'], { encoding: 'utf8' })
  if (status.status !== 0) fail('gh is not authenticated: run `gh auth login`')
  if (!/'project'/.test(`${status.stdout}${status.stderr}`)) {
    fail('gh token lacks the `project` scope: run `gh auth refresh -s project` and retry')
  }
}

const issueNumber = (url) => Number(url.match(/\/issues\/(\d+)/)?.[1] ?? fail(`unexpected gh output: ${url}`))

function ensureProject(owner, repo) {
  if (!plan.project.number) {
    const created = ghJson('project', 'create', '--owner', owner, '--title', plan.project.title, '--format', 'json')
    Object.assign(plan.project, { owner, number: created.number, url: created.url })
    save()
    console.log(`✓ project ${created.url}`)
  }
  spawnSync('gh', ['project', 'link', String(plan.project.number), '--owner', owner, '--repo', repo], { stdio: 'ignore' })
}

function ensureFields(owner) {
  const { number } = plan.project
  const existing = ghJson('project', 'field-list', String(number), '--owner', owner, '--format', 'json').fields
  const wanted = {
    Phase: Object.values(PHASES).map((p) => p.field),
    Domain: plan.domains.map((d) => d.name),
    Area: Object.values(AREAS),
    Size: Object.values(SIZES),
  }
  for (const [name, options] of Object.entries(wanted)) {
    const field = existing.find((f) => f.name === name)
    if (!field) {
      gh('project', 'field-create', String(number), '--owner', owner, '--name', name,
        '--data-type', 'SINGLE_SELECT', '--single-select-options', options.join(','))
      console.log(`✓ field ${name}`)
      continue
    }
    const missing = options.filter((o) => !field.options?.some((x) => x.name === o))
    if (missing.length) console.warn(`! field ${name} lacks options ${missing.join(', ')}: add them in the project settings`)
  }
}

function ensureLabels(repo) {
  const labels = [
    ...Object.values(PHASES).map((p) => [p.label, p.color, p.description]),
    ...plan.domains.map((d) => [`domain:${d.slug}`, '1d76db', d.description.slice(0, 100)]),
    ['epic', '5319e7', 'Domain epic grouping its tasks as sub-issues'],
  ]
  for (const [name, color, description] of labels) {
    gh('label', 'create', name, '--repo', repo, '--color', color, '--description', description, '--force')
  }
  console.log(`✓ ${labels.length} labels`)
}

function ensureMilestones(repo) {
  const existing = ghJson('api', `repos/${repo}/milestones?state=all&per_page=100`).map((m) => m.title)
  for (const p of Object.values(PHASES)) {
    if (existing.includes(p.milestone)) continue
    gh('api', `repos/${repo}/milestones`, '-f', `title=${p.milestone}`, '-f', `description=${p.description}`)
    console.log(`✓ milestone ${p.milestone}`)
  }
}

function addToProject(owner, url, values) {
  gh('project', 'item-add', String(plan.project.number), '--owner', owner, '--url', url, '--format', 'json')
  for (const [field, value] of Object.entries(values)) {
    gh('project', 'item-edit', String(plan.project.number), '--owner', owner, '--url', url, '--field', field, '--value', value)
  }
}

function ensureEpics(owner, repo) {
  for (const d of plan.domains) {
    if (d.epic) continue
    const url = gh('issue', 'create', '--repo', repo, '--title', `Epic: ${d.name}`, '--body', renderEpic(d),
      '--label', 'epic', '--label', `domain:${d.slug}`)
    d.epic = issueNumber(url)
    save()
    addToProject(owner, url, { Domain: d.name })
    console.log(`✓ epic #${d.epic} ${d.name}`)
  }
}

function createTasks(owner, repo) {
  for (const t of plan.tasks) {
    if (t.issue) continue
    const domain = plan.domains.find((d) => d.name === t.domain)
    const a = ['issue', 'create', '--repo', repo, '--title', t.title, '--body', renderTask(t),
      '--label', PHASES[t.phase].label, '--label', `domain:${domain.slug}`,
      '--milestone', PHASES[t.phase].milestone, '--parent', String(domain.epic)]
    const blockers = (t.dependsOn ?? []).map((k) => taskByKey.get(k).issue).filter(Boolean)
    if (blockers.length) a.push('--blocked-by', blockers.join(','))
    for (const login of t.assignees ?? []) a.push('--assignee', login)
    const url = gh(...a)
    t.issue = issueNumber(url)
    save()
    addToProject(owner, url, {
      Phase: PHASES[t.phase].field, Domain: t.domain, Area: AREAS[t.area], Size: SIZES[t.size],
    })
    console.log(`✓ #${t.issue} ${t.title}`)
  }
}

validate()

if (dryRun) {
  for (const t of plan.tasks) console.log(`\n${'='.repeat(80)}\n${t.key} · ${t.title}\n${'='.repeat(80)}\n${renderTask(t)}`)
  console.log(`\n${'='.repeat(80)}\nHAMBURGERS\n${'='.repeat(80)}\n${plan.hamburgers.map(renderHamburger).join('\n\n')}`)
  const count = (p) => plan.tasks.filter((t) => t.phase === p).length
  console.log(`\n✓ valid: ${plan.domains.length} domains, P1 ${count(1)} · P2 ${count(2)} · P3 ${count(3)} tasks`)
  process.exit(0)
}

checkAuth()
const repo = plan.repo ?? gh('repo', 'view', '--json', 'nameWithOwner', '-q', '.nameWithOwner')
const owner = plan.project.owner ?? repo.split('/')[0]
ensureProject(owner, repo)
ensureFields(owner)
ensureLabels(repo)
ensureMilestones(repo)
ensureEpics(owner, repo)
createTasks(owner, repo)
refreshEpics(repo)
console.log(`\n✓ done: ${plan.project.url}`)
