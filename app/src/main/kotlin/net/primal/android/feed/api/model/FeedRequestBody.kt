package net.primal.android.feed.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedRequestBody(
    @SerialName("directive") val directive: String,
    @SerialName("user_pubkey") val userPubKey: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("limit") val limit: Int? = null,
    @SerialName("until") val until: Long? = null,
    @SerialName("since") val since: Long? = null,
    @SerialName("order") val order: String? = null,
)
