// Code Analysis function
def call(service) {
    script {
        // cartservice files are stored in different location relative to other services
        def serviceDir = (service == 'cartservice') ? "${SER_SRC_DIR}/${service}/src" : "${SER_SRC_DIR}/${service}"
        withSonarQubeEnv('sonar') {
            dir(serviceDir) {
                sh "$SCANNER_HOME/bin/sonar-scanner -Dsonar.projectKey=${service} -Dsonar.projectName=${service} -Dsonar.java.binaries=. -Dsonar.exclusions=**/Dockerfile"
            }
        }
    }
}