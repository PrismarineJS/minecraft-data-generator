const cp = require('child_process')
const fs = require('fs')
const { runQueue, writeState } = require('./bump-state')

const git = (...args) => {
  console.log('$ git', ...args)
  cp.execFileSync('git', args, { stdio: 'inherit' })
}

const readSupported = () => JSON.parse(fs.readFileSync('versions.json', 'utf8'))

function prepareBumpBranch () {
  git('fetch', 'origin', '+refs/heads/main:refs/remotes/origin/main')
  git('checkout', '-B', 'bump-next', 'refs/remotes/origin/main')
  return readSupported()
}

function createBumpCommit (version) {
  // Run the freshly checked-out tool in a new process, without cached modules
  // or a versions.json loaded before the predecessor merged.
  cp.execFileSync(process.execPath, ['tools/newVersion.js', version], { stdio: 'inherit' })
  git('config', 'user.name', 'github-actions[bot]')
  git('config', 'user.email', '41898282+github-actions[bot]@users.noreply.github.com')
  git('add', 'mc', 'versions.json')
  git('commit', '-m', `Add version ${version}`)
  git('push', 'origin', 'HEAD:refs/heads/bump', '--force')
}

const generateBody = ({ version, issue, pr }) => `
This automated PR sets up the relevant boilerplate for Minecraft version ${version}.

Links:
- Issue: ${issue ? `https://github.com/PrismarineJS/minecraft-data/issues/${issue}` : 'N/A'}
- PR: ${pr ? `https://github.com/PrismarineJS/minecraft-data/pull/${pr}` : 'N/A'}

If the PR is passing, it will automatically send the generated artifacts to PrismarineJS/minecraft-data${pr ? `#${pr}` : ''}.

If the PR is **not** passing:
* You can help contribute to this PR by opening a PR against this <code>bump</code> branch instead of <code>main</code>.
`

function validateRequest (request) {
  if (!/^[a-zA-Z0-9][a-zA-Z0-9._-]*$/.test(request.version)) throw new Error('Invalid Minecraft version')
  for (const field of ['pr', 'issue']) {
    if (request[field] && !/^\d+$/.test(request[field])) throw new Error(`Invalid minecraft-data ${field} number`)
  }
}

function isBumpBranch (pr, repository) {
  return pr.headBranch === 'bump' && pr.headRepo === repository && pr.targetBranch === 'main'
}

function isBumpPull (pr, repository) {
  return isBumpBranch(pr, repository) && pr.title.startsWith('🎈')
}

module.exports = async ([newVersion], helpers) => {
  const gh = require('gh-helpers')()
  const repository = process.env.GITHUB_REPOSITORY
  if (!repository) throw new Error('GITHUB_REPOSITORY is required')
  const listBumpPulls = async () => (await gh.listPullRequests({
    state: 'all', head: `${repository.split('/')[0]}:bump`, base: 'main'
  })).filter(pr => isBumpBranch(pr, repository))
  const request = newVersion
    ? { version: newVersion, pr: process.env.MCDATA_PULL_NO || '', issue: process.env.MCDATA_ISSUE_NO || '' }
    : null
  if (request) validateRequest(request)

  try {
    await runQueue({
      supported: readSupported(),
      request,
      api: {
        list: async () => (await listBumpPulls()).filter(pr => isBumpPull(pr, repository)),
        get: number => gh.getPullRequest(number),
        update: (number, body) => gh.updatePull(number, { body }),
        prepare: prepareBumpBranch,
        create: async (next, state) => {
          validateRequest(next)
          // Never reset a branch that still has an open PR with contributor work.
          if ((await listBumpPulls()).some(pr => pr.state === 'open')) throw new Error('A bump PR is already open')
          createBumpCommit(next.version)
          const pr = await gh.createPullRequest(`🎈 Add version ${next.version}`, writeState(generateBody(next), state), 'bump', 'main')
          console.log('Pull request created:', pr.url)
          return pr
        }
      }
    })
  } catch (error) {
    await helpers.reply('Error bumping version: ' + error.message)
    throw error
  }
}

module.exports.isBumpPull = isBumpPull
module.exports.prepareBumpBranch = prepareBumpBranch
module.exports.createBumpCommit = createBumpCommit

if (require.main === module) {
  const args = process.argv.slice(2)
  if (args.length === 1) {
    module.exports([args[0] === '--advance' ? undefined : args[0]], { reply: console.error }).catch(error => {
      console.error(error)
      process.exitCode = 1
    })
  } else {
    console.error('Usage: node bump.js <newVersion> | --advance')
    process.exitCode = 1
  }
}
