package com.example.data.repository

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val attachedImage: Bitmap? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)

class AiRepository {

    private val assistantModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are WeConnect AI, a helpful, friendly, and concise assistant built into the WeConnect messaging and social app. " +
                        "Help users draft messages, summarize information, translate languages, brainstorm social captions, and analyze shared images clearly."
                )
            }
        )
    }

    private val smartReplySchema by lazy {
        Schema.obj(
            properties = mapOf(
                "suggestions" to Schema.array(
                    items = Schema.string(description = "A short, natural reply under 18 words")
                )
            )
        )
    }

    private val smartReplyModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = smartReplySchema
            }
        )
    }

    private val textTransformModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    fun streamAssistantReply(
        history: List<AiChatMessage>,
        prompt: String,
        image: Bitmap? = null
    ): Flow<String> {
        val sdkHistory: List<Content> = history
            .filter { it.text.isNotBlank() }
            .takeLast(12)
            .map { msg ->
                content(role = if (msg.isUser) "user" else "model") {
                    msg.attachedImage?.let { image(it) }
                    text(msg.text)
                }
            }
        val chat = assistantModel.startChat(history = sdkHistory)
        val outgoingContent = content("user") {
            image?.let { image(it) }
            text(prompt.ifBlank { "Please describe and analyze this image." })
        }
        return chat.sendMessageStream(outgoingContent).map { chunk ->
            chunk.text.orEmpty()
        }
    }

    suspend fun generateSmartReplies(
        recentConversationLines: List<String>,
        userDisplayName: String
    ): List<String> {
        val contextBlock = if (recentConversationLines.isEmpty()) {
            "No prior messages yet. Suggest 3 friendly conversation openers."
        } else {
            "Recent messages in chat (user is $userDisplayName):\n" +
                recentConversationLines.takeLast(6).joinToString("\n")
        }
        val response = smartReplyModel.generateContent(
            "$contextBlock\n\nGenerate 3 natural, varied reply suggestions for $userDisplayName."
        )
        val jsonString = response.text ?: throw IllegalStateException("Gemini returned an empty response.")
        val json = JSONObject(jsonString)
        val arr = json.getJSONArray("suggestions")
        return (0 until arr.length())
            .map { arr.getString(it).trim() }
            .filter { it.isNotBlank() }
            .take(4)
    }

    suspend fun transformDraft(
        draftText: String,
        instruction: String
    ): String {
        val prompt = """
            Rewrite or transform the following chat message draft according to this instruction: "$instruction".
            Return only the ready-to-send message text with no quotes or extra commentary.

            Draft:
            $draftText
        """.trimIndent()
        val response = textTransformModel.generateContent(prompt)
        return response.text?.trim()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Gemini returned an empty response.")
    }
}
