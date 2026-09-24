package com.piptechnologies.openchat.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.piptechnologies.openchat.R
import com.piptechnologies.openchat.ui.components.ScreenSurface
import com.piptechnologies.openchat.ui.onboarding.OnboardingRoute
import com.piptechnologies.openchat.ui.splash.SplashRoute
import com.piptechnologies.openchat.ui.theme.OcTheme

/**
 * Root of the app UI: the navigation graph (design map §3). The launch screen goes to onboarding on
 * the first run and to Home afterwards; onboarding goes to Home. Both remove themselves from the back
 * stack, so Back on Home leaves the app. `launchSingleTop` keeps a double tap from stacking two Homes.
 */
@Composable
fun AppRoot() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashRoute(
                onFinished = { firstRun ->
                    navController.navigate(if (firstRun) Routes.ONBOARDING else Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.ONBOARDING) {
            OnboardingRoute(
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        // Stand-in so the launch flow always has a Home to land on. Task 18 replaces this entry with
        // HomeRoute and registers the remaining destinations.
        composable(Routes.HOME) {
            HomePlaceholder()
        }
    }
}

@Composable
private fun HomePlaceholder() {
    ScreenSurface {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = OcTheme.type.title19,
                color = OcTheme.colors.inkStrong,
            )
        }
    }
}
