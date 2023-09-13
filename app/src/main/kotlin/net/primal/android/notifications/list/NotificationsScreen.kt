package net.primal.android.notifications.list

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import net.primal.android.R
import net.primal.android.core.compose.ListLoading
import net.primal.android.core.compose.ListNoContent
import net.primal.android.core.compose.PrimalTopAppBar
import net.primal.android.core.compose.PrimalTopLevelDestination
import net.primal.android.core.compose.icons.PrimalIcons
import net.primal.android.core.compose.icons.primaliconpack.AvatarDefault
import net.primal.android.drawer.DrawerScreenDestination
import net.primal.android.drawer.PrimalDrawerScaffold
import net.primal.android.notifications.domain.NotificationType
import net.primal.android.notifications.list.ui.UserFollowedYouListItem
import net.primal.android.notifications.list.ui.UserUnfollowedYouListItem

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onProfileClick: (String) -> Unit,
    onNoteClick: (String) -> Unit,
    onTopLevelDestinationChanged: (PrimalTopLevelDestination) -> Unit,
    onDrawerScreenClick: (DrawerScreenDestination) -> Unit,
) {
    val uiState = viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.setEvent(NotificationsContract.UiEvent.NotificationsSeen)
    }

    NotificationsScreen(
        state = uiState.value,
        onProfileClick = onProfileClick,
        onNoteClick = onNoteClick,
        onPrimaryDestinationChanged = onTopLevelDestinationChanged,
        onDrawerDestinationClick = onDrawerScreenClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    state: NotificationsContract.UiState,
    onProfileClick: (String) -> Unit,
    onNoteClick: (String) -> Unit,
    onPrimaryDestinationChanged: (PrimalTopLevelDestination) -> Unit,
    onDrawerDestinationClick: (DrawerScreenDestination) -> Unit,
) {
    val uiScope = rememberCoroutineScope()
    val drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed)
    val listState = rememberLazyListState()

    PrimalDrawerScaffold(
        drawerState = drawerState,
        activeDestination = PrimalTopLevelDestination.Notifications,
        onActiveDestinationClick = { uiScope.launch { listState.animateScrollToItem(0) } },
        onPrimaryDestinationChanged = onPrimaryDestinationChanged,
        onDrawerDestinationClick = onDrawerDestinationClick,
        badges = state.badges,
        topBar = {
            PrimalTopAppBar(
                title = stringResource(id = R.string.notifications_title),
                avatarUrl = state.activeAccountAvatarUrl,
                navigationIcon = PrimalIcons.AvatarDefault,
                onNavigationIconClick = {
                    uiScope.launch { drawerState.open() }
                },
                scrollBehavior = it,
            )
        },
        content = { paddingValues ->
            LazyColumn(
                contentPadding = paddingValues
            ) {
                items(
                    items = state.notifications,
                    key = { "${it.data.type};${it.data.createdAt};${it.data.ownerId}" },
                    contentType = { it.data.type },
                ) {
                    when (it.data.type) {
                        NotificationType.NEW_USER_FOLLOWED_YOU -> UserFollowedYouListItem(
                            notifications = listOf(it),
                            onProfileClick = onProfileClick,
                        )

                        NotificationType.USER_UNFOLLOWED_YOU -> UserUnfollowedYouListItem(
                            notifications = listOf(it),
                            onProfileClick = onProfileClick,
                        )

                        NotificationType.YOUR_POST_WAS_ZAPPED -> Unit
                        NotificationType.YOUR_POST_WAS_LIKED -> Unit
                        NotificationType.YOUR_POST_WAS_REPOSTED -> Unit
                        NotificationType.YOUR_POST_WAS_REPLIED_TO -> Unit // Skip grouping

                        NotificationType.YOU_WERE_MENTIONED_IN_POST -> Unit
                        NotificationType.YOUR_POST_WAS_MENTIONED_IN_POST -> Unit

                        NotificationType.POST_YOU_WERE_MENTIONED_IN_WAS_ZAPPED -> Unit
                        NotificationType.POST_YOU_WERE_MENTIONED_IN_WAS_LIKED -> Unit
                        NotificationType.POST_YOU_WERE_MENTIONED_IN_WAS_REPOSTED -> Unit
                        NotificationType.POST_YOU_WERE_MENTIONED_IN_WAS_REPLIED_TO -> Unit // Skip grouping

                        NotificationType.POST_YOUR_POST_WAS_MENTIONED_IN_WAS_ZAPPED -> Unit
                        NotificationType.POST_YOUR_POST_WAS_MENTIONED_IN_WAS_LIKED -> Unit
                        NotificationType.POST_YOUR_POST_WAS_MENTIONED_IN_WAS_REPOSTED -> Unit
                        NotificationType.POST_YOUR_POST_WAS_MENTIONED_IN_WAS_REPLIED_TO -> Unit // Skip grouping
                    }
                }

                if (state.notifications.isEmpty()) {
                    item {
                        if (state.loading) {
                            ListLoading(
                                modifier = Modifier.fillParentMaxSize(),
                            )
                        } else {
                            ListNoContent(
                                modifier = Modifier.fillParentMaxSize(),
                                refreshButtonVisible = false,
                                noContentText = "No notifications."
                            )
                        }
                    }
                }
            }
        },
    )
}

