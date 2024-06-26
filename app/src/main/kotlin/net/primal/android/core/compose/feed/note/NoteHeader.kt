package net.primal.android.core.compose.feed.note

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import net.primal.android.LocalContentDisplaySettings
import net.primal.android.attachments.domain.CdnImage
import net.primal.android.core.compose.AvatarThumbnail
import net.primal.android.core.compose.NostrUserText
import net.primal.android.core.compose.ReplyingToText
import net.primal.android.core.compose.WrappedContentWithSuffix
import net.primal.android.core.compose.asBeforeNowFormat
import net.primal.android.core.utils.formatNip05Identifier
import net.primal.android.theme.AppTheme
import net.primal.android.theme.PrimalTheme
import net.primal.android.theme.domain.PrimalTheme
import net.primal.android.user.domain.ContentDisplaySettings

@Composable
fun FeedNoteHeader(
    modifier: Modifier = Modifier,
    authorDisplayName: String,
    singleLine: Boolean = false,
    postTimestamp: Instant? = null,
    authorAvatarSize: Dp = 42.dp,
    authorAvatarVisible: Boolean = true,
    authorAvatarCdnImage: CdnImage? = null,
    authorInternetIdentifier: String? = null,
    replyToAuthor: String? = null,
    label: String? = authorInternetIdentifier,
    labelStyle: TextStyle? = null,
    onAuthorAvatarClick: (() -> Unit)? = null,
) {
    val displaySettings = LocalContentDisplaySettings.current
    val topRowTextStyle = AppTheme.typography.bodyMedium.copy(
        fontSize = displaySettings.noteAppearance.usernameSize,
        lineHeight = displaySettings.noteAppearance.usernameSize,
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (authorAvatarVisible) {
            AvatarThumbnail(
                avatarCdnImage = authorAvatarCdnImage,
                avatarSize = authorAvatarSize,
                onClick = onAuthorAvatarClick,
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = if (authorAvatarVisible) 8.dp else 0.dp),
        ) {
            val identifier = if (!singleLine) "" else authorInternetIdentifier?.formatNip05Identifier() ?: ""
            val suffixText = buildAnnotatedString {
                append(
                    AnnotatedString(
                        text = identifier,
                        spanStyle = SpanStyle(
                            color = AppTheme.extraColorScheme.onSurfaceVariantAlt2,
                            fontSize = displaySettings.noteAppearance.usernameSize,
                        ),
                    ),
                )
            }

            WrappedContentWithSuffix(
                wrappedContent = {
                    NostrUserText(
                        displayName = authorDisplayName,
                        internetIdentifier = authorInternetIdentifier,
                        annotatedStringSuffixBuilder = {
                            append(suffixText)
                        },
                        style = topRowTextStyle,
                        internetIdentifierBadgeSize = topRowTextStyle.fontSize.value.dp,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                suffixFixedContent = {
                    if (postTimestamp != null) {
                        Text(
                            text = " • ${postTimestamp.asBeforeNowFormat()}",
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            style = topRowTextStyle,
                            fontSize = (displaySettings.noteAppearance.usernameSize.value).sp,
                            color = AppTheme.extraColorScheme.onSurfaceVariantAlt2,
                        )
                    }
                },
            )

            if (!label.isNullOrEmpty() && !singleLine) {
                Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = labelStyle ?: topRowTextStyle,
                    color = AppTheme.extraColorScheme.onSurfaceVariantAlt2,
                )
            }

            if (!replyToAuthor.isNullOrEmpty()) {
                ReplyingToText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    replyToUsername = replyToAuthor,
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewLightNoteHeader() {
    CompositionLocalProvider(LocalContentDisplaySettings provides ContentDisplaySettings()) {
        PrimalTheme(
            primalTheme = PrimalTheme.Sunrise,
        ) {
            Surface {
                FeedNoteHeader(
                    modifier = Modifier.fillMaxWidth(),
                    authorDisplayName = "Donald Duck",
                    postTimestamp = Instant.now().minusSeconds(3600.seconds.inWholeSeconds),
                    authorInternetIdentifier = "donald@the.duck",
                    onAuthorAvatarClick = {},
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewLightSingleNoteHeader() {
    CompositionLocalProvider(LocalContentDisplaySettings provides ContentDisplaySettings()) {
        PrimalTheme(
            primalTheme = PrimalTheme.Sunrise,
        ) {
            Surface {
                FeedNoteHeader(
                    modifier = Modifier.fillMaxWidth(),
                    authorDisplayName = "Donald Duck",
                    postTimestamp = Instant.now().minusSeconds(3600.seconds.inWholeSeconds),
                    singleLine = true,
                    authorInternetIdentifier = "donald@the.duck",
                    onAuthorAvatarClick = {},
                    replyToAuthor = "alex",
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewDarkNoteHeader() {
    CompositionLocalProvider(LocalContentDisplaySettings provides ContentDisplaySettings()) {
        PrimalTheme(
            primalTheme = PrimalTheme.Sunset,
        ) {
            Surface {
                FeedNoteHeader(
                    modifier = Modifier.fillMaxWidth(),
                    authorDisplayName = "Donald Duck",
                    postTimestamp = Instant.now().minusSeconds(3600.seconds.inWholeSeconds),
                    authorAvatarVisible = false,
                    authorInternetIdentifier = "donald@the.duck",
                    onAuthorAvatarClick = {},
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewDarkSingleLineNoAvatarNoteHeader() {
    CompositionLocalProvider(LocalContentDisplaySettings provides ContentDisplaySettings()) {
        PrimalTheme(
            primalTheme = PrimalTheme.Sunset,
        ) {
            Surface {
                FeedNoteHeader(
                    modifier = Modifier.fillMaxWidth(),
                    authorDisplayName = "Donald Duck",
                    postTimestamp = Instant.now().minusSeconds(3600.seconds.inWholeSeconds),
                    singleLine = true,
                    authorAvatarVisible = false,
                    authorInternetIdentifier = "donald@the.duck",
                    onAuthorAvatarClick = {},
                    replyToAuthor = "alex",
                )
            }
        }
    }
}
