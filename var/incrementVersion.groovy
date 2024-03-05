// Function to Upgrade Image version
def call(version) {
    // Split the version number into parts
    def versionParts = version.tokenize('.').collect { it.toInteger() }

    // Increment the last part
    versionParts[-1] += 1

    // Carry over if needed
    for (int i = versionParts.size() - 1; i > 0; i--) {
        if (versionParts[i] > 9) {
            versionParts[i] = 0
            versionParts[i - 1] += 1
        }
    }

    // Join the parts back into a string
    def newVersion = versionParts.join('.')
    return newVersion
}