package com.carwash.mobile.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.carwash.mobile.R
import com.carwash.mobile.ingreso.normalizeServer

@Composable
fun LoginScreen(model: LoginViewModel, serverUrl: String, onServerUrl: (String) -> Unit) {
    val state = model.state
    var server by remember { mutableStateOf(serverUrl) }
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, androidx.compose.ui.Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = server,
            onValueChange = { server = it; normalizeServer(it)?.let(onServerUrl) },
            label = { Text(stringResource(R.string.server_label)) },
            placeholder = { Text(stringResource(R.string.server_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.userName,
            onValueChange = model::updateUser,
            label = { Text(stringResource(R.string.login_user)) },
            singleLine = true,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = model::updatePassword,
            label = { Text(stringResource(R.string.login_password)) },
            singleLine = true,
            enabled = !state.loading,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let {
            Text(
                stringResource(
                    when (it) {
                        LoginError.EMPTY -> R.string.login_error_empty
                        LoginError.BAD_CREDENTIALS -> R.string.login_error_bad
                        LoginError.UNREACHABLE -> R.string.login_error_unreachable
                    },
                ),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(onClick = model::submit, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
            if (state.loading) CircularProgressIndicator(Modifier.padding(2.dp)) else Text(stringResource(R.string.login_submit))
        }
    }
}
