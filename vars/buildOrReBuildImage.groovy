// Function to build or rebuild services
def call(service, initialBuild) {
    script {
        echo "${initialBuild ? 'Building' : 'Rebuilding'} and Pushing ${service} image...."
        withDockerRegistry(credentialsId: 'docker-credentials', toolName: 'docker') {
            // Set the correct directory based on the service
            def serviceDir = (service == 'cartservice') ? "${SER_SRC_DIR}/${service}/src" : "${SER_SRC_DIR}/${service}"
            // Set image name
            IMAGE_NAME = "${DOCKER_USER}/${service}"

            // def currentVersion = sh(script: "grep \"image: ${IMAGE_NAME}:[^[:space:]]*\" ${KUBE_MANIFESTS_DIR}/${service}.yaml | awk -F ':' '{print \$3}'", returnStdout: true).trim()
            // def currentVersion = sh(script: "grep 'image: ${IMAGE_NAME}:[^[:space:]]*' ${KUBE_MANIFESTS_DIR}/${service}.yaml | awk -F ':' '{print \$3}' | awk '{gsub(/^v/, ""); print}'", returnStdout: true).trim()
            def currentVersion = sh(script: "grep \"image: ${IMAGE_NAME}:[^[:space:]]*\" ${KUBE_MANIFESTS_DIR}/${service}/${service}.yaml | awk -F ':' '{print \$3}' | awk '{gsub(/^v/, \"\"); print}'", returnStdout: true).trim()

            dir(serviceDir) {
                // Build image
                sh "docker build -t ${IMAGE_NAME} ."

                // Tag and push image
                sh "docker tag ${IMAGE_NAME} ${IMAGE_NAME}:v${initialBuild ? INITIAL_VERSION : incrementVersion(currentVersion)}"
                sh "docker push ${IMAGE_NAME}:v${initialBuild ? INITIAL_VERSION : incrementVersion(currentVersion)}"
                sh "docker push ${IMAGE_NAME}:latest"

                // Update manifest file
                sh "sed -i '' -e 's|${IMAGE_NAME}:v${currentVersion}|${IMAGE_NAME}:v${initialBuild ? INITIAL_VERSION : incrementVersion(currentVersion)}|g' ${KUBE_MANIFESTS_DIR}/${service}/${service}.yaml || true"

                // Remove the local images
                sh "docker rmi ${IMAGE_NAME}"
                sh "docker rmi ${IMAGE_NAME}:latest"
                sh "docker rmi ${IMAGE_NAME}:v${initialBuild ? INITIAL_VERSION : incrementVersion(currentVersion)}"
                // Remove dangling images
                sh 'docker image prune -f'
            }
        }
    }
}
