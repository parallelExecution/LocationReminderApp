package com.example.android.locationfinder.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.android.locationfinder.R
import com.example.android.locationfinder.locationreminders.RemindersActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth


/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    companion object {
        const val TAG = "projectlog"
    }

    private val signInLauncher =
        registerForActivityResult(FirebaseAuthUIActivityResultContract()) { res ->
            this.onSignInResult(res)
        }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            Log.i(TAG, "Successfully signed in")
            val user = FirebaseAuth.getInstance().currentUser
            navigateToReminderActivity()
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
            response?.error?.message?.let { Log.i(TAG, it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        val loginBtn = findViewById<Button>(R.id.login_btn)
        loginBtn.setOnClickListener {
            launchSignInFlow()
        }
    }

    override fun onStart() {
        super.onStart()
        if (FirebaseAuth.getInstance().currentUser != null) {
            Log.i(TAG, "already signed in")
            Log.i(
                TAG,
                "In Authentication activity, user ${FirebaseAuth.getInstance().currentUser?.displayName}!"
            )
            navigateToReminderActivity()
        } else {
            Log.i(TAG, "not signed in")
        }
    }

    private fun launchSignInFlow() {
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build())

        // Create and launch sign-in intent
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setTheme(R.style.GreenTheme)
            .build()

        signInLauncher.launch(signInIntent)
    }

    private fun navigateToReminderActivity() {
        val intent = Intent(this, RemindersActivity::class.java)
        startActivity(intent)
        this.finish()
    }
}
