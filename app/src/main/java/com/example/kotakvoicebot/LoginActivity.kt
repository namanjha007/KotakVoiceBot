package com.example.kotakvoicebot

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent

import android.app.ProgressDialog
import android.os.Build
import android.widget.Button
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import android.view.View
import android.view.WindowManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.AuthResult

class LoginActivity : AppCompatActivity() {

    private var mGoogleSignInClient: GoogleSignInClient? = null
    private var mAuth: FirebaseAuth? = null
    private var progressBar: ProgressDialog? = null

    override fun onStart() {
        super.onStart()
        if (mAuth != null) {
            if (mAuth!!.currentUser != null) {
                val intent = Intent(applicationContext, MainActivity::class.java)
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (Build.VERSION.SDK_INT >= 21) {
            val window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = this.resources.getColor(R.color.kotakblue)
        }

        mAuth = FirebaseAuth.getInstance()

        //Progress Bar

        //Progress Bar
        progressBar = ProgressDialog(this)
        progressBar!!.setCancelable(true) //you can cancel it by pressing back button

        progressBar!!.setMessage("Please Wait...")

        createRequest()

        findViewById<View>(R.id.googleIcon).setOnClickListener { view: View? -> signIn() }
    }

    private fun createRequest() {


        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("48714798979-gnl0tq0cos33su6lvd1av90kj1hhmlr5.apps.googleusercontent.com")
            .requestEmail()
            .build()


        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
    var someActivityResultLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        progressBar!!.dismiss() //dismisses the progress bar
        if (result.resultCode == RESULT_OK) {
            // There are no request codes
            val data = result.data
            progressBar!!.show()
            val task: Task<*> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account =
                    (task.getResult(ApiException::class.java) as GoogleSignInAccount)
                firebaseAuthWithGoogle(account)
            } catch (e: Throwable) {
                // Google Sign In failed, update UI appropriately
                // ...
                progressBar!!.dismiss()
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun signIn() {
        progressBar!!.show() //displays the progress bar
        val intent = mGoogleSignInClient!!.signInIntent
        someActivityResultLauncher.launch(intent)
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        mAuth!!.signInWithCredential(credential)
            .addOnCompleteListener(
                this
            ) { task: Task<AuthResult?> ->
                if (task.isSuccessful) {
                    progressBar!!.dismiss()
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(applicationContext, "Sorry auth failed.", Toast.LENGTH_SHORT)
                        .show()
                    progressBar!!.dismiss()
                }
            }
    }

    override fun onBackPressed() {
        startActivity(Intent(applicationContext, SplashScreen::class.java))
        super.onBackPressed()
    }
}