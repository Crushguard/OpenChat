package com.piptechnologies.openchat.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.piptechnologies.openchat.core.messages.InboxMode
import com.piptechnologies.openchat.platform.NotificationAccess
import com.piptechnologies.openchat.ui.components.DarkToastHost
import com.piptechnologies.openchat.ui.components.LocalToastHost
import com.piptechnologies.openchat.ui.components.rememberToastHostState
import com.piptechnologies.openchat.ui.gate.GateRoute
import com.piptechnologies.openchat.ui.home.HomeRoute
import com.piptechnologies.openchat.ui.media.MediaDetailRoute
import com.piptechnologies.openchat.ui.media.MediaRoute
import com.piptechnologies.openchat.ui.messages.ConversationRoute
import com.piptechnologies.openchat.ui.messages.MessagesRoute
import com.piptechnologies.openchat.ui.messages.conversationKeyArg
import com.piptechnologies.openchat.ui.messages.inboxModeArg
import com.piptechnologies.openchat.ui.onboarding.OnboardingRoute
import com.piptechnologies.openchat.ui.second.SecondAccountRoute
import com.piptechnologies.openchat.ui.settings.ContactRoute
import com.piptechnologies.openchat.ui.settings.LanguageRoute
import com.piptechnologies.openchat.ui.settings.SettingsRoute
import com.piptechnologies.openchat.ui.splash.SplashRoute

/** What [MediaDetailRoute] gets when the id argument is missing; its ViewModel reads the same argument itself. */
private const val NO_MEDIA_ID = -1L

/**
 * Root of the app UI: the navigation graph (design map §3) under one dark toast host, so a toast a route
 * shows outlives that route being popped (§4.21: the toast shows over any screen). The launch screen goes
 * to onboarding on the first run and to Home afterwards; both remove themselves from the back stack, so
 * Back on Home leaves the app. A Home tool row opens its screen when notification access is granted and
 * the gate otherwise; Continue on the gate replaces the gate with the pending tool, and the gate opened
 * from Settings just goes back. The tool screens re-check the grant on every resume and hand over to the
 * gate when it was revoked ([RequireNotificationAccess]). Every push is `launchSingleTop`, so a double
 * tap cannot stack the same screen twice, and a route's Back pops only while that route is still on top
 * ([popFrom]), so a double tap cannot pop the screen underneath as well.
 */
@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val toast = rememberToastHostState()
    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalToastHost provides toast) {
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
                composable(Routes.HOME) {
                    HomeRoute(
                        onSettings = { navController.open(Routes.SETTINGS) },
                        onTool = { tool, accessGranted -> navController.open(homeToolRoute(tool, accessGranted)) },
                    )
                }
                composable(
                    route = Routes.GATE,
                    arguments = listOf(navArgument(Routes.ARG_TOOL) { type = NavType.StringType }),
                ) { entry ->
                    GateRoute(
                        tool = gateToolArg(entry.arguments?.getString(Routes.ARG_TOOL)),
                        onBack = { navController.popFrom(entry) },
                        onContinue = { tool ->
                            val target = gateTargetRoute(tool)
                            if (target == null) {
                                navController.popFrom(entry)
                            } else {
                                navController.navigate(target) {
                                    popUpTo(Routes.GATE) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                    )
                }
                composable(
                    route = Routes.MESSAGES,
                    arguments = listOf(navArgument(Routes.ARG_MODE) { type = NavType.StringType }),
                ) { entry ->
                    val mode = inboxModeArg(entry.arguments?.getString(Routes.ARG_MODE))
                    RequireNotificationAccess(tool = gateToolFor(mode), replaceFrom = Routes.MESSAGES, navController = navController, entry = entry) {
                        MessagesRoute(
                            mode = mode,
                            onBack = { navController.popFrom(entry) },
                            onOpenConversation = { m, key -> navController.open(Routes.conversation(m, key)) },
                        )
                    }
                }
                composable(
                    route = Routes.CONVERSATION,
                    arguments = listOf(
                        navArgument(Routes.ARG_MODE) { type = NavType.StringType },
                        navArgument(Routes.ARG_KEY) { type = NavType.StringType },
                    ),
                ) { entry ->
                    val mode = inboxModeArg(entry.arguments?.getString(Routes.ARG_MODE))
                    // A conversation is only reached from Messages, which stays under it, so the gate replaces both.
                    RequireNotificationAccess(tool = gateToolFor(mode), replaceFrom = Routes.MESSAGES, navController = navController, entry = entry) {
                        // Navigation hands the key over URI-decoded; conversationKeyArg only decodes a key still in
                        // its encoded form, so this is the same value the ViewModel reads from its SavedStateHandle.
                        ConversationRoute(
                            mode = mode,
                            key = conversationKeyArg(entry.arguments?.getString(Routes.ARG_KEY)),
                            onBack = { navController.popFrom(entry) },
                            onOpenMedia = { id -> navController.open(Routes.mediaDetail(id)) },
                        )
                    }
                }
                composable(Routes.MEDIA) { entry ->
                    RequireNotificationAccess(tool = GateTool.MEDIA, replaceFrom = Routes.MEDIA, navController = navController, entry = entry) {
                        MediaRoute(
                            onBack = { navController.popFrom(entry) },
                            onOpenDetail = { id -> navController.open(Routes.mediaDetail(id)) },
                        )
                    }
                }
                composable(
                    route = Routes.MEDIA_DETAIL,
                    arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.LongType }),
                ) { entry ->
                    MediaDetailRoute(
                        id = entry.arguments?.getLong(Routes.ARG_ID) ?: NO_MEDIA_ID,
                        onBack = { navController.popFrom(entry) },
                    )
                }
                composable(Routes.SECOND) { entry ->
                    SecondAccountRoute(onBack = { navController.popFrom(entry) })
                }
                composable(Routes.SETTINGS) { entry ->
                    SettingsRoute(
                        onBack = { navController.popFrom(entry) },
                        onOpenGate = { navController.open(Routes.gate(GateTool.SETTINGS)) },
                        onLanguage = { navController.open(Routes.LANGUAGE) },
                        onContact = { navController.open(Routes.CONTACT) },
                    )
                }
                composable(Routes.LANGUAGE) { entry ->
                    LanguageRoute(onBack = { navController.popFrom(entry) })
                }
                composable(Routes.CONTACT) { entry ->
                    ContactRoute(onBack = { navController.popFrom(entry) })
                }
            }
        }
        DarkToastHost(state = toast, modifier = Modifier.matchParentSize())
    }
}

/**
 * Design map §3: a tool screen re-checks notification access on every resume and, when it was revoked, hands
 * over to the gate for [tool]. The gate replaces the stack from [replaceFrom] up (the tool screen, and for a
 * conversation the Messages list under it), so Back from the gate returns to Home and Continue re-opens the
 * tool through the gate's own Continue. Only the current entry acts, never one that already sits under the
 * gate, and once per resumed period: the flag is reset on pause, so the next resume checks again.
 */
@Composable
private fun RequireNotificationAccess(
    tool: GateTool,
    replaceFrom: String,
    navController: NavHostController,
    entry: NavBackStackEntry,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var handedOver by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        if (!handedOver && !NotificationAccess.isGranted(context) && navController.currentBackStackEntry?.id == entry.id) {
            handedOver = true
            navController.navigate(Routes.gate(tool)) {
                popUpTo(replaceFrom) { inclusive = true }
                launchSingleTop = true
            }
        }
        onPauseOrDispose { handedOver = false }
    }
    content()
}

/** The gate a Messages screen hands over to: the unseen tool for All, the deleted-messages tool for Deleted only. */
private fun gateToolFor(mode: InboxMode): GateTool = when (mode) {
    InboxMode.ALL -> GateTool.UNSEEN
    InboxMode.DELETED -> GateTool.DELETED_MESSAGES
}

/** Pushes [route] unless it is already on top, so a double tap does not open the same screen twice. */
private fun NavHostController.open(route: String) {
    navigate(route) { launchSingleTop = true }
}

/** Pops [entry] only while it is still the current destination, so a double tap on Back pops one screen, not two. */
private fun NavHostController.popFrom(entry: NavBackStackEntry) {
    if (currentBackStackEntry?.id == entry.id) popBackStack()
}

/**
 * Where a Home tool row goes (design map §3, §4.3): the second account directly; the notification tools
 * to their screen once access is granted, and to the gate for that tool until then.
 */
private fun homeToolRoute(tool: HomeTool, accessGranted: Boolean): String = when (tool) {
    HomeTool.SECOND -> Routes.SECOND
    HomeTool.UNSEEN -> if (accessGranted) Routes.messages(InboxMode.ALL) else Routes.gate(GateTool.UNSEEN)
    HomeTool.DELETED_MESSAGES -> if (accessGranted) Routes.messages(InboxMode.DELETED) else Routes.gate(GateTool.DELETED_MESSAGES)
    HomeTool.MEDIA -> if (accessGranted) Routes.MEDIA else Routes.gate(GateTool.MEDIA)
}

/** The screen the gate's Continue opens, or null for the gate opened from Settings, which has no tool behind it. */
private fun gateTargetRoute(tool: GateTool): String? = when (tool) {
    GateTool.UNSEEN -> Routes.messages(InboxMode.ALL)
    GateTool.DELETED_MESSAGES -> Routes.messages(InboxMode.DELETED)
    GateTool.MEDIA -> Routes.MEDIA
    GateTool.SETTINGS -> null
}

/** The gate's `tool` argument; a value that is not a [GateTool] reads as the Settings gate, whose Continue goes back. */
private fun gateToolArg(raw: String?): GateTool = GateTool.entries.firstOrNull { it.name == raw } ?: GateTool.SETTINGS
