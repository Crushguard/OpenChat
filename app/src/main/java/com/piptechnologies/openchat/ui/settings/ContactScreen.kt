package com.piptechnologies.openchat.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.ui.components.DarkToastHost
import com.piptechnologies.openchat.ui.components.LocalToastHost
import com.piptechnologies.openchat.ui.components.InfoCallout
import com.piptechnologies.openchat.ui.components.OcTopBar
import com.piptechnologies.openchat.ui.components.PrimaryButton
import com.piptechnologies.openchat.ui.components.ScreenSurface
import com.piptechnologies.openchat.ui.components.rememberToastHostState
import com.piptechnologies.openchat.ui.theme.OcTheme

/**
 * Contact us (design map §4.18): the bar, the intro, a six-line message card, the optional email
 * field, the callout about what is sent, and a white footer with "Send", enabled once there is
 * [text]. Typing reports [onTextChange] / [onEmailChange]; Send reports [onSend].
 */
@Composable
fun ContactScreen(
    text: String,
    email: String,
    onBack: () -> Unit,
    onTextChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val c = OcTheme.colors
    ScreenSurface(modifier = Modifier.imePadding()) {
        OcTopBar(title = stringResource(R.string.contact_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = stringResource(R.string.contact_intro), style = OcTheme.type.body14, color = c.ink2)
            MessageField(value = text, onValueChange = onTextChange)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.contact_email_label), style = OcTheme.type.label12_5, color = c.ink2)
                Spacer(Modifier.height(6.dp))
                EmailField(value = email, onValueChange = onEmailChange)
            }
            InfoCallout(text = stringResource(R.string.contact_callout))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.surface),
        ) {
            HorizontalDivider(thickness = 1.dp, color = c.borderSoft)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp),
            ) {
                PrimaryButton(text = stringResource(R.string.contact_send), onClick = onSend, enabled = text.isNotBlank())
            }
        }
    }
}

/** Message card: white, 1 px border, radius 14, padding 12 14; a six-line body15 field with the placeholder in the placeholder colour. */
@Composable
private fun MessageField(value: String, onValueChange: (String) -> Unit) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.border, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        textStyle = OcTheme.type.body15.copy(color = c.ink),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        minLines = 6,
        cursorBrush = SolidColor(c.green),
        decorationBox = { innerField ->
            Box {
                if (value.isEmpty()) {
                    Text(text = stringResource(R.string.contact_placeholder), style = OcTheme.type.body15, color = c.placeholder)
                }
                innerField()
            }
        },
    )
}

/** Email input: 50 high, radius 12, 1 px border, padding 0 14, one line, email keyboard. */
@Composable
private fun EmailField(value: String, onValueChange: (String) -> Unit) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.border, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = OcTheme.type.body15.copy(color = c.ink),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            singleLine = true,
            cursorBrush = SolidColor(c.green),
            decorationBox = { innerField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(text = stringResource(R.string.contact_email_placeholder), style = OcTheme.type.body15, color = c.placeholder, maxLines = 1)
                    }
                    innerField()
                }
            },
        )
    }
}

/** Binds [ContactViewModel] to [ContactScreen]: toasts go to the app-level toast host (own host when none is provided), and [onBack] runs once the note has been handed to the email app. */
@Composable
fun ContactRoute(onBack: () -> Unit, viewModel: ContactViewModel = hiltViewModel()) {
    val text by viewModel.text.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val toast = LocalToastHost.current ?: rememberToastHostState()
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(viewModel, toast) {
        viewModel.toasts.collect { toast.show(it) }
    }
    LaunchedEffect(viewModel) {
        viewModel.sent.collect { currentOnBack() }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        ContactScreen(
            text = text,
            email = email,
            onBack = onBack,
            onTextChange = viewModel::setText,
            onEmailChange = viewModel::setEmail,
            onSend = viewModel::send,
        )
        if (LocalToastHost.current == null) DarkToastHost(state = toast)
    }
}
