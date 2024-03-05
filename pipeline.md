````groovy
@Library("shared-library") _
pipeline {
agent any
    environment {
        SCANNER_HOME = tool 'sonar-scanner'
        INITIAL_VERSION = '1.0.0'
        DOCKER_USER = 'mwanthi'
        ROOT_DIR = '/var/lib/jenkins'
        DIR = "${ROOT_DIR}/workspace/${env.JOB_NAME}"
        SER_SRC_DIR = "${DIR}/src"
        SER_TRACKING_DIR = "${ROOT_DIR}/track/${env.JOB_NAME}"
        SER_TRACKING_FILE="${SER_TRACKING_DIR}/existingServices.txt"
        KUBE_MANIFESTS_DIR = "${DIR}/kubernetes-manifests"
        GITHUB_REPO = 'https://github.com/D-Mwanth/microservices-demo.git'
        BUILD_BRANCH = 'main'
    }

    // step to clean workspace
    stages{
        stage('Cleanup Workspace') {
                steps {
                cleanWs()
                }
        }

        // step to clone Git Repostory
        stage('Clone Project Repostory') {
            steps {
                script {
                    // clone code from the main branch, we dont need credentials since it's a public repo
                    git branch: "${BUILD_BRANCH}", url: "${GITHUB_REPO}"
                }
            }
        }

        // Code Analysis
        stage('Code Analysis: SonarQube') {
            steps {
                script {
                    // Use BUIL_NUMBER to determine services to anlyise (analysed in parallel)
                    def servicesToAnalyse = determineServices(env.BUILD_NUMBER.toInteger())
                    // Execute code analysis function in paralled (since services are independent)
                    parallel servicesToAnalyse.collectEntries {
                        ["${it}" : {
                            script {
                                codeAnalysis(it)
                            }
                        }]
                    }
                }
            }
        }

        // Build Services and Push Artifacts to Registry
        stage('Build or Rebuild Image') {
            steps {
                script {
                    // Create services tracking dir and file if dont exist
                    sh "mkdir -p ${SER_TRACKING_DIR}"
                    sh "touch ${SER_TRACKING_FILE}"

                    // Use build number to determine services to build (refer to determineServices function for details)
                    def servicesToBuild = determineServices(env.BUILD_NUMBER.toInteger())
                    // Execute build or rebuild logic in parallel across all available agents
                    def existingServices = script {
                        return readFile("${SER_TRACKING_FILE}").trim()
                    }
                    echo "${servicesToBuild}"
                    servicesToBuild.each { service ->
                        def initialBuild = shouldBuildInitially(service, env.BUILD_NUMBER.toInteger(), existingServices)
                        if (initialBuild) {
                            trackAndBuild(service)
                        } else {
                            buildOrReBuildImage(service, false)
                        }
                    }
                }
            }
        }
        // Push changes to Github

        //  Send slack message
    }
}
```