package com.visionassist.eyetest.ui.screens.clinics

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visionassist.eyetest.R
import com.visionassist.eyetest.domain.model.Clinic
import com.visionassist.eyetest.ui.VisionAssistViewModelFactory
import com.visionassist.eyetest.ui.components.ClinicalCard
import com.visionassist.eyetest.ui.components.PrimaryActionButton
import com.visionassist.eyetest.ui.components.ScaffoldListContent
import com.visionassist.eyetest.ui.components.VisionAssistScaffold

@Composable
fun ClinicListScreen(
    factory: VisionAssistViewModelFactory,
    onNavigateUp: () -> Unit
) {
    val vm: ClinicListViewModel = viewModel(factory = factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hasLocationPermission = remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPermission.value = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(hasLocationPermission.value) {
        if (hasLocationPermission.value) {
            val loc = lastKnownLocation(context)
            if (loc != null) vm.loadForCoordinates(loc.latitude, loc.longitude) else vm.useDefaultLocationFallback()
        } else {
            vm.useDefaultLocationFallback()
        }
    }

    VisionAssistScaffold(
        title = stringResource(R.string.clinics_title),
        onNavigateUp = onNavigateUp
    ) { padding ->
        ScaffoldListContent(paddingValues = padding) {
            ClinicalCard {
                Text(
                    state.locationLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!hasLocationPermission.value) {
                    Spacer(Modifier.height(8.dp))
                    PrimaryActionButton(
                        text = stringResource(R.string.clinics_allow_location),
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (state.loading) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.clinics_loading),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                state.errorMessage?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    PrimaryActionButton(text = stringResource(R.string.clinics_retry), onClick = vm::retry)
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(state.clinics, key = { it.id }) { clinic ->
                    ClinicCard(clinic = clinic, onOpenMap = {
                        val uri = Uri.parse(
                            "geo:${clinic.latitude},${clinic.longitude}?q=${clinic.latitude},${clinic.longitude}(${Uri.encode(clinic.name)})"
                        )
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    })
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun lastKnownLocation(context: android.content.Context): Location? {
    val lm = context.getSystemService(LocationManager::class.java) ?: return null
    val providers = lm.getProviders(true)
    val known = providers.mapNotNull { provider ->
        runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
    }
    return known.maxByOrNull { it.time }
}

@Composable
private fun ClinicCard(clinic: Clinic, onOpenMap: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                clinic.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(clinic.address, style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.clinic_distance_km, clinic.distanceKm),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                clinic.availabilityNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Lat ${"%.6f".format(clinic.latitude)}, Lng ${"%.6f".format(clinic.longitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PrimaryActionButton(
                text = stringResource(R.string.open_in_maps),
                onClick = onOpenMap,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
