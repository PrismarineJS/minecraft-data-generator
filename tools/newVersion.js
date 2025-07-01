const fs = require('fs')
const { join } = require('path')

const manifest = require('../versions.json')

const oldVersion = manifest.at(-1)
const oldDir = join(__dirname, '../mc', oldVersion)

function bump (newVersion) {
  const newDir = join(__dirname, '../mc', newVersion)

  if (fs.existsSync(newDir)) {
    console.warn(`New version directory already exists: ${newDir}`)
    process.exit(0)
  }

  fs.cpSync(oldDir, newDir, { recursive: true })

  // update the build.gradle file in the new mc version directory to replace the old version with the new version
  const buildGradlePath = join(__dirname, '../mc', newVersion, 'build.gradle')
  let buildGradleContent = fs.readFileSync(buildGradlePath, 'utf8')
  buildGradleContent = buildGradleContent.replace(new RegExp(oldVersion, 'g'), newVersion)
  fs.writeFileSync(buildGradlePath, buildGradleContent)

  // Now update the versions.json file
  manifest.push(newVersion)
  fs.writeFileSync(join(__dirname, '../versions.json'), JSON.stringify(manifest, null, 2))
}

module.exports = bump
if (require.main === module) {
  const [newVersion] = process.argv.slice(2)
  if (!newVersion) {
    console.error('Usage: node newVersion.js <newVersion>')
    process.exit(1)
  }
  bump(newVersion)
}
