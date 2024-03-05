def call(buildNumb) {
    if (buildNumb == 1) {
        // Get all services in in the microserv-app (assumption: it's the initial build)
        def serviceList = sh(script: 'git ls-tree --name-only -r HEAD | grep "^src/" | grep -v "/.gitignore" | cut -d "/" -f 2 | sort | uniq | tr "\\n" " "', returnStdout: true).trim()
        return serviceList.split(' ')
    } else {
        // Get modified services only or new services (services that were not there in the previous commit).
        return sh(script: 'git diff --name-only HEAD~1 | grep "^src" | grep -v "/.gitignore" | cut -d "/" -f 2 | sort | uniq | tr "\\n" " "', returnStdout: true).trim().split(' ')
    }
}