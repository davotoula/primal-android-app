package net.primal.android.core.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import net.primal.android.core.compose.icons.PrimalIcons
import net.primal.android.core.compose.icons.primaliconpack.Verified
import net.primal.android.core.utils.isPrimalIdentifier
import net.primal.android.theme.AppTheme
import net.primal.android.theme.PrimalTheme

@Composable
fun NostrUserText(
    displayName: String,
    verifiedBadge: Boolean,
    internetIdentifier: String?,
    modifier: Modifier = Modifier,
    annotatedStringPrefixBuilder: (AnnotatedString.Builder.() -> Unit)? = null,
    annotatedStringSuffixBuilder: (AnnotatedString.Builder.() -> Unit)? = null,
) {

    val titleText = buildAnnotatedString {
        annotatedStringPrefixBuilder?.invoke(this)
        append(
            AnnotatedString(
                text = displayName,
                spanStyle = SpanStyle(
                    color = AppTheme.colorScheme.onSurface,
                    fontStyle = AppTheme.typography.titleMedium.fontStyle,
                    fontWeight = FontWeight.Bold
                )
            )
        )
        if (verifiedBadge) {
            appendInlineContent("verifiedBadge", "[badge]")
        }
        annotatedStringSuffixBuilder?.invoke(this)
    }

    val inlineContent = mapOf(
        "verifiedBadge" to InlineTextContent(
            placeholder = Placeholder(
                24.sp, 24.sp, PlaceholderVerticalAlign.TextCenter
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    imageVector = PrimalIcons.Verified,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        color = if (internetIdentifier.isPrimalIdentifier()) {
                            AppTheme.colorScheme.secondary
                        } else {
                            AppTheme.extraColorScheme.onSurfaceVariantAlt2
                        }
                    )
                )
            }
        }
    )

    Text(
        modifier = modifier,
        text = titleText,
        textAlign = TextAlign.Start,
        inlineContent = inlineContent,
        maxLines = 2,
    )

}


@Preview
@Composable
fun PreviewNostrUserTextWithPrimalBadge() {
    PrimalTheme {
        Surface {
            NostrUserText(
                displayName = "Nostr Adamus",
                verifiedBadge = true,
                internetIdentifier = "adam@primal.net",
                annotatedStringSuffixBuilder = {
                    append("| 42 y. ago")
                }
            )
        }
    }
}

@Preview
@Composable
fun PreviewNostrUserTextWithRandomBadge() {
    PrimalTheme {
        Surface {
            NostrUserText(
                displayName = "Nostr Adamus",
                verifiedBadge = true,
                internetIdentifier = "adam@nostr.com",
                annotatedStringSuffixBuilder = {
                    append("| 42 y. ago")
                }
            )
        }
    }
}

@Preview
@Composable
fun PreviewNostrUserTextWithoutBadge() {
    PrimalTheme {
        Surface {
            NostrUserText(
                displayName = "Nostr Adamus",
                verifiedBadge = false,
                internetIdentifier = null,
                annotatedStringSuffixBuilder = {
                    append(" | 42 y. ago")
                }
            )
        }
    }
}