const cp = require('child_process')
const gh = require('gh-helpers')()
const bump = require('../../tools/newVersion')

module.exports = async ([newVersion], helpers) => {
  try {
    bump(newVersion)
    // Delete any existing branch named 'bump'
    cp.execSync('git branch -D bump', { stdio: 'ignore' })
    cp.execSync('git checkout -b bump')
    cp.execSync('git add mc/' + newVersion)
    cp.execSync('git commit -m "Add version ' + newVersion + '"')
    cp.execSync('git push origin bump --force')
    const pr = await gh.createPullRequest(
      'Add version ' + newVersion,
      'This automated PR adds version ' + newVersion,
      'bump',
      'main'
    )
    console.log('Pull request created:', pr.html_url)
  } catch (error) {
    console.log('Error bumping version:', error)
    return helpers.reply('Error bumping version: ' + error.message)
  }
}
