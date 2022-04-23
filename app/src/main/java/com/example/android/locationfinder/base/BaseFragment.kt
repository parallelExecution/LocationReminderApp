package com.example.android.locationfinder.base

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.example.android.locationfinder.authentication.AuthenticationActivity
import com.example.android.locationfinder.authentication.AuthenticationState
import com.example.android.locationfinder.locationreminders.reminderslist.ReminderListFragment

/**
 * Base Fragment to observe on the common LiveData objects
 */
abstract class BaseFragment : Fragment() {
    /**
     * Every fragment has to have an instance of a view model that extends from the BaseViewModel
     */
    abstract val _viewModel: BaseViewModel

    private fun navigateToAuthentication() {
        val intent = Intent(requireActivity(), AuthenticationActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onStart() {
        super.onStart()
        _viewModel.showErrorMessage.observe(this, Observer {
            Toast.makeText(activity, it, Toast.LENGTH_LONG).show()
        })
        _viewModel.showToast.observe(this, Observer {
            Toast.makeText(activity, it, Toast.LENGTH_LONG).show()
        })
        _viewModel.showSnackBar.observe(this, Observer {
            Snackbar.make(this.requireView(), it, Snackbar.LENGTH_LONG).show()
        })
        _viewModel.showSnackBarInt.observe(this, Observer {
            Snackbar.make(this.requireView(), getString(it), Snackbar.LENGTH_LONG).show()
        })

        _viewModel.navigationCommand.observe(this, Observer { command ->
            when (command) {
                is NavigationCommand.To -> findNavController().navigate(command.directions)
                is NavigationCommand.Back -> findNavController().popBackStack()
                is NavigationCommand.BackTo -> findNavController().popBackStack(
                    command.destinationId,
                    false
                )
            }
        })

        _viewModel.authenticationState.observe(viewLifecycleOwner, Observer { authState ->
            when (authState) {
                AuthenticationState.AUTHENTICATED -> {
                    Log.i(
                        "projectlog",
                        "In ${javaClass.canonicalName}, user ${FirebaseAuth.getInstance().currentUser?.displayName}!"
                    )
                }
                AuthenticationState.UNAUTHENTICATED -> {
                    Log.i(
                        "projectlog",
                        "In ${javaClass.canonicalName}, user ${FirebaseAuth.getInstance().currentUser?.displayName}!"
                    )
                    navigateToAuthentication()
                }
            }
        })
    }
}