const cp = require('child_process')
const gh = require('gh-helpers')()
const bump = require('../../tools/newVersion')
const exec = (a, o) => {
  console.log('$', a)
  cp.execSync(a, { stdio: 'inherit', ...o })
}

const generateBody = (newVersion, issueNo, prNo) => `
This automated PR sets up the relevant boilerplate for Minecraft version ${newVersion}.

Links:
- Issue: ${issueNo ? `https://github.com/PrismarineJS/minecraft-data/issues/${issueNo}` : 'N/A'}
- PR: ${prNo ? `https://github.com/PrismarineJS/minecraft-data/pull/${prNo}` : 'N/A'}

If the PR is passing, it will automatically send the generated artifacts to PrismarineJS/minecraft-data${prNo ? `#${prNo}` : ''}.

If the PR is **not** passing:
* You can help contribute to this PR by opening a PR against this <code>bump</code> branch instead of <code>main</code>.
`

module.exports = async ([newVersion], helpers) => {
  const existingPr = await gh.findPullRequest({ titleIncludes: '🎈', status: 'open' })
  if (existingPr) {
    console.warn('There is already an open PR; cannot create a new one until that one is closed.', existingPr)
    await gh.comment(existingPr.number, `Cannot open new PR for version '${newVersion}' as this one is already open. Maintainers: Once this is merged, please manually re-run the update workflow for the new version (${newVersion}).`)
    return
  }
  try {
    bump(newVersion)
    try { exec('git branch -D bump') } catch (e) { console.log('No existing branch to delete; ok.') }
    exec('git checkout -b bump')
    exec('git config user.name "github-actions[bot]"')
    exec('git config user.email "41898282+github-actions[bot]@users.noreply.github.com"')
    exec('git add mc versions.json')
    exec('git commit -m "Add version ' + newVersion + '"')
    exec('git push origin bump --force')
    const pr = await gh.createPullRequest(
      '🎈 Add version ' + newVersion, // special marker
      generateBody(newVersion, process.env.MCDATA_ISSUE_NO, process.env.MCDATA_PULL_NO),
      'bump',
      'main'
    )
    console.log('Pull request created:', pr.html_url)
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
