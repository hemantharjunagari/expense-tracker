package com.spendless.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendless.app.core.data.database.entities.Category
import com.spendless.app.core.data.database.entities.Transaction
import com.spendless.app.core.data.database.entities.TransactionType
import com.spendless.app.ui.theme.MonoAmount
import java.text.SimpleDateFormat
import java.util.*

/**
 * Single transaction list item with Nothing OS aesthetic.
 * Shows merchant icon (emoji from category), merchant name,
 * amount (monospace), and timestamp.
 */
@Composable
fun TransactionItem(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    val isMissingContactInfo = (transaction.status == com.spendless.app.core.data.database.entities.TransactionStatus.LENT || 
                                transaction.status == com.spendless.app.core.data.database.entities.TransactionStatus.BORROWED ||
                                transaction.type == com.spendless.app.core.data.database.entities.TransactionType.LENT ||
                                transaction.type == com.spendless.app.core.data.database.entities.TransactionType.BORROWED) && 
                               (transaction.contactName.isNullOrBlank() || transaction.contactPhone.isNullOrBlank())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isMissingContactInfo) {
                    Modifier
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category emoji icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = transaction.category.emoji,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Merchant + date
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.merchant,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateFormat.format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isMissingContactInfo) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "⚠️ Missing Name/Phone",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Amount (right aligned)
        Column(horizontalAlignment = Alignment.End) {
            val prefix = if (transaction.type == TransactionType.CREDIT) "+ ₹" else "- ₹"
            val amountColor = if (transaction.type == TransactionType.CREDIT)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant

            Text(
                text = "$prefix${formatCurrency(transaction.amount)}",
                style = MonoAmount.copy(
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                ),
                color = amountColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = transaction.category.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TransactionDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 20.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}
