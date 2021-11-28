package com.example.kotakvoicebot.helpers

import android.util.Log
import com.google.cloud.dialogflow.v2beta1.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import android.os.AsyncTask
import com.example.kotakvoicebot.interfaces.BotReply


class SendMessageInBg(
    botReply: BotReply, session: SessionName, sessionsClient: SessionsClient,
    queryInput: QueryInput
) : AsyncTask<Void?, Void?, DetectIntentResponse?>() {
    private val session: SessionName
    private val sessionsClient: SessionsClient
    private val queryInput: QueryInput
    private val TAG = "async"
    private val botReply: BotReply = botReply
    override fun doInBackground(vararg params: Void?): DetectIntentResponse? {
        try {
            val detectIntentRequest = DetectIntentRequest.newBuilder()
                .setSession(session.toString())
                .setQueryInput(queryInput)
                .build()
            return sessionsClient.detectIntent(detectIntentRequest)
        } catch (e: Exception) {
            Log.d(TAG, "doInBackground: " + e.message)
            e.printStackTrace()
        }
        return null
    }

    override fun onPostExecute(response: DetectIntentResponse?) {
        //handle return response here
        botReply.callback(response)
    }

    init {
        this.session = session
        this.sessionsClient = sessionsClient
        this.queryInput = queryInput
    }
}