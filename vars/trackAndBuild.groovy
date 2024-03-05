// Function to track and build a new service
def call(service) {
    echo "Tracking and building ${service} service..."
    sh "echo '${service}' >> ${SER_TRACKING_FILE}"
    buildOrReBuildImage(service, true)
}