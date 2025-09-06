/**
 * setupCopilot.js
 *
 * Reads versions from:
 *  - versions.json at repo root (if present), else
 *  - directories under mc/
 *
 * For each version it runs:
 *   ./gradlew :mc:<version>:runServer --stacktrace
 *
 * Uses child_process.execSync(cmd, { stdio: 'inherit' }) so logs stream to the action console.
 * Errors are caught, logged, and the loop continues. At the end the script writes
 * success_versions and failed_versions to $GITHUB_OUTPUT (if available) for downstream steps.
 *
 * To force the action to fail if any version errors, set env FAIL_ON_ERROR=true in the workflow.
 */

const cp = require('child_process')
const fs = require('fs')
const { join } = require('path')

const failOnError = process.env.FAIL_ON_ERROR === 'true'

// Save some time by skipping old versions as we are not parallelizing builds
const versions = require('../../versions.json')
const SKIP_VERSIONS = versions.slice(0, versions.indexOf('1.21.3'))

async function main () {

  if (!Array.isArray(versions) || versions.length === 0) {
    console.error('No versions found (no VERSION env, no versions.json, no mc/* directories).')
    process.exit(1)
  }

  console.log('Versions to build:', versions.join(', '))

  process.chdir(join(__dirname, '../../'))

  const successes = []
  const failures = []

  for (const v of versions) {
    if (SKIP_VERSIONS.includes(v)) {
      console.log(`Skipping old version ${v}...`)
      continue
    }
    console.log('')
    console.log('=== Building version:', v, '===')
    const cmd = `./gradlew :mc:${v}:runServer --stacktrace`
    try {
      // stream output to the runner logs
      cp.execSync(cmd, { stdio: 'inherit' })
      successes.push(v)
      console.log(`✅ ${v} succeeded`)
    } catch (err) {
      failures.push(v)
      console.error(`❌ ${v} failed (continuing to next version)`)
      // continue to next version
    }
  }

  // Write outputs for downstream jobs, if GITHUB_OUTPUT available
  const ghOut = process.env.GITHUB_OUTPUT
  if (ghOut) {
    try {
      fs.appendFileSync(ghOut, `success_versions=${JSON.stringify(successes)}\n`)
      fs.appendFileSync(ghOut, `failed_versions=${JSON.stringify(failures)}\n`)
    } catch (e) {
      console.warn('Could not write to GITHUB_OUTPUT:', e && e.message)
    }
  } else {
    console.log('GITHUB_OUTPUT not found; printing summaries to stdout instead.')
    console.log('success_versions=', JSON.stringify(successes))
    console.log('failed_versions=', JSON.stringify(failures))
  }

  console.log('')
  console.log('Summary:')
  console.log('  succeeded:', JSON.stringify(successes))
  console.log('  failed:   ', JSON.stringify(failures))

  if (failOnError && failures.length > 0) {
    console.error('FAIL_ON_ERROR is true and some builds failed — exiting with non-zero code.')
    process.exit(1)
  }

  // otherwise exit 0 so the workflow continues (Copilot can triage failures)
  process.exit(0)
}

main()
