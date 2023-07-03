package net.primal.android.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.primal.android.R
import net.primal.android.core.compose.PrimalButton
import net.primal.android.core.compose.PrimalDefaults
import net.primal.android.core.compose.PrimalTopAppBar
import net.primal.android.core.compose.icons.PrimalIcons
import net.primal.android.core.compose.icons.primaliconpack.ArrowBack
import net.primal.android.core.utils.isValidNsec
import net.primal.android.theme.AppTheme
import net.primal.android.theme.PrimalTheme

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onClose: () -> Unit,
    onLoginSuccess: (String) -> Unit,
) {
    LaunchedEffect(viewModel, onLoginSuccess) {
        viewModel.effect.collect {
            when (it) {
                is LoginContract.SideEffect.LoginSuccess -> onLoginSuccess(it.pubkey)
            }
        }
    }

    val uiState = viewModel.state.collectAsState()

    LoginScreen(
        state = uiState.value,
        eventPublisher = { viewModel.setEvent(it) },
        onClose = onClose,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    state: LoginContract.UiState,
    eventPublisher: (LoginContract.UiEvent) -> Unit,
    onClose: () -> Unit,
) {
    Scaffold(
        topBar = {
            PrimalTopAppBar(
                title = stringResource(id = R.string.login_title),
                navigationIcon = PrimalIcons.ArrowBack,
                onNavigationIconClick = onClose,
            )
        },
        content = { paddingValues ->
            LoginContent(
                state = state,
                paddingValues = paddingValues,
                onLogin = {
                    eventPublisher(LoginContract.UiEvent.LoginEvent(nsec = it))
                }
            )
        }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginContent(
    state: LoginContract.UiState,
    paddingValues: PaddingValues,
    onLogin: (String) -> Unit,
) {
    var nsecValue by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    LaunchedEffect(Unit) {
        val clipboardText = clipboardManager.getText()?.text.orEmpty()
        if (clipboardText.isValidNsec()) {
            nsecValue = clipboardText
        }
    }

    val isValidNsec by remember { derivedStateOf { nsecValue.isValidNsec() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(paddingValues = paddingValues)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 64.dp)
                .weight(1f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier = Modifier
                    .padding(all = 16.dp)
                    .fillMaxWidth(),
                text = stringResource(id = R.string.login_description),
                textAlign = TextAlign.Center,
                style = AppTheme.typography.bodyLarge,
                fontSize = 20.sp,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = nsecValue,
                onValueChange = { input -> nsecValue = input },
                placeholder = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.nsec),
                        textAlign = TextAlign.Center,
                        style = AppTheme.typography.bodyLarge,
                        color = AppTheme.extraColorScheme.onSurfaceVariantAlt4,
                    )
                },
                supportingText = {
                    if (nsecValue.isNotEmpty()) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = if (isValidNsec) {
                                stringResource(id = R.string.login_valid_nsec_key)
                            } else {
                                stringResource(id = R.string.login_invalid_nsec_key)
                            },
                            textAlign = TextAlign.Center,
                            color = if (isValidNsec) {
                                AppTheme.extraColorScheme.successBright
                            } else {
                                AppTheme.colorScheme.error
                            }
                        )
                    }
                },
                isError = nsecValue.isNotEmpty() && !isValidNsec,
                minLines = 3,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    imeAction = if (isValidNsec) ImeAction.Go else ImeAction.Default
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (isValidNsec) {
                            keyboardController?.hide()
                            onLogin(nsecValue)
                        }
                    }
                ),
                colors = PrimalDefaults.outlinedTextFieldColors(
                    focusedBorderColor = if (nsecValue.isEmpty()) {
                        AppTheme.extraColorScheme.surfaceVariantAlt
                    } else {
                        AppTheme.extraColorScheme.successBright.copy(alpha = 0.5f)
                    },
                    unfocusedBorderColor = if (nsecValue.isEmpty()) {
                        AppTheme.extraColorScheme.surfaceVariantAlt
                    } else {
                        AppTheme.extraColorScheme.successBright.copy(alpha = 0.5f)
                    },
                )
            )
        }

        PrimalButton(
            text = if (isValidNsec) {
                stringResource(id = R.string.login_button_continue)
            } else {
                stringResource(id = R.string.login_button_paste_key)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .align(alignment = Alignment.CenterHorizontally),
            loading = state.loading,
            onClick = {
                if (isValidNsec) {
                    keyboardController?.hide()
                    onLogin(nsecValue)
                } else {
                    val clipboardText = clipboardManager.getText()?.text.orEmpty()
                    if (clipboardText.isNotEmpty()) {
                        nsecValue = clipboardText
                    }

                }
            },
        )
    }
}

@Preview
@Composable
fun PreviewLoginScreen() {
    PrimalTheme {
        LoginScreen(
            state = LoginContract.UiState(loading = false),
            eventPublisher = {},
            onClose = {},
        )
    }
}