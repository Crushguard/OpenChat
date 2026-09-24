package com.piptechnologies.openchat.ui.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.ui.components.BrandMark
import com.piptechnologies.openchat.ui.components.HonestyLine
import com.piptechnologies.openchat.ui.components.OcSpinner
import com.piptechnologies.openchat.ui.components.ScreenSurface
import com.piptechnologies.openchat.ui.theme.OcTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** How long the launch screen stays up (design map §4.1). */
private const val SPLASH_DURATION_MS = 1_200L

/**
 * Launch screen (design map §4.1): brand mark 88/26 with its green glow, "OpenChat" display24, the
 * 22 dp spinner, and the honesty line 34 dp above the bottom. The group is centred in the space above
 * a 30 dp bottom padding, as in the design.
 */
@Composable
fun SplashScreen() {
    val c = OcTheme.colors
    ScreenSurface {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandMark(
                size = 88.dp,
                radius = 26.dp,
                iconSize = 40.dp,
                modifier = Modifier.shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(26.dp),
                    ambientColor = Color.Transparent,
                    spotColor = c.green,
                ),
            )
            // Design: 14 column gap + 6 top margin.
            Spacer(Modifier.height(20.dp))
            Text(text = stringResource(R.string.app_name), style = OcTheme.type.display24, color = c.inkStrong)
            // Design: 14 column gap + 14 top margin on the spinner.
            Spacer(Modifier.height(28.dp))
            OcSpinner(size = 22.dp)
        }
        HonestyLine(
            text = stringResource(R.string.splash_honesty),
            modifier = Modifier.padding(bottom = 34.dp),
        )
    }
}

/** Shows [SplashScreen] for 1.2 s, then reports whether this is the first run (onboarding not done yet). */
@Composable
fun SplashRoute(onFinished: (firstRun: Boolean) -> Unit, viewModel: SplashViewModel = hiltViewModel()) {
    val latestOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(viewModel) {
        delay(SPLASH_DURATION_MS)
        val onboardingDone = viewModel.onboardingDone.first()
        latestOnFinished(!onboardingDone)
    }
    SplashScreen()
}
