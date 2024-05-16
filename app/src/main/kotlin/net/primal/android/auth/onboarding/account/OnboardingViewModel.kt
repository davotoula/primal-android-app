package net.primal.android.auth.onboarding.account

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.primal.android.auth.onboarding.account.OnboardingContract.UiEvent
import net.primal.android.auth.onboarding.account.OnboardingContract.UiState
import net.primal.android.auth.onboarding.account.api.OnboardingApi
import net.primal.android.auth.repository.CreateAccountHandler
import net.primal.android.core.coroutines.CoroutineDispatcherProvider
import net.primal.android.crypto.CryptoUtils
import net.primal.android.networking.primal.upload.PrimalFileUploader
import net.primal.android.networking.primal.upload.UnsuccessfulFileUpload
import net.primal.android.networking.sockets.errors.WssException
import net.primal.android.profile.domain.ProfileMetadata
import timber.log.Timber

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val dispatcherProvider: CoroutineDispatcherProvider,
    private val onboardingApi: OnboardingApi,
    private val createAccountHandler: CreateAccountHandler,
    private val fileUploader: PrimalFileUploader,
) : ViewModel() {

    private val keyPair = CryptoUtils.generateHexEncodedKeypair()

    private var avatarUploadJob: Job? = null
    private var bannerUploadJob: Job? = null

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()
    fun setState(reducer: UiState.() -> UiState) {
        _state.getAndUpdate(reducer)
    }

    private val events = MutableSharedFlow<UiEvent>()
    fun setEvent(event: UiEvent) = viewModelScope.launch { events.emit(event) }

    init {
        observeEvents()
        fetchInterests()
    }

    private fun observeEvents() =
        viewModelScope.launch {
            events.collect {
                when (it) {
                    is UiEvent.InterestSelected -> setState {
                        copy(suggestions = suggestions.toMutableList().apply { add(it.suggestion) })
                    }

                    is UiEvent.InterestUnselected -> setState {
                        copy(suggestions = suggestions.toMutableList().apply { remove(it.suggestion) })
                    }

                    is UiEvent.ProfileAboutYouUpdated -> setState {
                        copy(profileAboutYou = it.aboutYou)
                    }

                    is UiEvent.ProfileAvatarUriChanged -> updateAvatarPhoto(it.avatarUri)
                    is UiEvent.ProfileBannerUriChanged -> updateBannerPhoto(it.bannerUri)

                    is UiEvent.ProfileDisplayNameUpdated -> setState {
                        copy(profileDisplayName = it.displayName)
                    }

                    UiEvent.RequestNextStep -> setState {
                        copy(currentStep = this.currentStep.nextStep())
                    }

                    UiEvent.RequestPreviousStep -> setState {
                        copy(currentStep = this.currentStep.previousStep())
                    }

                    UiEvent.CreateNostrProfile -> {
                        createNostrAccount()
                    }

                    UiEvent.DismissError -> setState {
                        copy(error = null)
                    }
                }
            }
        }

    private fun fetchInterests() =
        viewModelScope.launch {
            try {
                setState { copy(working = true) }
                val response = retry(times = 3) {
                    withContext(dispatcherProvider.io()) {
                        onboardingApi.getFollowSuggestions()
                    }
                }
                setState { copy(allSuggestions = response.suggestions) }
            } catch (error: IOException) {
                Timber.e(error)
            } finally {
                setState { copy(working = false) }
            }
        }

    private suspend fun <T> retry(times: Int, block: suspend (Int) -> T): T {
        repeat(times) {
            try {
                return block(it)
            } catch (error: IOException) {
                Timber.w(error)
                delay(DELAY * (it + 1))
            }
        }
        return block(times)
    }

    private fun createNostrAccount() =
        viewModelScope.launch {
            try {
                setState { copy(working = true) }
                avatarUploadJob?.join()
                bannerUploadJob?.join()
                val uiState = state.value
                withContext(dispatcherProvider.io()) {
                    createAccountHandler.createNostrAccount(
                        privateKey = keyPair.privateKey,
                        profileMetadata = uiState.asProfileMetadata(),
                        interests = uiState.suggestions,
                    )
                }
                setState { copy(accountCreated = true) }
            } catch (error: UnsuccessfulFileUpload) {
                Timber.w(error)
                setState { copy(error = UiState.OnboardingError.ImageUploadFailed(error)) }
            } catch (error: CreateAccountHandler.AccountCreationException) {
                Timber.w(error)
                setState { copy(error = UiState.OnboardingError.CreateAccountFailed(error)) }
            } finally {
                setState { copy(working = false) }
            }
        }

    private fun updateAvatarPhoto(avatarUri: Uri?) {
        setState { copy(avatarUri = avatarUri) }
        avatarUploadJob?.cancel()
        if (avatarUri != null) {
            avatarUploadJob = viewModelScope.launch {
                try {
                    val remoteAvatarUrl = withContext(dispatcherProvider.io()) {
                        fileUploader.uploadFile(keyPair = keyPair, uri = avatarUri)
                    }
                    setState { copy(avatarRemoteUrl = remoteAvatarUrl) }
                } catch (error: UnsuccessfulFileUpload) {
                    Timber.w(error)
                } catch (error: WssException) {
                    Timber.w(error)
                }
            }
        }
    }

    private fun updateBannerPhoto(bannerUri: Uri?) {
        setState { copy(bannerUri = bannerUri) }
        bannerUploadJob?.cancel()
        if (bannerUri != null) {
            bannerUploadJob = viewModelScope.launch {
                try {
                    val remoteBannerUrl = withContext(dispatcherProvider.io()) {
                        fileUploader.uploadFile(keyPair = keyPair, uri = bannerUri)
                    }
                    setState { copy(bannerRemoteUrl = remoteBannerUrl) }
                } catch (error: UnsuccessfulFileUpload) {
                    Timber.w(error)
                } catch (error: WssException) {
                    Timber.w(error)
                }
            }
        }
    }

    private fun OnboardingStep.nextStep() = OnboardingStep.fromIndex(this.index + 1)

    private fun OnboardingStep.previousStep() = OnboardingStep.fromIndex(this.index - 1)

    private fun UiState.asProfileMetadata(): ProfileMetadata =
        ProfileMetadata(
            username = null,
            displayName = this.profileDisplayName,
            about = this.profileAboutYou,
            localPictureUri = this.avatarUri,
            remotePictureUrl = this.avatarRemoteUrl,
            localBannerUri = this.bannerUri,
            remoteBannerUrl = this.bannerRemoteUrl ?: DEFAULT_BANNER_URL,
        )

    companion object {
        private const val DELAY = 300L
        private const val DEFAULT_BANNER_URL = "https://m.primal.net/HQTd.jpg"
    }
}
