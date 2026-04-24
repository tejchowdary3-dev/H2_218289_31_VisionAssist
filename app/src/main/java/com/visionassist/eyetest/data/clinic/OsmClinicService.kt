package com.visionassist.eyetest.data.clinic

import com.visionassist.eyetest.domain.model.Clinic
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class OsmClinicService(
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    suspend fun fetchNearbyEyeHospitals(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 10000
    ): Result<List<Clinic>> = withContext(Dispatchers.IO) {
        runCatching {
            val query = """
                [out:json][timeout:25];
                (
                  node(around:$radiusMeters,$latitude,$longitude)["amenity"="hospital"];
                  way(around:$radiusMeters,$latitude,$longitude)["amenity"="hospital"];
                  relation(around:$radiusMeters,$latitude,$longitude)["amenity"="hospital"];
                );
                out center tags;
            """.trimIndent()

            val request = Request.Builder()
                .url("https://overpass-api.de/api/interpreter")
                .post(query.toRequestBody("text/plain".toMediaType()))
                .header("Accept", "application/json")
                .build()

            val body = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("OSM request failed: HTTP ${response.code}")
                }
                response.body?.string().orEmpty()
            }

            val elements = JSONObject(body).optJSONArray("elements") ?: return@runCatching emptyList()
            buildList {
                for (i in 0 until elements.length()) {
                    val item = elements.optJSONObject(i) ?: continue
                    val lat = item.optDouble("lat", Double.NaN).takeIf { it.isFinite() }
                        ?: item.optJSONObject("center")?.optDouble("lat", Double.NaN)
                    val lng = item.optDouble("lon", Double.NaN).takeIf { it.isFinite() }
                        ?: item.optJSONObject("center")?.optDouble("lon", Double.NaN)
                    if (lat == null || lng == null || !lat.isFinite() || !lng.isFinite()) continue

                    val tags = item.optJSONObject("tags")
                    val name = tags?.optString("name").orEmpty().ifBlank { "Hospital" }
                    val address = buildAddress(tags)
                    val note = tags?.optString("opening_hours").orEmpty()
                        .ifBlank { "Data source: OpenStreetMap" }

                    add(
                        Clinic(
                            id = item.opt("id")?.toString() ?: "osm_$i",
                            name = name,
                            address = address,
                            latitude = lat,
                            longitude = lng,
                            distanceKm = haversineKm(latitude, longitude, lat, lng),
                            availabilityNote = note
                        )
                    )
                }
            }.sortedBy { it.distanceKm }
        }
    }

    private fun buildAddress(tags: JSONObject?): String {
        if (tags == null) return "Address unavailable"
        val street = tags.optString("addr:street")
        val houseNo = tags.optString("addr:housenumber")
        val city = tags.optString("addr:city")
        val area = tags.optString("addr:suburb")
        val composed = listOf(houseNo, street, area, city).filter { it.isNotBlank() }.joinToString(", ")
        return composed.ifBlank { "Address unavailable" }
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
