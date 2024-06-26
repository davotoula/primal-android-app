package net.primal.android.notifications.db

import androidx.room.Embedded
import androidx.room.Relation
import net.primal.android.attachments.db.NoteAttachment
import net.primal.android.attachments.db.NoteNostrUri
import net.primal.android.feed.db.PostData
import net.primal.android.note.db.NoteStats
import net.primal.android.note.db.NoteUserStats
import net.primal.android.profile.db.ProfileData

data class Notification(
    @Embedded
    val data: NotificationData,

    @Relation(entityColumn = "ownerId", parentColumn = "actionUserId")
    val actionByUser: ProfileData?,

    @Relation(entityColumn = "postId", parentColumn = "actionPostId")
    val actionPost: PostData? = null,

    @Relation(entityColumn = "postId", parentColumn = "actionPostId")
    val actionNoteStats: NoteStats? = null,

    @Relation(entityColumn = "postId", parentColumn = "actionPostId")
    val actionPostUserStats: NoteUserStats? = null,

    @Relation(entityColumn = "eventId", parentColumn = "actionPostId")
    val actionPostNoteAttachments: List<NoteAttachment> = emptyList(),

    @Relation(entityColumn = "postId", parentColumn = "actionPostId")
    val actionPostNostrUris: List<NoteNostrUri> = emptyList(),
)
