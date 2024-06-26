package net.primal.android.note.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteZapDao {

    @Insert
    fun insert(data: NoteZapData)

    @Query(
        """
            DELETE FROM NoteZapData 
            WHERE zapSenderId = :senderId AND zapReceiverId = :receiverId AND noteId = :noteId 
                AND (zapRequestAt = :timestamp OR zapReceiptAt = :timestamp)
        """,
    )
    fun delete(
        senderId: String,
        receiverId: String,
        noteId: String,
        timestamp: Long,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(data: List<NoteZapData>)

    @Transaction
    @Query(
        """
            SELECT * FROM NoteZapData WHERE noteId = :noteId
            ORDER BY CAST(amountInBtc AS REAL) DESC, zapReceiptAt ASC
            LIMIT 10
        """,
    )
    fun observeTopZaps(noteId: String): Flow<List<NoteZap>>

    @Transaction
    @Query(
        """
            SELECT * FROM NoteZapData WHERE noteId = :noteId
            ORDER BY CAST(amountInBtc AS REAL) DESC, zapReceiptAt ASC
        """,
    )
    fun pagedNoteZaps(noteId: String): PagingSource<Int, NoteZap>
}
