package com.iptvtv.player.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.Text
import com.iptvtv.player.di.AppContainer
import com.iptvtv.player.di.AppViewModelFactory
import com.iptvtv.player.ui.channels.ChannelListScreen
import com.iptvtv.player.ui.channels.ChannelListViewModel
import com.iptvtv.player.ui.player.PlayerScreen
import com.iptvtv.player.ui.player.PlayerViewModel
import com.iptvtv.player.ui.settings.SettingsScreen
import com.iptvtv.player.ui.settings.SettingsViewModel
import com.iptvtv.player.ui.sources.AddEditSourceScreen
import com.iptvtv.player.ui.sources.AddEditSourceViewModel
import com.iptvtv.player.ui.sources.SourcesScreen
import com.iptvtv.player.ui.sources.SourcesViewModel
import kotlinx.coroutines.flow.first

/**
 * Resolves where the app should open (a saved default channel, the active source's channel
 * list, or the sources screen when nothing is configured yet) and then hosts every screen.
 */
@Composable
fun AppNavHost(container: AppContainer) {
    val viewModelFactory = remember { AppViewModelFactory(container) }
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val defaultChannelId = container.settingsRepository.observeDefaultChannelId().first()
        val defaultChannelRoute = defaultChannelId?.let { id ->
            container.channelRepository.getChannel(id)?.let { channel -> Routes.player(channel.sourceId, channel.id) }
        }
        startDestination = defaultChannelRoute ?: run {
            val activeSourceId = container.settingsRepository.observeActiveSourceId().first()
            val activeSource = activeSourceId?.let { container.sourceRepository.getSource(it) }
            if (activeSource != null) Routes.channelList(activeSource.id) else Routes.SOURCES
        }
    }

    val destination = startDestination
    if (destination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Carregando...")
        }
        return
    }

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = destination) {
        composable(Routes.SOURCES) {
            val vm: SourcesViewModel = viewModel(factory = viewModelFactory)
            SourcesScreen(
                viewModel = vm,
                onAddSource = { type -> navController.navigate(Routes.addSource(type)) },
                onEditSource = { sourceId -> navController.navigate(Routes.editSource(sourceId)) },
                onOpenChannelList = { sourceId -> navController.navigate(Routes.channelList(sourceId)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            route = Routes.ADD_EDIT_SOURCE_PATTERN,
            arguments = listOf(
                navArgument("sourceId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("sourceType") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { backStackEntry ->
            val vm: AddEditSourceViewModel = viewModel(factory = viewModelFactory)
            val rawSourceId = backStackEntry.arguments?.getLong("sourceId") ?: -1L
            val rawSourceType = backStackEntry.arguments?.getString("sourceType").orEmpty()
            AddEditSourceScreen(
                viewModel = vm,
                sourceId = rawSourceId.takeIf { it != -1L },
                sourceType = rawSourceType.takeIf { it.isNotBlank() },
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.CHANNEL_LIST_PATTERN,
            arguments = listOf(navArgument("sourceId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val sourceId = backStackEntry.arguments?.getLong("sourceId") ?: return@composable
            val vm: ChannelListViewModel = viewModel(factory = viewModelFactory)
            ChannelListScreen(
                viewModel = vm,
                sourceId = sourceId,
                onChannelClick = { channelId -> navController.navigate(Routes.player(sourceId, channelId)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            route = Routes.PLAYER_PATTERN,
            arguments = listOf(
                navArgument("sourceId") { type = NavType.LongType },
                navArgument("channelId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val sourceId = backStackEntry.arguments?.getLong("sourceId") ?: return@composable
            val channelId = backStackEntry.arguments?.getLong("channelId") ?: return@composable
            val vm: PlayerViewModel = viewModel(factory = viewModelFactory)
            PlayerScreen(
                viewModel = vm,
                sourceId = sourceId,
                initialChannelId = channelId,
                onExit = {
                    val poppedToChannelList = navController.popBackStack(Routes.channelList(sourceId), false)
                    if (!poppedToChannelList) {
                        // The player was the start destination (a default channel skipped the
                        // list on launch) - there is no channel-list entry to pop back to yet,
                        // so navigate to it and drop the player entry from the back stack.
                        navController.navigate(Routes.channelList(sourceId)) {
                            popUpTo(Routes.PLAYER_PATTERN) { inclusive = true }
                        }
                    }
                },
            )
        }

        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(factory = viewModelFactory)
            SettingsScreen(
                viewModel = vm,
                onManageSources = { navController.navigate(Routes.SOURCES) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
