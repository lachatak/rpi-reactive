/**
 * Semantic Versioning Script for Jenkins
 *
 * For this to work commit messages must contain one of the characters [!, +, =]
 *
 * ! is a breaking change and will increment the major version number.
 *      e.g. 1.1.256 will become 2.0.0
 *
 * + is a backward compatible new feature and will increment the minor version number.
 *      e.g. 1.2.24 will become 1.3.0
 *
 * = is a bug fix and will increment the patch number.
 *      e.g. 1.3.1 will become 1.3.2
 *
 */

import hudson.model.*
def env = build.getEnvironment(listener)
//println env.keySet()
pathToJob = "${env.get('JENKINS_HOME')}/jobs/${env.get('JOB_NAME')}/"
pathToWorkspace = pathToJob + "workspace/"
pathToLastSuccessful = pathToJob + "lastSuccessful/build.xml"
// Read the last successfully built commit hash (only the develop branch)
lastBuildText = new File(pathToLastSuccessful).text
root = new XmlParser().parseText(lastBuildText)
buildData = root.'**'.find { it.name() == 'buildsByBranchName' }
developBuild = buildData.children().find { it.string.text().contains('refs/remotes/origin/develop') }
lastSuccessfulDevelopSHA1 = developBuild.'**'.sha1.text()

// Find the commits between the last successful build and the current HEAD
logCommand = "git --git-dir ${pathToWorkspace + ".git"} log --pretty=\"%H%n%s\" ${lastSuccessfulDevelopSHA1}..HEAD".execute()
def sout = new StringBuffer(), serr = new StringBuffer()
logCommand.consumeProcessOutput(sout, serr)
logCommand.waitForOrKill(2000)
mostRecentCommitLine = sout.readLines()[0]
mostRecentCommit = (null != mostRecentCommitLine) ? mostRecentCommitLine.replaceAll("\"", '') : lastSuccessfulDevelopSHA1
commitMessages = sout.toString()

println "commit messages: ${commitMessages}"

// Find the last tag
lastTagCommand = "git --git-dir ${pathToWorkspace + ".git"} describe --abbrev=0 --tags".execute()
sout = new StringBuffer()
serr = new StringBuffer()
lastTagCommand.consumeProcessOutput(sout, serr)
lastTagCommand.waitForOrKill(2000)
lastTag = sout.readLines()[0]


if (null == lastTag || lastTag.contains("fatal"))
    lastTag = "1.0.0"

println "lastTag: $lastTag"
versionDigits = lastTag.split('\\.')


if(!commitMessages.readLines().isEmpty()) {
// Find the symbols from the commit messages
    symbols = commitMessages.toString().findAll { it == '!' || it == '=' || it == '+' }

    println "extracted command symbols: $symbols"

// Semantically version based on the commit message symbol.
    if (symbols.contains('!')) {
        versionDigits[0] = versionDigits[0].toInteger() + 1
        versionDigits[1] = 0
        versionDigits[2] = 0
    } else if (symbols.contains('+')) {
        versionDigits[1] = versionDigits[1].toInteger() + 1
        versionDigits[2] = 0
    } else if (symbols.contains('=')) {
        versionDigits[2] = versionDigits[2].toInteger() + 1
    } else {
        versionDigits[1] = versionDigits[1].toInteger() + 1
        versionDigits[2] = 0
    }
    nextVersion = versionDigits.join(".")
    println "derived version number: $nextVersion"
} else {
    nextVersion = lastTag
    println "Skipping Automated Versioning, there is no change. Keeping version $lastTag"
}

println "setting nextVersion to $nextVersion"

branchName = env.get('BRANCH_NAME')
if (branchName == 'develop'){
    buildName = "$branchName-$nextVersion"
} else {
    buildName = "$branchName-${mostRecentCommit.substring(0, 5)}"
}

new File(pathToWorkspace,'PIPELINE_PARAMS').withWriter('utf-8') { writer ->
    writer.writeLine "NEXT_TAG=$nextVersion"
    writer.writeLine "NEXT_VERSION=$nextVersion"
    writer.writeLine "BUILD_NAME=$buildName"
}

props = new Properties()
propsFile = new File(pathToWorkspace, 'gradle.properties')
props.load(propsFile.newDataInputStream())
props.setProperty('BUILD_ID', buildName)
props.store(propsFile.newWriter(), null)

def build = Thread.currentThread().executable
build.displayName = buildName
