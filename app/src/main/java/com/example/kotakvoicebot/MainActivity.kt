package com.example.kotakvoicebot

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.example.kotakvoicebot.helpers.SendMessageInBg
import com.example.kotakvoicebot.interfaces.BotReply
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.dialogflow.v2beta1.*
import com.google.common.collect.Lists
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView
import java.io.InputStream
import java.util.*

import com.bumptech.glide.Glide

import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import android.app.ProgressDialog
import android.net.Uri
import android.widget.*


class MainActivity : AppCompatActivity(), BotReply {

    //dialogFlow
    private var sessionsClient: SessionsClient? = null
    private var sessionName: SessionName? = null
    private val uuid: String = UUID.randomUUID().toString()
    private val TAG = "mainactivity"
    private val REQUEST_CODE_SPEECH_INPUT = 1
    private lateinit var textView : TextView
    var textToSpeech: TextToSpeech? = null
    private val back: ImageView? = null
    private var progressBar: ProgressDialog? = null
    private var rl_acc : RelativeLayout? = null
    private var rl_bal : RelativeLayout? = null
    private var rl_pro : RelativeLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Progress Bar
        progressBar = ProgressDialog(this);
        progressBar?.setCancelable(true);//you can cancel it by pressing back button
        progressBar?.setMessage("Please Wait...");
        progressBar?.show();

        val emergency = findViewById<ImageView>(R.id.emergency)
        emergency.setOnClickListener(View.OnClickListener {
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.data = Uri.parse("tel:"+15712106396) //change the number
            startActivity(callIntent)
        })

        rl_acc = findViewById(R.id.rl_account)
        rl_acc?.setOnClickListener(View.OnClickListener {
            startActivity(Intent(applicationContext, AccountActivity::class.java))
        })

        rl_bal = findViewById(R.id.rl_balance)
        rl_bal?.setOnClickListener(View.OnClickListener {
            startActivity(Intent(applicationContext, BalanceActivity::class.java))
        })

        rl_pro = findViewById(R.id.rl_product)
        rl_pro?.setOnClickListener(View.OnClickListener {
            startActivity(Intent(applicationContext, ProductActivity::class.java))
        })

        var logout = findViewById<ImageView>(R.id.logout);
        logout.setOnClickListener { view ->
            // Configure Google Sign In
            val gso =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()


            // Build a GoogleSignInClient with the options specified by gso.
            val mGoogleSignInClient =
                GoogleSignIn.getClient(applicationContext, gso)
            FirebaseAuth.getInstance().signOut()
            mGoogleSignInClient.signOut()
            val intent =
                Intent(applicationContext, LoginActivity::class.java)
            startActivity(intent)
        }

        val profileImage = findViewById<ImageView>(R.id.user)
        val signInAccount = GoogleSignIn.getLastSignedInAccount(this)
        if (signInAccount != null) {
            Glide.with(this).load(signInAccount.photoUrl).placeholder(R.drawable.user_icon).into(profileImage)
            progressBar?.dismiss();
        }

        // create an object textToSpeech and adding features into it
        // create an object textToSpeech and adding features into it
        textToSpeech = TextToSpeech(applicationContext) { i ->
            // if No error is found then only it will run
            if (i != TextToSpeech.ERROR) {
                // To Choose language of speech
                textToSpeech!!.language = Locale.ENGLISH
            }
        }

//        textView = findViewById(R.id.bot_text)
        findViewById<CircleImageView>(R.id.user).setOnClickListener(View.OnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
            )
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")

            try {
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
            } catch (e: Exception) {
                Toast
                    .makeText(
                        this@MainActivity, " " + e.message,
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
        })

        if (Build.VERSION.SDK_INT >= 21) {
            val window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = this.resources.getColor(R.color.kotakblue)
        }

        setUpBot()

    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int,
        @Nullable data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                val result = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                )
                var message = Objects.requireNonNull(result)?.get(0)
                if (message != null) {
                    if (!message.isEmpty()) {
                        sendMessageToBot(message)
                    }
                }
            }
        }
    }

    private fun setUpBot() {
        try {
            val stream: InputStream = this.resources.openRawResource(R.raw.credential)
            val credentials: GoogleCredentials = GoogleCredentials.fromStream(stream)
                .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"))
            val projectId = (credentials as ServiceAccountCredentials).projectId
            val settingsBuilder: SessionsSettings.Builder = SessionsSettings.newBuilder()
            val sessionsSettings: SessionsSettings = settingsBuilder.setCredentialsProvider(
                FixedCredentialsProvider.create(credentials)
            ).build()
            sessionsClient = SessionsClient.create(sessionsSettings)
            sessionName = SessionName.of(projectId, uuid)
            Log.d(TAG, "projectId : $projectId")
        } catch (e: Exception) {
            Log.d(TAG, "setUpBot: " + e.message)
        }
    }

    private fun sendMessageToBot(message: String) {
        val input: QueryInput = QueryInput.newBuilder()
            .setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US")).build()
        SendMessageInBg(this, sessionName!!, sessionsClient!!, input).execute()
    }

    override fun callback(returnResponse: DetectIntentResponse?) {
        if (returnResponse != null) {
            val botReply = returnResponse.queryResult.fulfillmentText
            if (!botReply.isEmpty()) {
//                textView.text = botReply
                textToSpeech?.speak(botReply,TextToSpeech.QUEUE_FLUSH,null);
                intentCall(botReply)
            } else {
                Toast.makeText(this, "something went wrong", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "failed to connect!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun intentCall(botReply: String?) {
        if(botReply == "Your current account balance is four Lakhs, thirty five Thousands, five hundred and eleven rupees only.") {
            startActivity(Intent(applicationContext, BalanceActivity::class.java))
        }
        else if(botReply == "Here is your account details with all the recent transactions.") {
            startActivity(Intent(applicationContext, AccountActivity::class.java))
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val a = Intent(Intent.ACTION_MAIN)
        a.addCategory(Intent.CATEGORY_HOME)
        a.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(a)
    }
}