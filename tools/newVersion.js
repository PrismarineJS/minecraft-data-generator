const fs = require('fs')
const { join } = require('path')

function raise (message) {
  console.error(message)
  process.exit(1)
}

const [oldVersion, newVersion] = process.argv.slice(2)
if (!oldVersion || !newVersion) raise('Usage: node newVersion.js <oldVersion> <newVersion>')

const oldDir = join(__dirname, '../mc', oldVersion)
const newDir = join(__dirname, '../mc', newVersion)

if (!fs.existsSync(oldDir)) raise(`Old version directory does not exist: ${oldDir}`)
if (fs.existsSync(newDir)) raise(`New version directory already exists: ${newDir}`)

fs.cpSync(oldDir, newDir, { recursive: true })

// update the build.gradle file in the new mc version directory to replace the old version with the new version
const buildGradlePath = join(__dirname, '../mc', newVersion, 'build.gradle')
let buildGradleContent = fs.readFileSync(buildGradlePath, 'utf8')
buildGradleContent = buildGradleContent.replace(new RegExp(oldVersion, 'g'), newVersion)
fs.writeFileSync(buildGradlePath, buildGradleContent)
