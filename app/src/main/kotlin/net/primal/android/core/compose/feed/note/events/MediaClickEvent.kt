package net.primal.android.core.compose.feed.note.events

import net.primal.android.attachments.domain.NoteAttachmentType

data class MediaClickEvent(
    val noteId: String,
    val noteAttachmentType: NoteAttachmentType,
    val mediaUrl: String,
    val positionMs: Long = 0,
)
