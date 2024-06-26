package net.primal.android.note.db

import androidx.room.Entity
import androidx.room.Index

@Entity(
    primaryKeys = [
        "postId",
        "userId",
    ],
    indices = [
        Index(value = ["postId"]),
        Index(value = ["userId"]),
    ],
)
data class NoteUserStats(
    val postId: String,
    val userId: String,
    val replied: Boolean = false,
    val liked: Boolean = false,
    val reposted: Boolean = false,
    val zapped: Boolean = false,
)
