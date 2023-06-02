package net.primal.android.feed.api

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.primal.android.core.forEachKey
import net.primal.android.db.PrimalDatabase
import net.primal.android.feed.api.model.FeedRequestBody
import net.primal.android.feed.db.FeedPost
import net.primal.android.feed.db.FeedPostDataCrossRef
import net.primal.android.nostr.ext.mapNotNullAsPost
import net.primal.android.nostr.ext.mapNotNullAsRepost
import net.primal.android.nostr.model.NostrEvent
import net.primal.android.nostr.model.NostrEventKind
import net.primal.android.nostr.model.primal.NostrPrimalEvent
import net.primal.android.nostr.processor.NostrEventProcessorFactory
import net.primal.android.nostr.processor.primal.NostrPrimalEventProcessorFactory
import timber.log.Timber

@ExperimentalPagingApi
class FeedRemoteMediator(
    private val feedDirective: String,
    private val feedApi: FeedApi,
    private val database: PrimalDatabase,
) : RemoteMediator<Int, FeedPost>() {

    override suspend fun initialize(): InitializeAction {
        InitializeAction.SKIP_INITIAL_REFRESH
        InitializeAction.LAUNCH_INITIAL_REFRESH
        return super.initialize()
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, FeedPost>
    ): MediatorResult {
        withContext(Dispatchers.IO) {
            val response = feedApi.getFeed(
                body = FeedRequestBody(
                    directive = feedDirective,
                    userPubKey = "9a500dccc084a138330a1d1b2be0d5e86394624325d25084d3eca164e7ea698a", // User hard-coded to Nostr Highlights
                    limit = 50,
                )
            )

            response.nostrEvents.apply {
                processShortTextNotesAndReposts(feedDirective = feedDirective)
                dropRepostsAndShortTextNotes().processAllNostrEvents()
            }

            response.primalEvents.apply {
                processAllNostrPrimalEvents()
            }

            Timber.d("PagingEvent=${response.paging}")
        }

        return MediatorResult.Success(endOfPaginationReached = true)
    }

    private suspend fun List<NostrEvent>.processShortTextNotesAndReposts(
        feedDirective: String
    ) {
        val mapByKind = groupBy { NostrEventKind.valueOf(it.kind) }
        val shortTextNoteEvents = mapByKind[NostrEventKind.ShortTextNote]
        val repostEvents = mapByKind[NostrEventKind.Reposts]
        database.withTransaction {
            val posts = shortTextNoteEvents?.mapNotNullAsPost() ?: emptyList()
            val reposts = repostEvents?.mapNotNullAsRepost() ?: emptyList()
            Timber.i("Received ${posts.size} posts and ${reposts.size} reposts..")

            database.posts().upsertAll(data = posts)
            database.reposts().upsertAll(data = reposts)

            val feedConnections = posts.map { it.postId } + reposts.map { it.postId }
            database.feedsConnections().connect(
                data = feedConnections.map { postId ->
                    FeedPostDataCrossRef(
                        feedDirective = feedDirective,
                        postId = postId
                    )
                }
            )
        }
    }

    private fun List<NostrEvent>.dropRepostsAndShortTextNotes(): List<NostrEvent> = filter {
        it.kind != NostrEventKind.Reposts.value && it.kind != NostrEventKind.ShortTextNote.value
    }

    private fun List<NostrEvent>.processAllNostrEvents() {
        val factory = NostrEventProcessorFactory(database = database)

        this.groupBy { NostrEventKind.valueOf(it.kind) }
            .forEachKey {
                val events = getValue(it)
                Timber.i("$it has ${events.size} nostr events.")
                factory.create(it)?.process(events = events)
            }
    }

    private fun List<NostrPrimalEvent>.processAllNostrPrimalEvents() {
        val factory = NostrPrimalEventProcessorFactory(database = database)
        this.groupBy { NostrEventKind.valueOf(it.kind) }
            .forEachKey {
                val events = getValue(it)
                Timber.i("$it has ${events.size} primal events.")
                factory.create(it)?.process(events = events)
            }
    }

}