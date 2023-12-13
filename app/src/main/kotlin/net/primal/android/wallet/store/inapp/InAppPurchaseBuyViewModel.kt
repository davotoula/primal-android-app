package net.primal.android.wallet.store.inapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.primal.android.networking.sockets.errors.WssException
import net.primal.android.user.accounts.active.ActiveAccountStore
import net.primal.android.wallet.repository.WalletRepository
import net.primal.android.wallet.store.domain.SatsPurchaseQuote
import net.primal.android.wallet.store.inapp.InAppPurchaseBuyContract.SideEffect
import net.primal.android.wallet.store.inapp.InAppPurchaseBuyContract.UiEvent
import net.primal.android.wallet.store.inapp.InAppPurchaseBuyContract.UiState
import net.primal.android.wallet.store.play.BillingClientHandler
import net.primal.android.wallet.store.play.InAppPurchaseException
import timber.log.Timber

@HiltViewModel
class InAppPurchaseBuyViewModel @Inject constructor(
    private val billingClientHandler: BillingClientHandler,
    private val activeAccountStore: ActiveAccountStore,
    private val walletRepository: WalletRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UiState(inAppSupported = billingClientHandler.supported))
    val state = _state.asStateFlow()
    private fun setState(reducer: UiState.() -> UiState) {
        _state.getAndUpdate { it.reducer() }
    }

    private val events = MutableSharedFlow<UiEvent>()
    fun setEvent(event: UiEvent) = viewModelScope.launch { events.emit(event) }

    private val _effects = Channel<SideEffect>()
    val effects = _effects.receiveAsFlow()
    private fun setEffect(effect: SideEffect) = viewModelScope.launch { _effects.send(effect) }

    init {
        subscribeToEvents()
        subscribeToPurchases()
    }

    private fun subscribeToEvents() =
        viewModelScope.launch {
            events.collect {
                when (it) {
                    UiEvent.ClearQuote -> setState { copy(quote = null) }
                    UiEvent.RefreshQuote -> refreshQuote()
                    is UiEvent.RequestPurchase -> launchBillingFlow(it)
                }
            }
        }

    private fun refreshQuote() =
        viewModelScope.launch {
            try {
                val localCurrency = billingClientHandler.currency
                val response = walletRepository.getInAppPurchaseMinSatsQuote(
                    userId = activeAccountStore.activeUserId(),
                    region = localCurrency.currencyCode,
                )
                setState {
                    copy(
                        quote = SatsPurchaseQuote(
                            quoteId = response.quoteId,
                            amountInBtc = response.amountBtc,
                            purchaseAmount = response.inAppPurchaseAmount,
                            purchaseSymbol = localCurrency.symbol,
                            purchaseCurrency = localCurrency.currencyCode,
                        ),
                    )
                }
            } catch (error: WssException) {
                Timber.e(error)
                if (_state.value.quote == null) {
                    setState { copy(error = error) }
                }
            }
        }

    private fun subscribeToPurchases() =
        viewModelScope.launch {
            billingClientHandler.purchases.collect { purchase ->
                if (purchase.quote.quoteId == _state.value.purchasingQuote?.quoteId) {
                    setEffect(SideEffect.PurchaseConfirmed)
                }
            }
        }

    private fun launchBillingFlow(event: UiEvent.RequestPurchase) =
        viewModelScope.launch {
            try {
                _state.value.quote?.let {
                    setState { copy(purchasingQuote = it) }
                    billingClientHandler.launchMinSatsBillingFlow(quote = it, activity = event.activity)
                }
            } catch (error: InAppPurchaseException) {
                Timber.e(error)
                setState { copy(error = error) }
            }
        }
}