package com.example.kotakvoicebot.interfaces

import com.google.cloud.dialogflow.v2beta1.DetectIntentResponse


interface BotReply {
    fun callback(returnResponse: DetectIntentResponse?)
}