package weddellseal.markrecap.domain.location.data

import kotlin.math.*

data class Coordinates(
    val latitude: Double,
    val longitude: Double
) {
    /**
     * Calculate the distance between two coordinates using the Haversine formula
     * @return distance in meters
     */
    fun distanceTo(other: Coordinates): Double {
        val earthRadius = 6371000.0 // Earth's radius in meters
        
        val lat1Rad = Math.toRadians(this.latitude)
        val lat2Rad = Math.toRadians(other.latitude)
        val deltaLatRad = Math.toRadians(other.latitude - this.latitude)
        val deltaLonRad = Math.toRadians(other.longitude - this.longitude)
        
        val a = sin(deltaLatRad / 2).pow(2) + 
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLonRad / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
}