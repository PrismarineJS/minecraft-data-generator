const cp = require('child_process')
const gh = require('gh-helpers')()
const bump = require('../../tools/newVersion')
const exec = (a, o) => {
  console.log('$', a)
  cp.execSync(a, { stdio: 'inherit', ...o })
}

// Each version gets its own branch, so a new version's PR can be opened while an older one is still open
const getBranch = (version) => 'bump_' + version.replace(/[^a-zA-Z0-9]/g, '_').toLowerCase()

const generateBody = (newVersion, branch, issueNo, prNo) => `
This automated PR sets up the relevant boilerplate for Minecraft version ${newVersion}.

Links:
- Issue: ${issueNo ? `https://github.com/PrismarineJS/minecraft-data/issues/${issueNo}` : 'N/A'}
- PR: ${prNo ? `https://github.com/PrismarineJS/minecraft-data/pull/${prNo}` : 'N/A'}

If the PR is passing, it will automatically send the generated artifacts to PrismarineJS/minecraft-data${prNo ? `#${prNo}` : ''}.

If the PR is **not** passing:
* You can help contribute to this PR by opening a PR against this <code>${branch}</code> branch instead of <code>main</code>.
`

module.exports = async ([newVersion], helpers) => {
  const branch = getBranch(newVersion)
  // Don't force-push over a branch whose PR is still open, it may have fixes from contributors
  const openPrs = await gh.findPullRequests({ titleIncludes: '🎈', status: 'open', author: null })
  for (const { number } of openPrs) {
    const pr = await gh.getPullRequest(number)
    if (pr.headBranch === branch && pr.headRepo === pr.targetRepo) {
      console.warn(`There is already an open PR for version '${newVersion}' (#${number}); leaving the ${branch} branch alone.`)
      return
    }
  }
  try {
    bump(newVersion)
    try { exec(`git branch -D ${branch}`) } catch (e) { console.log('No existing branch to delete; ok.') }
    exec(`git checkout -b ${branch}`)
    exec('git config user.name "github-actions[bot]"')
    exec('git config user.email "41898282+github-actions[bot]@users.noreply.github.com"')
    exec('git add mc versions.json')
    exec('git commit -m "Add version ' + newVersion + '"')
    exec(`git push origin ${branch} --force`)
    const pr = await gh.createPullRequest(
      '🎈 Add version ' + newVersion, // special marker
      generateBody(newVersion, branch, process.env.MCDATA_ISSUE_NO, process.env.MCDATA_PULL_NO),
      branch,
      'main'
    )
    console.log('Pull request created:', pr.url)
  } catch (error) {
    console.log('Error bumping version:', error)
    return helpers.reply('Error bumping version: ' + error.message)
  }
}

if (require.main === module) {
  const args = process.argv.slice(2)
  if (args.length !== 1) {
    console.error('Usage: node bump.js <newVersion>')
    process.exit(1)
  }
  module.exports(args, { reply: console.log })
}
