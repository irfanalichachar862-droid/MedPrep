package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MedPrepConstants
import com.example.data.SubscriptionPlanTier
import com.example.data.SubscriptionRequest
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.components.JazzCashPaymentAccountBanner
import com.example.ui.components.PlanTierBadge
import com.example.ui.theme.LocalMedPrepColors
import com.example.ui.theme.MonoFontFamily
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AdminSubscriptionsManagerTab(
    allUsersState: UiState<List<UserProfile>>,
    allRequestsState: UiState<List<SubscriptionRequest>>,
    isMutating: Boolean,
    onReviewRequest: (SubscriptionRequest, Boolean, String) -> Unit,
    onSetUserPlan: (String, String, String) -> Unit,
    onDeleteRequest: (String) -> Unit,
    onPostMessage: (String) -> Unit
) {
    val ext = LocalMedPrepColors.current
    val users = (allUsersState as? UiState.Success)?.data.orEmpty()
    val requests = (allRequestsState as? UiState.Success)?.data.orEmpty()

    val pendingRequests = remember(requests) { requests.filter { it.status == "pending" } }
    val reviewedRequests = remember(requests) { requests.filter { it.status != "pending" } }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var planFilter by rememberSaveable { mutableStateOf("all") }
    var rejectingTarget by remember { mutableStateOf<SubscriptionRequest?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    val filteredUsers = remember(users, searchQuery, planFilter) {
        val q = searchQuery.trim()
        users.filter { u ->
            val userTier = SubscriptionPlanTier.fromKey(u.subscriptionPlan)
            val matchesPlan = planFilter == "all" || userTier.key == planFilter
            val matchesQuery = q.isEmpty() ||
                u.displayName.contains(q, ignoreCase = true) ||
                u.email.orEmpty().contains(q, ignoreCase = true) ||
                u.contactEmail.orEmpty().contains(q, ignoreCase = true) ||
                u.phoneNumber.orEmpty().contains(q, ignoreCase = true)
            matchesPlan && matchesQuery
        }
    }

    val basicCount = remember(users) { users.count { SubscriptionPlanTier.fromKey(it.subscriptionPlan) == SubscriptionPlanTier.BASIC } }
    val advanceCount = remember(users) { users.count { SubscriptionPlanTier.fromKey(it.subscriptionPlan) == SubscriptionPlanTier.ADVANCE } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Summary KPIs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Subscriptions & Plan Access",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubscriptionKpiBox(
                        label = "PENDING",
                        value = "${pendingRequests.size}",
                        bg = if (pendingRequests.isNotEmpty()) ext.warningContainer else MaterialTheme.colorScheme.surfaceVariant,
                        fg = if (pendingRequests.isNotEmpty()) ext.onWarningContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    SubscriptionKpiBox(
                        label = "BASIC (499)",
                        value = "$basicCount",
                        bg = MaterialTheme.colorScheme.primaryContainer,
                        fg = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    SubscriptionKpiBox(
                        label = "ADVANCE (999)",
                        value = "$advanceCount",
                        bg = ext.successContainer,
                        fg = ext.onSuccessContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                JazzCashPaymentAccountBanner(
                    onCopied = { onPostMessage("Copied JazzCash IBAN: ${MedPrepConstants.JAZZCASH_IBAN}") }
                )
            }
        }

        // Section 1: Pending JazzCash Payment Verification Requests
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Pending JazzCash Requests (${pendingRequests.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (allRequestsState is UiState.Loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (allRequestsState is UiState.Error) {
            item {
                Text(
                    text = allRequestsState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else if (pendingRequests.isEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No pending JazzCash payment requests right now. When students submit a Transaction ID for Basic (PKR 499) or Advance (PKR 999), it appears here for your approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(pendingRequests, key = { "pending_${it.id}" }) { req ->
                val tier = SubscriptionPlanTier.fromKey(req.requestedPlan)
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, ext.warning.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pending_request_${req.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = req.userName.ifBlank { "Student" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (req.userEmail.isNotBlank()) {
                                    Text(
                                        text = req.userEmail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            PlanTierBadge(tier = tier)
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${req.paymentMethod} • PKR ${req.amountPkr}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Transaction ID (TID): ${req.transactionRef}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily),
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (!req.senderPhone.isNullOrBlank()) {
                                    Text(
                                        text = "Sender Mobile: ${req.senderPhone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "Submitted: ${formatReqDate(req.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onReviewRequest(req, true, "Payment verified via JazzCash") },
                                enabled = !isMutating,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ext.success,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("admin_approve_req_${req.id}")
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Approve & Grant")
                            }
                            OutlinedButton(
                                onClick = {
                                    rejectingTarget = req
                                    rejectReason = ""
                                },
                                enabled = !isMutating,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(0.75f)
                                    .height(48.dp)
                                    .testTag("admin_reject_req_${req.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reject", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Direct Admin Plan Access Control for All Users (Give or Remove Access Anytime)
        item {
            Column(
                modifier = Modifier.padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Manage Student Plans (${users.size} Accounts)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Grant Basic/Advance access or remove a user's plan back to Free anytime.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it.take(80) },
                    placeholder = { Text("Search student by name, email, or phone…") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_plans_user_search")
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "all" to "All (${users.size})",
                        "free" to "Free",
                        "basic" to "Basic (499)",
                        "advance" to "Advance (999)"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = planFilter == key,
                            onClick = { planFilter = key },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        when (allUsersState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(
                        text = allUsersState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            is UiState.Success -> {
                if (filteredUsers.isEmpty()) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No matching student accounts found.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(filteredUsers, key = { "user_${it.id}" }) { user ->
                        val activeTier = SubscriptionPlanTier.fromKey(user.subscriptionPlan)
                        val targetUid = user.id.ifBlank { user.userId }
                        val safeName = user.displayName.ifBlank { user.email ?: "Student" }

                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_user_plan_card_$targetUid")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = safeName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!user.email.isNullOrBlank()) {
                                            Text(
                                                text = user.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (!user.phoneNumber.isNullOrBlank()) {
                                            Text(
                                                text = user.phoneNumber,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    PlanTierBadge(tier = activeTier)
                                }

                                // One-tap Grant / Revoke Plan Controls
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PlanControlPill(
                                        label = if (activeTier == SubscriptionPlanTier.FREE) "Free ✓" else "Remove (Free)",
                                        selected = activeTier == SubscriptionPlanTier.FREE,
                                        isDestructive = activeTier != SubscriptionPlanTier.FREE,
                                        enabled = !isMutating && activeTier != SubscriptionPlanTier.FREE,
                                        tag = "admin_set_free_$targetUid",
                                        onClick = { onSetUserPlan(targetUid, safeName, "free") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    PlanControlPill(
                                        label = if (activeTier == SubscriptionPlanTier.BASIC) "Basic ✓" else "Grant Basic",
                                        selected = activeTier == SubscriptionPlanTier.BASIC,
                                        isDestructive = false,
                                        enabled = !isMutating && activeTier != SubscriptionPlanTier.BASIC,
                                        tag = "admin_set_basic_$targetUid",
                                        onClick = { onSetUserPlan(targetUid, safeName, "basic") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    PlanControlPill(
                                        label = if (activeTier == SubscriptionPlanTier.ADVANCE) "Advance ✓" else "Grant Advance",
                                        selected = activeTier == SubscriptionPlanTier.ADVANCE,
                                        isDestructive = false,
                                        enabled = !isMutating && activeTier != SubscriptionPlanTier.ADVANCE,
                                        tag = "admin_set_advance_$targetUid",
                                        onClick = { onSetUserPlan(targetUid, safeName, "advance") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Payment Request History
        if (reviewedRequests.isNotEmpty()) {
            item {
                Text(
                    text = "Reviewed Payment History (${reviewedRequests.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            items(reviewedRequests.take(20), key = { "history_${it.id}" }) { req ->
                val isApproved = req.status == "approved"
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = if (isApproved) ext.success else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${req.userName.ifBlank { req.userEmail }} • ${req.requestedPlan.uppercase()} (PKR ${req.amountPkr})",
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "TID: ${req.transactionRef} • ${req.status.uppercase()} • ${formatReqDate(req.updatedAt ?: req.createdAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!req.adminNote.isNullOrBlank()) {
                                Text(
                                    text = "Note: ${req.adminNote}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { onDeleteRequest(req.id) }) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete history record",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Reject Dialog with optional Admin Note
    val targetToReject = rejectingTarget
    if (targetToReject != null) {
        AlertDialog(
            onDismissRequest = { rejectingTarget = null },
            title = { Text("Reject JazzCash Payment?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Reject ${targetToReject.userName}'s request for ${targetToReject.requestedPlan.uppercase()} (TID: ${targetToReject.transactionRef})?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it.take(200) },
                        label = { Text("Reason (optional)") },
                        placeholder = { Text("e.g. Transaction ID not received in JazzCash") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReviewRequest(targetToReject, false, rejectReason.ifBlank { "Payment could not be verified." })
                        rejectingTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Reject Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SubscriptionKpiBox(
    label: String,
    value: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bg,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.85f), maxLines = 1)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
    }
}

@Composable
private fun PlanControlPill(
    label: String,
    selected: Boolean,
    isDestructive: Boolean,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        selected -> MaterialTheme.colorScheme.primary
        isDestructive -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        isDestructive -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = bg,
        modifier = modifier
            .height(42.dp)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatReqDate(timestamp: Timestamp?): String {
    val date = (timestamp ?: Timestamp.now()).toDate()
    return SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(date)
}
