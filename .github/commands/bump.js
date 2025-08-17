const cp = require('child_process')
const gh = require('gh-helpers')()
const bump = require('../../tools/newVersion')
const exec = (a, o) => {
  console.log('$', a)
  cp.execSync(a, { stdio: 'inherit', ...o })
}

module.exports = async ([newVersion], helpers) => {
  try {
    bump(newVersion)
    // Delete any existing branch named 'bump'
    exec('git branch -D bump')
    exec('git checkout -b bump')
    exec('git add mc/' + newVersion)
    exec('git commit -m "Add version ' + newVersion + '"')
    exec('git push origin bump --force')
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

if (require.main === module) {
  const args = process.argv.slice(2)
  if (args.length !== 1) {
    console.error('Usage: node bump.js <newVersion>')
    process.exit(1)
  }
  module.exports(args, { reply: console.log })
}
