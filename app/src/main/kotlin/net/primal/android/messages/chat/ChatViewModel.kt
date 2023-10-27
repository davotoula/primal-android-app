package net.primal.android.messages.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.primal.android.core.compose.feed.model.asNostrResourceUi
import net.primal.android.core.compose.media.model.asMediaResourceUi
import net.primal.android.core.compose.profile.model.asProfileDetailsUi
import net.primal.android.messages.chat.ChatContract.UiEvent
import net.primal.android.messages.chat.ChatContract.UiState
import net.primal.android.messages.chat.model.ChatMessageUi
import net.primal.android.messages.db.DirectMessage
import net.primal.android.messages.repository.MessageRepository
import net.primal.android.navigation.profileIdOrThrow
import net.primal.android.networking.sockets.errors.WssException
import net.primal.android.profile.repository.ProfileRepository
import net.primal.android.user.accounts.active.ActiveAccountStore
import net.primal.android.user.badges.BadgesManager
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    activeAccountStore: ActiveAccountStore,
    private val badgesManager: BadgesManager,
    private val messageRepository: MessageRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val participantId = savedStateHandle.profileIdOrThrow
    private val userId = activeAccountStore.activeUserId()

    private val _state = MutableStateFlow(
        UiState(
            participantId = participantId,
            messages = messageRepository
                .newestMessages(participantId = participantId)
                .mapAsPagingDataOfChatMessageUi()
        )
    )
    val state = _state.asStateFlow()
    private fun setState(reducer: UiState.() -> UiState) = _state.getAndUpdate { it.reducer() }

    private val _event: MutableSharedFlow<UiEvent> = MutableSharedFlow()
    fun setEvent(event: UiEvent) = viewModelScope.launch { _event.emit(event) }

    init {
        observeEvents()
        observeMessagesSeenEvents()
        observeParticipant()
        subscribeToTotalUnreadCountChanges()
    }

    private fun observeEvents() = viewModelScope.launch {
        _event.collect {
            when (it) {
                is UiEvent.MessageSend -> Unit
                UiEvent.MessagesSeen -> Unit

            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeMessagesSeenEvents() = viewModelScope.launch {
        _event
            .filterIsInstance(UiEvent.MessagesSeen::class)
            .debounce(1.seconds)
            .collect {
                markConversationAsRead()
            }
    }

    private fun observeParticipant() = viewModelScope.launch {
        profileRepository.observeProfile(profileId = participantId).collect {
            setState {
                copy(
                    participantProfile = it.metadata?.asProfileDetailsUi() ?: this.participantProfile,
                    participantMediaResources = it.resources.map { it.asMediaResourceUi() },
                )
            }
        }
    }

    private fun subscribeToTotalUnreadCountChanges() = viewModelScope.launch {
        badgesManager.badges
            .map { it.messages }
            .distinctUntilChanged()
            .collect {
                try {
                    messageRepository.fetchNewConversationMessages(userId, participantId)
                } catch (error: WssException) {
                    Timber.w(error)
                }
            }
    }

    private fun markConversationAsRead() = viewModelScope.launch {
        try {
            messageRepository.markConversationAsRead(userId, participantId)
        } catch (error: WssException) {
            Timber.w(error)
        }
    }

    private fun Flow<PagingData<DirectMessage>>.mapAsPagingDataOfChatMessageUi() =
        map { pagingData -> pagingData.map { it.mapAsChatMessageUi() } }

    private fun DirectMessage.mapAsChatMessageUi() =
        ChatMessageUi(
            messageId = this.data.messageId,
            isUserMessage = userId == this.data.senderId,
            senderId = this.data.senderId,
            timestamp = Instant.ofEpochSecond(this.data.createdAt),
            content = this.data.content,
            mediaResources = this.mediaResources.map { it.asMediaResourceUi() },
            nostrResources = this.nostrUris.map { it.asNostrResourceUi() },
            hashtags = this.data.hashtags,
        )
}