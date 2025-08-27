const gh = require('gh-helpers')()

module.exports = async ([mergedArtifactURL], helpers) => {
  // Have minecraft-data handle this successful update
  const dispatchPayload = {
    owner: 'PrismarineJS',
    repo: 'minecraft-data',
    workflow: 'handle-mcpc-generator.yml',
    branch: 'master',
    inputs: {
      versions: JSON.stringify(require('../../versions.json')),
      mergedArtifactURL: mergedArtifactURL,
      prNumber: process.env.PR_NUMBER
    }
  }
  console.log('Sending workflow dispatch', dispatchPayload)
  await gh.sendWorkflowDispatch(dispatchPayload)
}

if (require.main === module) {
  const merged = process.env.ARTIFACT_URL
  console.log('Handling merged artifacts @', merged)
  module.exports([merged], { reply: console.log })
}
