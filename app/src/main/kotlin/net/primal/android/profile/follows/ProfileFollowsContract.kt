package net.primal.android.profile.follows

import net.primal.android.core.compose.profile.model.UserProfileItemUi
import net.primal.android.profile.domain.ProfileFollowsType

interface ProfileFollowsContract {
    data class UiState(
        val userId: String,
        val followsType: ProfileFollowsType,
        val profileName: String? = null,
        val loading: Boolean = true,
        val userFollowing: Set<String> = emptySet(),
        val error: FollowsError? = null,
        val users: List<UserProfileItemUi> = emptyList(),
    ) {
        sealed class FollowsError {
            data class FailedToFollowUser(val cause: Throwable) : FollowsError()
            data class FailedToUnfollowUser(val cause: Throwable) : FollowsError()
            data class MissingRelaysConfiguration(val cause: Throwable) : FollowsError()
        }
    }

    sealed class UiEvent {
        data class FollowProfile(val profileId: String) : UiEvent()
        data class UnfollowProfile(val profileId: String) : UiEvent()
        data object DismissError : UiEvent()
        data object ReloadData : UiEvent()
    }
}
