sealed class Route(val route: String) {
    object Stepper : Route("stepper")
    object Map : Route("map/{stationName}/{latitude}/{longitude}/{distance}") {
        fun createRoute(
            stationName: String,
            latitude: Double,
            longitude: Double,
            distance: Int
        ) = "map/$stationName/$latitude/$longitude/$distance"
    }
}