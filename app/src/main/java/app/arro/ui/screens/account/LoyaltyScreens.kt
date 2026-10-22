package app.arro.ui.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.arro.core.Account
import app.arro.core.Currency
import app.arro.core.FriendStatus
import app.arro.core.Loyalty
import app.arro.core.Money
import app.arro.core.PointsKind
import app.arro.core.ReferredFriend
import app.arro.core.Text
import app.arro.platform.Share
import app.arro.ui.BannerKind
import app.arro.ui.LocalBanner
import app.arro.ui.LocalNav
import app.arro.ui.Routes
import app.arro.ui.appState
import app.arro.ui.back
import app.arro.ui.container
import app.arro.ui.go
import app.arro.ui.launchCall
import app.arro.ui.nowTicker
import app.arro.ui.rememberCall
import app.arro.ui.components.ArroButton
import app.arro.ui.components.ArroScreen
import app.arro.ui.components.Art
import app.arro.ui.components.Avatar
import app.arro.ui.components.Btn
import app.arro.ui.components.BtnSize
import app.arro.ui.components.Cap
import app.arro.ui.components.Chevron
import app.arro.ui.components.FieldHint
import app.arro.ui.components.Group
import app.arro.ui.components.GroupDivider
import app.arro.ui.components.Icons
import app.arro.ui.components.KV
import app.arro.ui.components.LinkText
import app.arro.ui.components.ListRow
import app.arro.ui.components.Note
import app.arro.ui.components.NoteKind
import app.arro.ui.components.Over
import app.arro.ui.components.Panel
import app.arro.ui.components.PanelKind
import app.arro.ui.components.Pill
import app.arro.ui.components.PillKind
import app.arro.ui.components.Segmented
import app.arro.ui.components.Sheet
import app.arro.ui.components.SheetHeader
import app.arro.ui.components.Tile
import app.arro.ui.components.TileStyle
import app.arro.ui.components.TopBar
import app.arro.ui.components.TrailingValue
import app.arro.ui.components.Txt
import app.arro.ui.screens.send.startSend
import app.arro.ui.screens.system.EmptyState
import app.arro.ui.theme.C
import app.arro.ui.theme.T

// ------------------------------------------------------------------ 11.10 Loyalty and rewards

/**
 * Points from transfers and money from referrals, kept apart. Opened from the award button on Home and from the
 * Account menu; both show this same screen and the same account data.
 */
@Composable
fun LoyaltyScreen() {
    val nav = LocalNav.current
    val c = container()
    val b = c.backend
    val banner = LocalBanner.current
    val state by appState()
    val a = state.account ?: return
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var swap by remember { mutableStateOf<Pair<Int, Long>?>(null) }
    val scope = rememberCoroutineScope()
    val call = rememberCall()
    val now = nowTicker(60_000)
    val r = a.referral
    val rewardsPaid = r.friends.count { it.rewardMinor > 0 }.let { if (r.friends.isEmpty() && r.earnedMinor > 0) (r.earnedMinor / Loyalty.REFERRAL_REWARD_MINOR).toInt() else it }

    ArroScreen(
        topBar = { TopBar(onBack = { nav.back() }, title = "Loyalty and rewards") },
        gap = 16.dp,
        overlay = {
            val pick = swap
            Sheet(pick != null, { if (!call.busy.value) swap = null }) {
                if (pick != null) {
                    val (pts, credit) = pick
                    SheetHeader("Swap ${Money.points(pts)} points?", { swap = null }, "You'll get ${Money.format(credit, Currency.GBP)} off the Arro fee on your next transfers.")
                    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        KV("Points now", Money.points(a.pointsBalance))
                        KV("Points after", Money.points(a.pointsBalance - pts))
                        KV("Fee credit", Money.format(a.feeCreditMinor + credit, Currency.GBP), strong = true)
                    }
                    FieldHint(call.error.value)
                    Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArroButton("Swap points", {
                            scope.launchCall(call) {
                                b.redeemPoints(pts)
                                swap = null
                                banner.show("Fee credit added", "${Money.format(credit, Currency.GBP)} comes off the Arro fee on your next transfers.", BannerKind.Success)
                            }
                        }, loading = call.busy.value)
                        ArroButton("Cancel", { swap = null }, style = Btn.Text)
                    }
                }
            }
        },
    ) {
        // Summary: the two kinds of reward side by side, never added together.
        Panel(kind = PanelKind.Dark, padding = 18.dp, gap = 12.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Over("Points", color = C.Lime)
                    Txt(Money.points(a.pointsBalance), T.Big28, color = C.OnNight)
                    Cap("Worth ${Money.format(Loyalty.creditFor(a.pointsBalance), Currency.GBP)} in fees", color = C.DarkCap, size = 12f)
                }
                Box(Modifier.width(1.dp).height(64.dp).background(C.OnNight.copy(alpha = 0.16f)))
                Column(Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Over("Referral earnings", color = C.Lime)
                    Txt(Money.format(r.earnedMinor, Currency.GBP), T.Big28, color = C.OnNight)
                    Cap("$rewardsPaid ${if (rewardsPaid == 1) "reward" else "rewards"} · in your balance", color = C.DarkCap, size = 12f)
                }
            }
            if (a.feeCreditMinor > 0) Pill("${Money.format(a.feeCreditMinor, Currency.GBP)} fee credit ready for your next transfer", PillKind.Lime)
        }
        Segmented(listOf("Points", "Referrals"), tab, { tab = it })
        if (tab == 0) PointsTab(a, now) { swap = it } else ReferralsTab(a, now)
    }
}

@Composable
private fun PointsTab(a: Account, now: Long, onSwap: (Pair<Int, Long>) -> Unit) {
    val nav = LocalNav.current
    val c = container()
    val banner = LocalBanner.current
    val pending = a.transfers.filter { it.isOpen }.sumOf { Loyalty.preview(it.feeMinor, it.pointsDiscountMinor) }
    if (a.points.isEmpty()) {
        EmptyState(Art.Gift, "No points yet", "You earn 1 point for every 1p of Arro fee once a transfer is delivered, " +
            "plus ${Loyalty.FIRST_TRANSFER_BONUS} bonus points on your first.") {
            ArroButton("Send money", { startSend(c, nav, banner) })
        }
        if (pending > 0) Note("${Money.points(pending)} points are on the way from transfers that haven't been delivered yet.", NoteKind.Info, Icons.Clock)
        return
    }
    if (pending > 0) Note("${Money.points(pending)} more points are on the way from transfers that haven't been delivered yet.", NoteKind.Info, Icons.Clock)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Over("Swap points for fee credit")
        Group {
            Loyalty.tiers.forEachIndexed { i, (pts, credit) ->
                if (i > 0) GroupDivider()
                val enough = a.pointsBalance >= pts
                ListRow("${Money.format(credit, Currency.GBP)} off fees", flat = true, minHeight = 58.dp,
                    subtitle = if (enough) "${Money.points(pts)} points" else "${Money.points(pts)} points · ${Money.points(pts - a.pointsBalance)} to go",
                    leading = { Tile(Icons.Percent, if (enough) TileStyle.Lime else TileStyle.Tint, 36.dp, iconSize = 16.dp) },
                    trailing = { ArroButton("Swap", { onSwap(pts to credit) }, style = Btn.Line, size = BtnSize.S, enabled = enough) })
            }
        }
        Cap("Fee credit comes off the Arro fee on your next transfers, automatically. It can't be paid out as cash.", size = 12f)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Over("Points history")
        Group {
            a.points.take(40).forEachIndexed { i, e ->
                if (i > 0) GroupDivider()
                ListRow(e.title, flat = true, minHeight = 58.dp, subtitle = "${e.subtitle} · ${Text.relativeDay(e.createdAt, now)}",
                    leading = { Tile(when (e.kind) { PointsKind.TRANSFER -> Icons.ArrowUpRight; PointsKind.BONUS -> Icons.Gift; PointsKind.REDEEMED -> Icons.Percent },
                        if (e.kind == PointsKind.REDEEMED) TileStyle.Tint else TileStyle.Lime, 36.dp, iconSize = 16.dp) },
                    trailing = { TrailingValue((if (e.points > 0) "+" else "−") + Money.points(kotlin.math.abs(e.points)), "points", valueColor = if (e.points > 0) C.Ok else C.Ink) },
                    onClick = e.ref.takeIf { it.startsWith("transfer:") }?.let { ref -> { nav.go(Routes.detail(ref.substringAfter(':'))) } })
            }
        }
    }
    Panel(gap = 8.dp) {
        Txt("How points work", T.H3)
        Cap("1 point for every 1p of Arro fee you pay, added when the transfer is delivered. Each transfer earns points once.")
        Cap("${Loyalty.FIRST_TRANSFER_BONUS} bonus points on your first delivered transfer. Cancelled, failed and refunded transfers don't earn points.")
        Cap("${Money.points(Loyalty.POINTS_PER_POUND)} points = £1.00 off fees.")
    }
}

@Composable
private fun ReferralsTab(a: Account, now: Long) {
    val nav = LocalNav.current
    val banner = LocalBanner.current
    val context = LocalContext.current
    val r = a.referral
    val waiting = r.friends.count { it.status == FriendStatus.SIGNED_UP }
    Group {
        ListRow("Invite friends", flat = true, minHeight = 58.dp, subtitle = "Your code ${r.code} · £10 for every friend who sends £100 or more",
            leading = { Tile(Icons.Gift, TileStyle.Lime, 36.dp, iconSize = 16.dp) }, trailing = { Chevron() }, onClick = { nav.go(Routes.REFER) })
        GroupDivider()
        ListRow("Copy invite code", flat = true, minHeight = 52.dp, leading = { Tile(Icons.Copy, size = 36.dp, iconSize = 16.dp) },
            onClick = { Share.copy(context, "Arro invite code", r.code); banner.show("Invite code copied", kind = BannerKind.Success) })
    }
    if (r.friends.isEmpty() && r.earnedMinor == 0L) {
        EmptyState(Art.Gift, "No referral rewards yet", "Share your code. Once a friend's first transfer of £100 or more is delivered, " +
            "£10 goes into your GBP balance.") {
            ArroButton("Invite friends", { nav.go(Routes.REFER) }, style = Btn.Lime, icon = Icons.Share)
        }
        return
    }
    Panel(gap = 10.dp) {
        KV("Friends joined", "${maxOf(r.friendsJoined, r.friends.size)}")
        KV("Rewards paid", "${r.friends.count { it.rewardMinor > 0 }}")
        if (waiting > 0) KV("Waiting for a first £100 transfer", "$waiting")
        KV("Total earned", Money.format(r.earnedMinor, Currency.GBP), strong = true)
    }
    if (r.friends.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Over("Friends")
        Group {
            r.friends.sortedByDescending { maxOf(it.joinedAt, it.qualifiedAt) }.forEachIndexed { i, f ->
                if (i > 0) GroupDivider()
                FriendRow(f, now)
            }
        }
    }
    Panel(kind = PanelKind.Lime, gap = 10.dp) {
        Txt("Where your rewards go", T.H3)
        Cap("Referral rewards are paid into your GBP balance, so you can send or convert them like any other money there.", color = C.Forest)
        ArroButton("View GBP balance", { nav.go(Routes.walletAccount("GBP")) }, style = Btn.Line, size = BtnSize.S)
    }
    if (r.friends.any { it.sample }) Note("Friends marked Sample are demo data in this test build. Real referrals appear here when friends join with your code.", NoteKind.Info, Icons.Info)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Cap("Up to ${Loyalty.REFERRAL_YEARLY_CAP} rewards a year. ", size = 12f)
        LinkText("Terms apply", { nav.go(Routes.legal("referral")) }, color = C.Ink, underline = true)
    }
}

@Composable
private fun FriendRow(f: ReferredFriend, now: Long) {
    val initials = f.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    ListRow(f.name, flat = true, minHeight = 62.dp,
        subtitle = when {
            f.status == FriendStatus.SIGNED_UP -> "Joined ${Text.relativeDay(f.joinedAt, now).lowercase()} · reward once they send £100 or more"
            f.rewardMinor > 0 -> "Sent ${Money.format(f.qualifyingMinor, Currency.GBP, false)} · reward paid ${Text.dayMonth(f.qualifiedAt)}"
            else -> "Sent ${Money.format(f.qualifyingMinor, Currency.GBP, false)} · yearly reward limit reached"
        },
        leading = { Avatar(initials, (f.name.length % 6) + 1, 36.dp) },
        trailing = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (f.rewardMinor > 0) Txt("+" + Money.format(f.rewardMinor, Currency.GBP), T.Num, color = C.Ok)
                else Pill(if (f.status == FriendStatus.SIGNED_UP) "Waiting" else "No reward", PillKind.Neutral)
                if (f.sample) Txt("Sample", T.Cap.copy(fontSize = 11.sp), color = C.Slate)
            }
        })
}
