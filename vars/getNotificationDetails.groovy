def call(result) {
    def detailsMap = [
        'FAILURE': [color: 'danger', message: '❗ FAILURE'],
        'SUCCESS': [color: '#11a611', message: '✅ SUCCESS']
    ]

    // Build status color and messages
    return detailsMap[result]
}
