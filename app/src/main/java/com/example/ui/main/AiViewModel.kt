package com.example.ui.main

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AiChatMessage
import com.example.data.repository.AiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

private const val TAG = "AiViewModel"

sealed interface AiStreamUiState {
    data object Idle : AiStreamUiState
    data object Loading : AiStreamUiState
    data class Streaming(val partialText: String) : AiStreamUiState
    data class Success(val text: String) : AiStreamUiState
    data class Error(val message: String) : AiStreamUiState
}

sealed interface AiAssistUiState {
    data object Idle : AiAssistUiState
    data object Loading : AiAssistUiState
    data class SuggestionsReady(val suggestions: List<String>) : AiAssistUiState
    data class DraftReady(val rewrittenText: String) : AiAssistUiState
    data class Error(val message: String) : AiAssistUiState
}

class AiViewModel(
    private val aiRepository: AiRepository = AiRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _streamState = MutableStateFlow<AiStreamUiState>(AiStreamUiState.Idle)
    val streamState: StateFlow<AiStreamUiState> = _streamState.asStateFlow()

    private val _assistState = MutableStateFlow<AiAssistUiState>(AiAssistUiState.Idle)
    val assistState: StateFlow<AiAssistUiState> = _assistState.asStateFlow()

    fun sendPrompt(prompt: String, attachedImage: Bitmap? = null) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank() && attachedImage == null) return
        if (_streamState.value is AiStreamUiState.Loading || _streamState.value is AiStreamUiState.Streaming) return

        val historySnapshot = _messages.value
        val userTurn = AiChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = cleanPrompt.ifBlank { "Analyze this image" },
            attachedImage = attachedImage
        )
        _messages.value = historySnapshot + userTurn
        _streamState.value = AiStreamUiState.Loading

        viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    aiRepository.streamAssistantReply(
                        history = historySnapshot,
                        prompt = cleanPrompt,
                        image = attachedImage
                    ).collect { chunk ->
                        if (chunk.isNotEmpty()) {
                            accumulated.append(chunk)
                            _streamState.value = AiStreamUiState.Streaming(accumulated.toString())
                        }
                    }
                }
                val finalText = accumulated.toString().trim()
                if (finalText.isNotBlank()) {
                    _messages.value = _messages.value + AiChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        text = finalText
                    )
                    _streamState.value = AiStreamUiState.Success(finalText)
                } else {
                    _streamState.value = AiStreamUiState.Error("Gemini returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini streaming timed out", e)
                _streamState.value = AiStreamUiState.Error("Request timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini streaming failed", e)
                _streamState.value = AiStreamUiState.Error(
                    e.localizedMessage ?: "Gemini request failed. Please check connection and try again."
                )
            }
        }
    }

    fun clearConversation() {
        _messages.value = emptyList()
        _streamState.value = AiStreamUiState.Idle
    }

    fun suggestReplies(recentLines: List<String>, userDisplayName: String) {
        viewModelScope.launch {
            _assistState.value = AiAssistUiState.Loading
            try {
                val suggestions = withTimeout(60_000L) {
                    aiRepository.generateSmartReplies(recentLines, userDisplayName)
                }
                if (suggestions.isNotEmpty()) {
                    _assistState.value = AiAssistUiState.SuggestionsReady(suggestions)
                } else {
                    _assistState.value = AiAssistUiState.Error("No reply suggestions returned.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Smart reply timed out", e)
                _assistState.value = AiAssistUiState.Error("Smart reply timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Smart reply failed", e)
                _assistState.value = AiAssistUiState.Error(
                    e.localizedMessage ?: "Unable to generate smart replies."
                )
            }
        }
    }

    fun rewriteDraft(draft: String, instruction: String) {
        if (draft.isBlank()) {
            _assistState.value = AiAssistUiState.Error("Type a draft message first to rewrite or translate it.")
            return
        }
        viewModelScope.launch {
            _assistState.value = AiAssistUiState.Loading
            try {
                val rewritten = withTimeout(60_000L) {
                    aiRepository.transformDraft(draft, instruction)
                }
                _assistState.value = AiAssistUiState.DraftReady(rewritten)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Draft rewrite timed out", e)
                _assistState.value = AiAssistUiState.Error("Rewrite timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Draft rewrite failed", e)
                _assistState.value = AiAssistUiState.Error(
                    e.localizedMessage ?: "Unable to rewrite draft."
                )
            }
        }
    }

    fun resetAssistState() {
        _assistState.value = AiAssistUiState.Idle
    }
}
