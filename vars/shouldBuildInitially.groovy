// Function to determine if a service should be built initially
def call(service, buildNumb, existingServices) {
    return (buildNumb == 1) || !existingServices.contains(service)
}