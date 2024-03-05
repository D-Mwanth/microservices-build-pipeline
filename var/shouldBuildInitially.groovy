// Function to determine if a service should be built initially
def shouldBuildInitially(service, buildNumb, existingServices) {
    return (buildNumb == 1) || !existingServices.contains(service)
}