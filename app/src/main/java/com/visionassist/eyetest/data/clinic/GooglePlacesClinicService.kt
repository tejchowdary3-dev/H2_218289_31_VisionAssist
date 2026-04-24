package com.visionassist.eyetest.data.clinic

import com.visionassist.eyetest.domain.model.Clinic
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class GooglePlacesClinicService(
    private val httpClient: OkHttpClient = OkHttpClient()
) {

    suspend fun fetchNearbyHospitals(
        apiKey: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 12_000
    ): Result<List<Clinic>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("location", "$latitude,$longitude")
                .addQueryParameter("radius", radiusMeters.toString())
                .addQueryParameter("type", "hospital")
                .addQueryParameter("keyword", "eye hospital")
                .addQueryParameter("key", apiKey)
                .build()

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val bodyString = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Google Places HTTP ${response.code}")
                }
                response.body?.string().orEmpty()
            }
            val json = JSONObject(bodyString)
            val status = json.optString("status")
            if (status != "OK" && status != "ZERO_RESULTS") {
                val error = json.optString("error_message").ifBlank { "Google Places status: $status" }
                throw IllegalStateException(error)
            }

            val results = json.optJSONArray("results") ?: return@runCatching emptyList()
            buildList {
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val geometry = item.optJSONObject("geometry")
                    val location = geometry?.optJSONObject("location")
                    val lat = location?.optDouble("lat", Double.NaN) ?: Double.NaN
                    val lng = location?.optDouble("lng", Double.NaN) ?: Double.NaN
                    if (!lat.isFinite() || !lng.isFinite()) continue

                    val rating = item.optDouble("rating", Double.NaN)
                    val ratingCount = item.optInt("user_ratings_total", 0)
                    val openNow = item.optJSONObject("opening_hours")?.optBoolean("open_now")
                    val distance = haversineKm(latitude, longitude, lat, lng)

                    add(
                        Clinic(
                            id = item.optString("place_id", "google_$i"),
                            name = item.optString("name", "Eye Hospital"),
                            address = item.optString("vicinity", item.optString("formatted_address", "Address unavailable")),
                            latitude = lat,
                            longitude = lng,
                            distanceKm = distance,
                            availabilityNote = buildAvailabilityNote(rating, ratingCount, openNow)
                        )
                    )
                }
            }.sortedBy { it.distanceKm }
        }
    }

    private fun buildAvailabilityNote(rating: Double, ratingCount: Int, openNow: Boolean?): String {
        val status = when (openNow) {
            true -> "Open now"
            false -> "Closed now"
            null -> "Hours not listed"
        }
        return if (rating.isFinite()) {
            "Google rating %.1f (%d) • %s".format(rating, ratingCount, status)
        } else {
            status
        }
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
