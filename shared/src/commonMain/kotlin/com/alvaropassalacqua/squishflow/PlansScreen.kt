package com.alvaropassalacqua.squishflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.DiscountPaymentMode
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import kotlinx.coroutines.launch

/**
 * The plans, in the product's own clothes.
 *
 * RevenueCat's paywall template is the fastest way to a working purchase, and
 * this used to use it: a red sheet titled "RevenueCat Paywalls" on top of a
 * product built around a dark ground, a light typeface and a pale-green body.
 * The plans are the one screen where somebody decides whether they trust the
 * app, and a screen that does not look like the app is a poor witness.
 *
 * The SDK still does everything that matters — offerings, purchase, restore,
 * the entitlement — and the screen only draws. Honesty rules from the rest of
 * the product hold here: the bodies on the screen are the ones for sale, the
 * footer says they can all be earned, and there is no countdown, no "most
 * popular" badge and no pre-ticked anything beyond a default selection.
 */
@Composable
internal fun PlansScreen(
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit,
) {
    var packages by remember { mutableStateOf<List<Package>?>(null) }
    var failedToLoad by remember { mutableStateOf(false) }
    var selected by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (!RevenueCatManager.isConfigured.value) {
            failedToLoad = true
            return@LaunchedEffect
        }
        runCatching { Purchases.sharedInstance.awaitOfferings() }
            .onSuccess { offerings ->
                val list = offerings.current?.availablePackages.orEmpty()
                packages = list
                selected = defaultPlanIndex(list.map { it.packageType })
                failedToLoad = list.isEmpty()
            }
            .onFailure { failedToLoad = true }
    }

    fun finish(isPremiumNow: Boolean, restoring: Boolean) {
        when {
            isPremiumNow -> onUnlocked()
            restoring -> notice = "Nothing to restore on this account."
            else -> notice = "The store confirmed it, but nothing unlocked. Try Restore purchases."
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF090B0A)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss, enabled = !busy) {
                    Text("Not now", color = Color.White.copy(alpha = 0.72f))
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SquishyMaterial.entries.filter { it.isPro }.forEach { body ->
                    MaterialOrb(
                        material = body,
                        tint = SquishyState.RELAXING.bodyTint(),
                        shade = SquishyState.RELAXING.bodyShadow(),
                        modifier = Modifier.size(46.dp),
                    )
                }
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "Pick a pace.",
                color = Color.White,
                fontSize = 34.sp,
                lineHeight = 41.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Same shelf either way. This just skips the wait.",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 15.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(34.dp))

            when {
                packages == null && !failedToLoad -> Box(
                    Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = Color.White.copy(alpha = 0.4f), strokeWidth = 2.dp) }

                failedToLoad -> Text(
                    "The store did not answer. Everything here can still be earned with focus.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 40.dp),
                )

                else -> packages.orEmpty().forEachIndexed { index, pkg ->
                    val price = pkg.storeProduct.price
                    val trial = pkg.storeProduct.introductoryDiscount
                        ?.takeIf { it.paymentMode == DiscountPaymentMode.FREE_TRIAL }
                        ?.let { trialLine(it.subscriptionPeriod.value, it.subscriptionPeriod.unit) }
                    val line = planLine(pkg.packageType, price.formatted, price.amountMicros)
                        .let { if (trial != null) it.copy(note = "$trial, then ${it.note.replaceFirstChar(Char::lowercase)}") else it }
                    PlanRow(
                        line = line,
                        price = price.formatted,
                        isSelected = index == selected,
                        enabled = !busy,
                        onClick = { selected = index },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            Spacer(Modifier.height(22.dp))

            notice?.let {
                Text(it, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
            }

            val chosen = packages?.getOrNull(selected)
            Button(
                onClick = {
                    val pkg = chosen ?: return@Button
                    busy = true
                    notice = null
                    scope.launch {
                        try {
                            val result = Purchases.sharedInstance.awaitPurchase(pkg)
                            RevenueCatManager.apply(result.customerInfo)
                            finish(RevenueCatManager.isPremium.value, restoring = false)
                        } catch (e: PurchasesTransactionException) {
                            if (!e.userCancelled) notice = "That did not go through. Nothing was charged."
                        } catch (e: Exception) {
                            notice = "That did not go through. Nothing was charged."
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = chosen != null && !busy,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(29.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Premium,
                    contentColor = OnLight,
                    disabledContainerColor = Premium.copy(alpha = 0.35f),
                    disabledContentColor = OnLight.copy(alpha = 0.6f),
                ),
            ) {
                Text(
                    when {
                        busy -> "One moment"
                        chosen?.storeProduct?.introductoryDiscount?.paymentMode == DiscountPaymentMode.FREE_TRIAL -> "Start free"
                        else -> "Unlock every body"
                    },
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = {
                    busy = true
                    notice = null
                    scope.launch {
                        runCatching { Purchases.sharedInstance.awaitRestore() }
                            .onSuccess { info ->
                                RevenueCatManager.apply(info)
                                finish(RevenueCatManager.isPremium.value, restoring = true)
                            }
                            .onFailure { notice = "Could not reach the store to restore." }
                        busy = false
                    }
                },
                enabled = !busy,
            ) {
                Text("Restore purchases", color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Everything here can be earned with focus. Cancel any time; keep every body you earned.",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                PolicyLink("Privacy", Links.PRIVACY)
                PolicyLink("Terms", Links.TERMS)
            }
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun PolicyLink(label: String, url: String) {
    Text(
        label,
        color = Color.White.copy(alpha = 0.4f),
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { openUrl(url) }
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
}

@Composable
private fun PlanRow(
    line: PlanLine,
    price: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (isSelected) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isSelected) Color.White.copy(alpha = 0.7f) else Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                role = Role.RadioButton
                selected = isSelected
                contentDescription = "${line.title}, $price. ${line.note}"
            },
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .then(
                        if (isSelected) Modifier
                        else Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.14f)),
                    ),
            )
            Spacer(Modifier.width(15.dp))
            Column(Modifier.weight(1f)) {
                Text(line.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                if (line.note.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(line.note, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
            Text(price, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}
