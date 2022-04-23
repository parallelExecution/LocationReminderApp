package com.example.android.locationfinder.locationreminders.savereminder

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.example.android.locationfinder.BuildConfig
import com.example.android.locationfinder.R
import com.example.android.locationfinder.base.BaseFragment
import com.example.android.locationfinder.base.NavigationCommand
import com.example.android.locationfinder.databinding.FragmentSaveReminderBinding
import com.example.android.locationfinder.locationreminders.geofence.GeofenceBroadcastReceiver
import com.example.android.locationfinder.locationreminders.reminderslist.ReminderDataItem
import com.example.android.locationfinder.locationreminders.savereminder.selectreminderlocation.SelectLocationFragment
import com.example.android.locationfinder.utils.GeofencingConstants
import com.example.android.locationfinder.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "com.example.android.locationfinder.locationreminders.savereminder.action.ACTION_GEOFENCE_EVENT"
        private const val TAG = "projectlog"
        private const val REQUEST_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 0
    }

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    private var deviceLocationOn = false

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(
            requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())

        }

        binding.saveReminder.setOnClickListener { saveReminder() }
    }

    @SuppressLint("MissingPermission")
    private fun saveReminder() {

        val title = _viewModel.reminderTitle.value
        val description = _viewModel.reminderDescription.value
        val location = _viewModel.reminderSelectedLocationStr.value
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value
        val reminderDataItem =
            ReminderDataItem(title, description, location, latitude, longitude)

        if (!_viewModel.validateEnteredData(reminderDataItem)) return

        if (runningQOrLater && !backgroundOnlyLocationPermissionApproved()) {
            checkBackgroundLocationPermissionAPI29()
            return
        }

        if (!deviceLocationOn) {
            checkDeviceLocationSettings()
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(
                latitude!!,
                longitude!!,
                GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.i(TAG, getString(R.string.geofences_added))
                Log.i(TAG, "$reminderDataItem")
                _viewModel.saveReminder(reminderDataItem)
            }
            addOnFailureListener {
                Log.i(TAG, getString(R.string.geofences_not_added))
                Log.i(TAG, it.toString())
                _viewModel.showToast.value = getString(R.string.geofences_not_added)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SelectLocationFragment.REQUEST_TURN_DEVICE_LOCATION_ON) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i(SelectLocationFragment.TAG, "Location settings enabled")
                checkDeviceLocationSettings()

            } else {
                Log.i(SelectLocationFragment.TAG, "Location settings disabled")
                checkDeviceLocationSettings(false)
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
            grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED
        ) {
            Snackbar.make(
                binding.savereminderConstraintLayout,
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
        }
        else {
            binding.saveReminder.callOnClick()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            deviceLocationOn = false
            Log.i(SelectLocationFragment.TAG, "Inside OnFailure listener of location settings task")
            if (exception is ResolvableApiException && resolve) {
                try {
                    Log.i(SelectLocationFragment.TAG, "Inside OnFailure listener if and try block")
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        SelectLocationFragment.REQUEST_TURN_DEVICE_LOCATION_ON, null, 0, 0, 0, null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.i(
                        SelectLocationFragment.TAG,
                        "Inside OnFailure listener if and catch block"
                    )
                    Log.d(
                        SelectLocationFragment.TAG,
                        "Error getting location settings resolutions: ${sendEx.message}"
                    )
                }
            } else {
                Log.i(SelectLocationFragment.TAG, "Inside OnFailure listener else block")
                Snackbar.make(
                    binding.savereminderConstraintLayout,
                    R.string.location_required_error,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(android.R.string.ok) {
                        checkDeviceLocationSettings()
                    }.show()
            }
        }

        locationSettingsResponseTask.addOnSuccessListener { response ->
            Log.i(SelectLocationFragment.TAG, "Inside OnSuccess listener of location settings task")
        }

        locationSettingsResponseTask.addOnCompleteListener { response ->
            if (response.isSuccessful) {
                Log.i(
                    SelectLocationFragment.TAG,
                    "Inside OnComplete listener of location settings task"
                )
                deviceLocationOn = true
                binding.saveReminder.callOnClick()
            }
        }
    }

    @TargetApi(29)
    private fun backgroundOnlyLocationPermissionApproved(): Boolean {
        val backgroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            ACCESS_BACKGROUND_LOCATION
                        ))
        Log.i(TAG, "background location status: $backgroundLocationApproved")

        return backgroundLocationApproved
    }

    @TargetApi(29)
    private fun checkBackgroundLocationPermissionAPI29() {
        if (backgroundOnlyLocationPermissionApproved()) {
            return
        } else {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.background_location_permission_title)
                .setMessage(R.string.background_location_permission_message)
                .setPositiveButton(R.string.yes) { _, _ ->
                    // this request will take user to Application's Setting page
                    requestPermissions(
                        arrayOf(ACCESS_BACKGROUND_LOCATION),
                        REQUEST_BACKGROUND_PERMISSION_RESULT_CODE
                    )
                }
                .setNegativeButton(R.string.no) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }
}
