package com.visionassist.eyetest.data.clinic

import com.visionassist.eyetest.domain.model.Clinic
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object ClinicDirectory {

    private val MOCK_CLINICS = listOf(
        Clinic("1", "City Vision Care", "1 MG Road", 12.975, 77.605, 0.0, "Next slot: tomorrow 10:00"),
        Clinic("2", "BrightEye Clinic", "22 Koramangala", 12.935, 77.625, 0.0, "Next slot: today 4:00"),
        Clinic("3", "Family Eye Center", "8 Indiranagar", 12.978, 77.640, 0.0, "Next slot: Fri 2:00")
    )

    fun mockClinicsNear(lat: Double, lng: Double): List<Clinic> {
        return MOCK_CLINICS.map { c ->
            val d = haversineKm(lat, lng, c.latitude, c.longitude)
            c.copy(distanceKm = d)
        }.sortedBy { it.distanceKm }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
