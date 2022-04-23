package com.example.android.locationfinder.locationreminders.savereminder.selectreminderlocation


import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.example.android.locationfinder.BuildConfig
import com.example.android.locationfinder.R
import com.example.android.locationfinder.base.BaseFragment
import com.example.android.locationfinder.base.NavigationCommand
import com.example.android.locationfinder.databinding.FragmentSelectLocationBinding
import com.example.android.locationfinder.locationreminders.savereminder.SaveReminderViewModel
import com.example.android.locationfinder.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    companion object {
        const val TAG = "projectlog"
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
        private const val LOCATION_PERMISSION_INDEX = 0
    }

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onStart() {
        super.onStart()
        checkDeviceLocationSettingsAndEnableLocationLayer()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setPoiClick(map)
        setMapLongClick(map)
        setMapStyle(map)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "Location settings enabled")

            } else {
                Log.i(TAG, "Location settings disabled")
                checkDeviceLocationSettingsAndEnableLocationLayer(false)
            }
            if (!foregroundOnlyLocationPermissionApproved()) {
                requestForegroundOnlyLocationPermissions()
            } else {
                enableMyLocationLayer()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")

        if (grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED
        ) {
            Snackbar.make(
                binding.map,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            enableMyLocationLayer()
        }
    }

    private fun onLocationSelected(poi: PointOfInterest) {
        _viewModel.latitude.value = poi.latLng.latitude
        _viewModel.longitude.value = poi.latLng.longitude
        _viewModel.selectedPOI.value = poi
        _viewModel.reminderSelectedLocationStr.value = poi.name
        _viewModel.navigationCommand.value =
            NavigationCommand.Back
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker?.showInfoWindow()
            onLocationSelected(poi)
        }
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            // A Snippet is Additional text that's displayed below the title.
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)

            )
            val poi = PointOfInterest(latLng, "${getUniqueId()}", getString(R.string.dropped_pin))
            onLocationSelected(poi)
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationLayer() {
        if (!::map.isInitialized) {
            Log.i(TAG, "map not initialized")
            return
        }
        map.isMyLocationEnabled = true
        try {
            val locationResult = fusedLocationProviderClient.lastLocation
            locationResult.addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Set the map's camera position to the current location of the device.
                    val lastKnownLocation = task.result
                    if (lastKnownLocation != null) {
                        Log.d(TAG, "Current location is $lastKnownLocation")
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    lastKnownLocation.latitude,
                                    lastKnownLocation.longitude
                                ), 15f
                            )
                        )
                        _viewModel.showToast.value = getString(R.string.select_poi)
                    }
                } else {
                    Log.d(TAG, "Current location is null. Using defaults.")
                    Log.e(TAG, "Exception: %s", task.exception)
                    map.moveCamera(
                        CameraUpdateFactory
                            .newLatLngZoom(LatLng(29.607064, 74.273558), 15f)
                    )
                    map.isMyLocationEnabled = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: %s", e)

        }
    }

    private fun checkDeviceLocationSettingsAndEnableLocationLayer(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            Log.i(TAG, "Inside OnFailure listener of location settings task")
            if (exception is ResolvableApiException && resolve) {
                try {
                    Log.i(TAG, "Inside OnFailure listener if and try block")
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        REQUEST_TURN_DEVICE_LOCATION_ON, null, 0, 0, 0, null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.i(TAG, "Inside OnFailure listener if and catch block")
                    Log.d(TAG, "Error getting location settings resolutions: ${sendEx.message}")
                }
            } else {
                Log.i(TAG, "Inside OnFailure listener else block")
                Snackbar.make(
                    binding.map,
                    R.string.location_required_error,
                    Snackbar.LENGTH_LONG
                )
                    .setAction(android.R.string.ok) {
                        checkDeviceLocationSettingsAndEnableLocationLayer()
                    }.show()
            }
        }

        locationSettingsResponseTask.addOnSuccessListener { response ->
            Log.i(TAG, "Inside OnSuccess listener of location settings task")
        }

        locationSettingsResponseTask.addOnCompleteListener { response ->
            if (response.isSuccessful) {
                Log.i(TAG, "Inside OnComplete listener of location settings task")
                if (!foregroundOnlyLocationPermissionApproved()) {
                    requestForegroundOnlyLocationPermissions()
                } else {
                    enableMyLocationLayer()
                }
            }
        }
    }

    @TargetApi(29)
    private fun foregroundOnlyLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            ACCESS_FINE_LOCATION
                        ))

        return foregroundLocationApproved
    }

    @TargetApi(29)
    private fun requestForegroundOnlyLocationPermissions() {
        if (foregroundOnlyLocationPermissionApproved()) return

        val permissionsArray = arrayOf(ACCESS_FINE_LOCATION)

        val resultCode = REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE

        Log.d(TAG, "Request foreground only location permission")

        requestPermissions(permissionsArray, resultCode)
    }
}

private fun getUniqueId() = ((System.currentTimeMillis() % 10000).toInt())