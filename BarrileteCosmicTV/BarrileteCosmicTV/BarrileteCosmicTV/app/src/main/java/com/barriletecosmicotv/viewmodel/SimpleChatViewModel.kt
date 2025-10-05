package com.barriletecosmicotv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriletecosmicotv.data.SimpleChatMessage
import com.barriletecosmicotv.data.SimpleChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SimpleChatViewModel @Inject constructor(
    private val chatRepository: SimpleChatRepository
) : ViewModel() {

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    init {
        // Load saved username
        viewModelScope.launch {
            chatRepository.username.collect { username ->
                _username.value = username
            }
        }
    }

    fun getChatMessages(streamId: String): Flow<List<SimpleChatMessage>> {
        return chatRepository.getChatMessages(streamId)
    }

    fun sendMessage(streamId: String, username: String, message: String) {
        viewModelScope.launch {
            chatRepository.addMessage(streamId, username, message)
        }
    }

    fun saveUsername(username: String) {
        viewModelScope.launch {
            chatRepository.saveUsername(username)
            _username.value = username
        }
    }

    fun clearChat(streamId: String) {
        // Clear not available in repository, skip
    }
}