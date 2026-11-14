const { test } = require('node:test')
const assert = require('node:assert/strict')
const cp = require('child_process')
const fs = require('fs')
const os = require('os')
const path = require('path')

test('a successor inherits main merged after checkout, using a fresh tool and manifest', t => {
  const temp = fs.mkdtempSync(path.join(os.tmpdir(), 'bump-race-'))
  t.after(() => fs.rmSync(temp, { recursive: true, force: true }))
  const remote = path.join(temp, 'remote.git')
  const maintainer = path.join(temp, 'maintainer')
  const runner = path.join(temp, 'runner')
  const git = (cwd, ...args) => cp.execFileSync('git', args, { cwd, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim()
  git(temp, 'init', '--bare', '--initial-branch=main', remote)
  git(temp, 'clone', remote, maintainer)
  git(maintainer, 'config', 'user.name', 'Queue Test')
  git(maintainer, 'config', 'user.email', 'queue@example.invalid')
  fs.mkdirSync(path.join(maintainer, 'tools'))
  fs.copyFileSync(path.join(__dirname, '../../tools/newVersion.js'), path.join(maintainer, 'tools/newVersion.js'))
  const writeVersion = (version, implementation) => {
    const dir = path.join(maintainer, 'mc', version)
    fs.mkdirSync(dir, { recursive: true })
    fs.writeFileSync(path.join(dir, 'build.gradle'), `version = '${version}'\n`)
    fs.writeFileSync(path.join(dir, 'Generator.java'), implementation)
  }
  writeVersion('26.2', 'old implementation')
  fs.writeFileSync(path.join(maintainer, 'versions.json'), JSON.stringify(['26.2']))
  git(maintainer, 'add', '.')
  git(maintainer, 'commit', '-m', 'Support 26.2')
  git(maintainer, 'push', 'origin', 'main')
  git(temp, 'clone', remote, runner)
  git(runner, 'branch', 'bump')
  const oldHead = git(runner, 'rev-parse', 'HEAD')

  // The workflow has checked out main; the predecessor now merges upstream.
  writeVersion('26.3', 'merged predecessor fixes')
  fs.writeFileSync(path.join(maintainer, 'versions.json'), JSON.stringify(['26.2', '26.3']))
  const tool = path.join(maintainer, 'tools/newVersion.js')
  fs.appendFileSync(tool, '\nrequire("fs").writeFileSync("mc/" + process.argv[2] + "/fresh-tool.txt", "new main tool")\n')
  git(maintainer, 'add', '.')
  git(maintainer, 'commit', '-m', 'Merge 26.3 and update scaffold tool')
  git(maintainer, 'push', 'origin', 'main')
  const mergedHead = git(maintainer, 'rev-parse', 'HEAD')

  // Run the actual queue and Git adapter in the stale checkout. GitHub writes
  // are simulated; the scaffold, commit and push target the real local remote.
  const script = `
    const assert = require('node:assert/strict')
    const { prepareBumpBranch, createBumpCommit } = require(process.argv[1])
    const { runQueue, writeState, initialState } = require(process.argv[2])
    const supported = require('./versions.json')
    require('./tools/newVersion') // Deliberately cache the old manifest/tool.
    const pr = { number: 1, state: 'closed', body: writeState('', initialState('26.3', [{ version: '26.4' }])) }
    runQueue({ supported, api: {
      list: async () => [pr],
      get: async () => pr,
      update: async (number, body) => { pr.body = body },
      prepare: prepareBumpBranch,
      create: async (next, state) => {
        assert.equal(next.version, '26.4')
        assert.equal(state.predecessor, 1)
        createBumpCommit(next.version)
        return { number: 2 }
      }
    }}).catch(error => { console.error(error); process.exitCode = 1 })
  `
  cp.execFileSync(process.execPath, ['-e', script, path.join(__dirname, 'bump.js'), path.join(__dirname, 'bump-state.js')], {
    cwd: runner, stdio: ['ignore', 'pipe', 'pipe']
  })
  assert.equal(git(runner, 'branch', '--show-current'), 'bump-next')
  assert.equal(git(runner, 'rev-parse', 'bump'), oldHead)
  assert.equal(git(remote, 'rev-parse', 'refs/heads/bump^'), mergedHead)
  assert.deepEqual(JSON.parse(git(remote, 'show', 'bump:versions.json')), ['26.2', '26.3', '26.4'])
  assert.equal(git(remote, 'show', 'bump:mc/26.4/Generator.java'), 'merged predecessor fixes')
  assert.equal(git(remote, 'show', 'bump:mc/26.4/fresh-tool.txt'), 'new main tool')
  assert.equal(git(remote, 'show', 'bump:mc/26.4/build.gradle'), "version = '26.4'")
})
