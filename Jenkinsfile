@Library("ci-library") _
pipeline {
    agent { label 'Jenkins-Agent'}

    environment {
        INITIAL_VERSION = '0.0.1'
        DOCKER_USER = 'mwanthi'
        ROOT_DIR = '/home/ubuntu'
        JOB_DIR = "${ROOT_DIR}/workspace/${env.JOB_NAME}"
        MICRO_REPO = "${ROOT_DIR}/workspace/${env.JOB_NAME}/microservices-app"
        SER_SRC_DIR = "${MICRO_REPO}/src"
        KUBE_MANIFESTS_DIR = "${JOB_DIR}/kubernetes-manifests"
        SER_TRACKING_DIR = "${ROOT_DIR}/track/${env.JOB_NAME}" // we could store this file externally (s3)
        SER_TRACKING_FILE = "${SER_TRACKING_DIR}/existingServices.txt"
        GITHUB_USER = 'D-Mwanth'
        GITHUB_USER_EMAIL = 'dmwanthi2@gmail.com'
        MICRO_REPO_URL = 'https://github.com/D-Mwanth/microservices-app.git'
        MANIFESTS_REPO_URL = 'https://github.com/D-Mwanth/kubernetes-manifests.git'
        BUILD_BRANCH = 'main'
    }

    stages {
        stage('Cleanup Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Checkout Microservices & K8s Manifests') {
            steps {
                script {
                    // Clone the microservices-app repository
                    dir('microservices-app') {
                        git branch: "${BUILD_BRANCH}", url: "${MICRO_REPO_URL}"
                    }

                    // Clone the kubernetes-manifests repository
                    dir('kubernetes-manifests') {
                        git branch: "${BUILD_BRANCH}", url: "${MANIFESTS_REPO_URL}"
                    }
                }
            }
        }

        stage('Determine Services to Build') {
            steps {
                script {
                    def servicesToBuild = determineServices(env.BUILD_NUMBER.toInteger(), MICRO_REPO)
                    echo "Services to Build: ${servicesToBuild}"
                    // Pass servicesToBuild to the environment for use in the next stage
                    env.SERVICES_TO_BUILD = servicesToBuild.join(',')
                    }
            }
        }

        stage('Build or Rebuild Services') {
            when {
                expression { return env.SERVICES_TO_BUILD != '' }
            }
            steps {
                script {
                    // Create services tracking dir and file if don't exist
                    sh "mkdir -p ${SER_TRACKING_DIR}"
                    sh "touch ${SER_TRACKING_FILE}"

                    // Set read, write, execute permissions for all users on manifest directory
                    sh 'chmod -R 777 ${KUBE_MANIFESTS_DIR}'

                    // Use build number to determine services to build (refer to determineServices function for details)
                    def servicesToBuild = env.SERVICES_TO_BUILD.split(',')
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
        
        stage("Push K8S Manifest changes to Github") {
            steps {
                script {
                    dir(KUBE_MANIFESTS_DIR) {
                        
                        // Configure Git user locally
                        sh """
                        git config --local user.name '${GITHUB_USER}'
                        git config --local user.email '${GITHUB_USER_EMAIL}'
                        """
                        
                        // Add changes
                        sh 'git add .'
                        
                        // Commit changes (will succeed or fail if there are no changes or will ensure gracefull exit)
                        sh "git commit -m 'Updated Deployment Manifest-Build ${env.BUILD_NUMBER}' || echo 'No changes to commit.'"
                
                        // Push changes to Github
                        withCredentials([gitUsernamePassword(credentialsId: 'github')]) {
                            sh "git push ${MANIFESTS_REPO_URL} ${BUILD_BRANCH}"
                        }
                    }
                }
            }
        }
    }

    // Jenkins-Slack Notification
    post {
        always {
            echo "Sending Slack Notification..."
            script {
                def notificationDetails = getNotificationDetails(currentBuild.currentResult)
                slackSend (
                    channel: "#jenkins-bot",
                    color: notificationDetails.color,  // Color for the message
                    message: "*${notificationDetails.message}:* Job *${env.JOB_NAME}* \nBuild *${env.BUILD_NUMBER}* \nMore info at: ${env.BUILD_URL}"
                )
            }
        }
    }
}
