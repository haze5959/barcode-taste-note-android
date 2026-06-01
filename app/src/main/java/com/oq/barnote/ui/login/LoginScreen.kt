package com.oq.barnote.ui.login

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.oq.barnote.Constants
import com.oq.barnote.R
import com.oq.barnote.core.designsystem.Dimens
import com.oq.barnote.core.designsystem.barNotePalette
import com.oq.barnote.core.oqcore.views.OQFillButton
import dagger.hilt.android.EntryPointAccessors

@Composable
fun LoginRoute(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(Unit) {
        viewModel.navEffect.collect { effect ->
            when (effect) {
                LoginNavEffect.LoggedIn -> onLoggedIn()
            }
        }
    }

    LoginScreen(
        state = state,
        onBack = onBack,
        onLogin = {
            val act = activity ?: return@LoginScreen
            startAuth0Login(
                activity = act,
                onStart = { viewModel.onEvent(LoginUiEvent.LoginStarted) },
                onSuccess = { creds -> viewModel.onEvent(LoginUiEvent.LoginSuccess(creds)) },
                onError = { msg -> viewModel.onEvent(LoginUiEvent.LoginError(msg)) },
                onCancel = { viewModel.onEvent(LoginUiEvent.LoginCancelled) },
            )
        },
        onDismissError = { viewModel.onEvent(LoginUiEvent.DismissError) },
    )
}

@Composable
internal fun LoginScreen(
    state: LoginUiState,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onDismissError: () -> Unit,
) {
    val background = colorResource(com.oq.barnote.core.designsystem.R.color.background_primary)
    val textPrimary = colorResource(com.oq.barnote.core.designsystem.R.color.text_primary)
    val textSecondary = colorResource(com.oq.barnote.core.designsystem.R.color.text_secondary)
    val palette = barNotePalette()

    Box(modifier = Modifier.fillMaxSize().background(background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.Padding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = textPrimary,
                    modifier = Modifier
                        .size(Dimens.IconSize)
                        .clickable(onClick = onBack)
                        .padding(4.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.BtnPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
            ) {
                Image(
                    painter = painterResource(R.drawable.launch_icon),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(Dimens.LargeCardSize)
                        .clip(RoundedCornerShape(24.dp)),
                )

                Text(
                    text = stringResource(R.string.rogeuin_pilyo),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                )

                Text(
                    text = stringResource(R.string.bogjabhan_gaib_eobsi_3comane_rogeuinhago_gineungeul_iyonghae),
                    style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            OQFillButton(
                text = stringResource(R.string.rogeuinhagi),
                onClick = onLogin,
                palette = palette,
                radius = Dimens.Radius.value,
                enabled = !state.isLoggingIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.BtnPadding),
            )
        }

        if (state.isLoggingIn) {
            Box(
                modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }

        if (state.errorMessage != null) {
            AlertDialog(
                onDismissRequest = onDismissError,
                title = { Text(text = stringResource(R.string.oryu_jebo)) },
                text = { Text(text = state.errorMessage) },
                confirmButton = {
                    TextButton(onClick = onDismissError) {
                        Text(text = stringResource(R.string.dadgi))
                    }
                },
            )
        }
    }
}

/** Auth0 `WebAuthProvider.login()` 호출. Activity 컨텍스트 필수. */
private fun startAuth0Login(
    activity: Activity,
    onStart: () -> Unit,
    onSuccess: (Credentials) -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val entryPoint = EntryPointAccessors.fromApplication(
        activity.applicationContext,
        LoginEntryPoint::class.java,
    )
    val auth0 = entryPoint.auth0()
    val scheme = entryPoint.auth0Scheme()

    onStart()
    WebAuthProvider.login(auth0)
        .withScheme(scheme)
        .withAudience(Constants.S.AUTH_AUDIENCE)
        // `offline_access` 가 있어야 refresh token 발급 → SecureCredentialsManager 자동 갱신 가능.
        // iOS `Auth0.webAuth().scope("openid profile offline_access")` 와 동등.
        .withScope("openid profile offline_access")
        .start(activity, object : Callback<Credentials, AuthenticationException> {
            override fun onSuccess(result: Credentials) {
                onSuccess(result)
            }

            override fun onFailure(error: AuthenticationException) {
                if (error.isCanceled) onCancel()
                else onError(error.message.orEmpty().ifBlank { error.code })
            }
        })
}

/** [Context] 체인을 따라 [Activity] 찾기. */
private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
