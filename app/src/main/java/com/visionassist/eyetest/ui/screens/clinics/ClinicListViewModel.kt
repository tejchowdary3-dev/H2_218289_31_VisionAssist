package com.visionassist.eyetest.ui.screens.clinics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visionassist.eyetest.data.repository.VisionAssistRepository
import com.visionassist.eyetest.domain.model.Clinic
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ClinicListUiState(
    val clinics: List<Clinic>,
    val locationLabel: String,
    val loading: Boolean = false,
    val errorMessage: String? = null
)

class ClinicListViewModel(
    private val repository: VisionAssistRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(
        ClinicListUiState(
            clinics = emptyList(),
            locationLabel = "Waiting for your location permission…",
            loading = true
        )
    )
    val uiState: StateFlow<ClinicListUiState> = _ui.asStateFlow()

    private var lastLat = DEFAULT_LAT
    private var lastLng = DEFAULT_LNG

    fun loadForCoordinates(lat: Double, lng: Double) {
        lastLat = lat
        lastLng = lng
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, errorMessage = null)
            val result = repository.nearbyHospitals(lat, lng)
            result.onSuccess { hospitals ->
                _ui.value = _ui.value.copy(
                    clinics = hospitals,
                    locationLabel = "Nearby hospitals around ${"%.5f".format(lat)}, ${"%.5f".format(lng)}",
                    loading = false,
                    errorMessage = null
                )
            }.onFailure { error ->
                _ui.value = _ui.value.copy(
                    clinics = repository.mockClinicsNear(lat, lng),
                    locationLabel = "Could not fetch online data, showing local fallback around ${"%.5f".format(lat)}, ${"%.5f".format(lng)}",
                    loading = false,
                    errorMessage = error.message ?: "Could not load hospital locations"
                )
            }
        }
    }

    fun retry() = loadForCoordinates(lastLat, lastLng)

    fun useDefaultLocationFallback() = loadForCoordinates(DEFAULT_LAT, DEFAULT_LNG)

    companion object {
        private const val DEFAULT_LAT = 12.9716
        private const val DEFAULT_LNG = 77.5946
    }
}
