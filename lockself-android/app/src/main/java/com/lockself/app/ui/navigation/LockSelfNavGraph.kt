package com.lockself.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.lockself.app.data.crypto.VaultCryptoManager
import com.lockself.app.data.model.RecordType
import com.lockself.app.data.repository.VaultRepository
import com.lockself.app.ui.screens.EditRecordScreen
import com.lockself.app.ui.screens.LockScreen
import com.lockself.app.ui.screens.PasswordHistoryScreen
import com.lockself.app.ui.screens.RecordDetailScreen
import com.lockself.app.ui.screens.SetupScreen
import com.lockself.app.ui.screens.SettingsScreen
import com.lockself.app.ui.screens.VaultMainScreen
import com.lockself.app.ui.viewmodel.EditRecordViewModel
import com.lockself.app.ui.viewmodel.RecordDetailViewModel
import com.lockself.app.ui.viewmodel.SettingsViewModel
import com.lockself.app.ui.viewmodel.VaultMainViewModel
import com.lockself.app.ui.viewmodel.ViewModelFactory
import com.lockself.app.util.AppPreferences
import com.lockself.app.util.ClipboardHelper

@Composable
fun LockSelfNavGraph(
    navController: NavHostController,
    repository: VaultRepository,
    crypto: VaultCryptoManager,
    preferences: AppPreferences,
    clipboardHelper: ClipboardHelper,
    onLaunchBiometricPrompt: () -> Unit
) {
    val startDest = when {
        !crypto.isVaultInitialized() -> NavRoute.Setup.route
        !crypto.isVaultUnlocked() -> NavRoute.Lock.route
        else -> NavRoute.VaultMain.route
    }

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable(NavRoute.Setup.route) {
            SetupScreen(
                crypto = crypto,
                onVaultCreated = {
                    navController.navigate(NavRoute.VaultMain.route) {
                        popUpTo(NavRoute.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoute.Lock.route) {
            LockScreen(
                crypto = crypto,
                preferences = preferences,
                onUnlocked = {
                    navController.navigate(NavRoute.VaultMain.route) {
                        popUpTo(NavRoute.Lock.route) { inclusive = true }
                    }
                },
                onLaunchBiometric = onLaunchBiometricPrompt
            )
        }

        composable(NavRoute.VaultMain.route) {
            val factory = ViewModelFactory(repository, crypto, preferences, clipboardHelper)
            val vm: VaultMainViewModel = viewModel(factory = factory)

            VaultMainScreen(
                viewModel = vm,
                onRecordClick = { recordId ->
                    navController.navigate(NavRoute.RecordDetail.createRoute(recordId))
                },
                onAddRecordClick = { type ->
                    navController.navigate(NavRoute.EditRecord.createRoute(recordId = null, type = type.name))
                },
                onSettingsClick = {
                    navController.navigate(NavRoute.Settings.route)
                },
                onLockClick = {
                    navController.navigate(NavRoute.Lock.route) {
                        popUpTo(NavRoute.VaultMain.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = NavRoute.RecordDetail.route,
            arguments = listOf(navArgument("recordId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong("recordId") ?: 0L
            val factory = ViewModelFactory(repository, crypto, preferences, clipboardHelper, recordId = recordId)
            val vm: RecordDetailViewModel = viewModel(factory = factory)

            RecordDetailScreen(
                viewModel = vm,
                onBackClick = { navController.popBackStack() },
                onEditClick = { id ->
                    val recType = vm.record.value?.type?.name ?: RecordType.PASSWORD.name
                    navController.navigate(NavRoute.EditRecord.createRoute(recordId = id, type = recType))
                },
                onHistoryClick = { id ->
                    navController.navigate(NavRoute.PasswordHistory.createRoute(id))
                },
                onRecordDeleted = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = NavRoute.EditRecord.route,
            arguments = listOf(
                navArgument("recordId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = "PASSWORD"
                }
            )
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getString("recordId")
            val recordId = rawId?.toLongOrNull()
            val initialType = backStackEntry.arguments?.getString("type") ?: "PASSWORD"

            val factory = ViewModelFactory(
                repository, crypto, preferences, clipboardHelper,
                recordId = recordId,
                initialTypeStr = initialType
            )
            val vm: EditRecordViewModel = viewModel(factory = factory)

            EditRecordScreen(
                viewModel = vm,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { savedId ->
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = NavRoute.PasswordHistory.route,
            arguments = listOf(navArgument("recordId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong("recordId") ?: 0L
            val factory = ViewModelFactory(repository, crypto, preferences, clipboardHelper, recordId = recordId)
            val vm: RecordDetailViewModel = viewModel(factory = factory)

            PasswordHistoryScreen(
                viewModel = vm,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(NavRoute.Settings.route) {
            val factory = ViewModelFactory(repository, crypto, preferences, clipboardHelper)
            val vm: SettingsViewModel = viewModel(factory = factory)

            SettingsScreen(
                viewModel = vm,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
