package com.example.kotakvoicebot

import android.app.ProgressDialog
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn

class BalanceActivity : AppCompatActivity() {

    private var progressBar: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balance)

        //Progress Bar
        progressBar = ProgressDialog(this);
        progressBar?.setCancelable(true);//you can cancel it by pressing back button
        progressBar?.setMessage("Please Wait...");
        progressBar?.show();

        val profileImage = findViewById<ImageView>(R.id.user)
        val signInAccount = GoogleSignIn.getLastSignedInAccount(this)
        if (signInAccount != null) {
            Glide.with(this).load(signInAccount.photoUrl).placeholder(R.drawable.user_icon).into(profileImage)
            progressBar?.dismiss();
        }

        if (Build.VERSION.SDK_INT >= 21) {
            val window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = this.resources.getColor(R.color.kotakblue)
        }
    }
}