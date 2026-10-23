package app.arro.ui.screens.account

import app.arro.R
import app.arro.ui.str
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
        topBar = { TopBar(onBack = { nav.back() }, title = str(R.string.loyalty_loyalty_and_rewards)) },
        gap = 16.dp,
        overlay = {
            val pick = swap
            Sheet(pick != null, { if (!call.busy.value) swap = null }) {
                if (pick != null) {
                    val (pts, credit) = pick
                    SheetHeader(str(R.string.loyalty_swap_x_points, Money.points(pts)), { swap = null }, str(R.string.loyalty_you_ll_get_x_off_the, Money.format(credit, Currency.GBP)))
                    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        KV(str(R.string.loyalty_points_now), Money.points(a.pointsBalance))
                        KV(str(R.string.loyalty_points_after), Money.points(a.pointsBalance - pts))
                        KV(str(R.string.loyalty_fee_credit), Money.format(a.feeCreditMinor + credit, Currency.GBP), strong = true)
                    }
                    FieldHint(call.error.value)
                    Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArroButton(str(R.string.loyalty_swap_points), {
                            scope.launchCall(call) {
                                b.redeemPoints(pts)
                                swap = null
                                banner.show(str(R.string.loyalty_fee_credit_added), str(R.string.loyalty_x_comes_off_the_arro_fee, Money.format(credit, Currency.GBP)), BannerKind.Success)
                            }
                        }, loading = call.busy.value)
                        ArroButton(str(R.string.loyalty_cancel), { swap = null }, style = Btn.Text)
                    }
                }
            }
        },
    ) {
        // Summary: the two kinds of reward side by side, never added together.
        Panel(kind = PanelKind.Dark, padding = 18.dp, gap = 12.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Over(str(R.string.loyalty_points), color = C.Lime)
                    Txt(Money.points(a.pointsBalance), T.Big28, color = C.OnNight)
                    Cap(str(R.string.loyalty_worth_x_in_fees, Money.format(Loyalty.creditFor(a.pointsBalance), Currency.GBP)), color = C.DarkCap, size = 12f)
                }
                Box(Modifier.width(1.dp).height(64.dp).background(C.OnNight.copy(alpha = 0.16f)))
                Column(Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Over(str(R.string.loyalty_referral_earnings), color = C.Lime)
                    Txt(Money.format(r.earnedMinor, Currency.GBP), T.Big28, color = C.OnNight)
                    Cap(str(R.string.loyalty_x_x_in_your_balance, rewardsPaid, if (rewardsPaid == 1) str(R.string.loyalty_reward) else str(R.string.loyalty_rewards)), color = C.DarkCap, size = 12f)
                }
            }
            if (a.feeCreditMinor > 0) Pill(str(R.string.loyalty_x_fee_credit_ready_for_your, Money.format(a.feeCreditMinor, Currency.GBP)), PillKind.Lime)
        }
        Segmented(listOf(str(R.string.loyalty_points), str(R.string.loyalty_referrals)), tab, { tab = it })
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
        EmptyState(Art.Gift, str(R.string.loyalty_no_points_yet), str(R.string.loyalty_you_earn_1_point_for_every) +
            str(R.string.loyalty_plus_x_bonus_points_on_your, Loyalty.FIRST_TRANSFER_BONUS)) {
            ArroButton(str(R.string.loyalty_send_money), { startSend(c, nav, banner) })
        }
        if (pending > 0) Note(str(R.string.loyalty_x_points_are_on_the_way, Money.points(pending)), NoteKind.Info, Icons.Clock)
        return
    }
    if (pending > 0) Note(str(R.string.loyalty_x_more_points_are_on_the, Money.points(pending)), NoteKind.Info, Icons.Clock)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Over(str(R.string.loyalty_swap_points_for_fee_credit))
        Group {
            Loyalty.tiers.forEachIndexed { i, (pts, credit) ->
                if (i > 0) GroupDivider()
                val enough = a.pointsBalance >= pts
                ListRow(str(R.string.loyalty_x_off_fees, Money.format(credit, Currency.GBP)), flat = true, minHeight = 58.dp,
                    subtitle = if (enough) str(R.string.loyalty_x_points, Money.points(pts)) else str(R.string.loyalty_x_points_x_to_go, Money.points(pts), Money.points(pts - a.pointsBalance)),
                    leading = { Tile(Icons.Percent, if (enough) TileStyle.Lime else TileStyle.Tint, 36.dp, iconSize = 16.dp) },
                    trailing = { ArroButton(str(R.string.loyalty_swap), { onSwap(pts to credit) }, style = Btn.Line, size = BtnSize.S, enabled = enough) })
            }
        }
        Cap(str(R.string.loyalty_fee_credit_comes_off_the_arro), size = 12f)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Over(str(R.string.loyalty_points_history))
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
        Txt(str(R.string.loyalty_how_points_work), T.H3)
        Cap(str(R.string.loyalty_n_1_point_for_every_1p_of))
        Cap(str(R.string.loyalty_x_bonus_points_on_your_first, Loyalty.FIRST_TRANSFER_BONUS))
        Cap(str(R.string.loyalty_x_points_1_00_off_fees, Money.points(Loyalty.POINTS_PER_POUND)))
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
        ListRow(str(R.string.loyalty_invite_friends), flat = true, minHeight = 58.dp, subtitle = str(R.string.loyalty_your_code_x_10_for_every, r.code),
            leading = { Tile(Icons.Gift, TileStyle.Lime, 36.dp, iconSize = 16.dp) }, trailing = { Chevron() }, onClick = { nav.go(Routes.REFER) })
        GroupDivider()
        ListRow(str(R.string.loyalty_copy_invite_code), flat = true, minHeight = 52.dp, leading = { Tile(Icons.Copy, size = 36.dp, iconSize = 16.dp) },
            onClick = { Share.copy(context, str(R.string.loyalty_arro_invite_code), r.code); banner.show(str(R.string.loyalty_invite_code_copied), kind = BannerKind.Success) })
    }
    if (r.friends.isEmpty() && r.earnedMinor == 0L) {
        EmptyState(Art.Gift, str(R.string.loyalty_no_referral_rewards_yet), str(R.string.loyalty_share_your_code_once_a_friend) +
            str(R.string.loyalty_n_10_goes_into_your_gbp_balance)) {
            ArroButton(str(R.string.loyalty_invite_friends), { nav.go(Routes.REFER) }, style = Btn.Lime, icon = Icons.Share)
        }
        return
    }
    Panel(gap = 10.dp) {
        KV(str(R.string.loyalty_friends_joined), "${maxOf(r.friendsJoined, r.friends.size)}")
        KV(str(R.string.loyalty_rewards_paid), "${r.friends.count { it.rewardMinor > 0 }}")
        if (waiting > 0) KV(str(R.string.loyalty_waiting_for_a_first_100_transfer), "$waiting")
        KV(str(R.string.loyalty_total_earned), Money.format(r.earnedMinor, Currency.GBP), strong = true)
    }
    if (r.friends.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Over(str(R.string.loyalty_friends))
        Group {
            r.friends.sortedByDescending { maxOf(it.joinedAt, it.qualifiedAt) }.forEachIndexed { i, f ->
                if (i > 0) GroupDivider()
                FriendRow(f, now)
            }
        }
    }
    Panel(kind = PanelKind.Lime, gap = 10.dp) {
        Txt(str(R.string.loyalty_where_your_rewards_go), T.H3)
        Cap(str(R.string.loyalty_referral_rewards_are_paid_into_your), color = C.Forest)
        ArroButton(str(R.string.loyalty_view_gbp_balance), { nav.go(Routes.walletAccount("GBP")) }, style = Btn.Line, size = BtnSize.S)
    }
    if (r.friends.any { it.sample }) Note(str(R.string.loyalty_friends_marked_sample_are_demo_data), NoteKind.Info, Icons.Info)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Cap(str(R.string.loyalty_up_to_x_rewards_a_year, Loyalty.REFERRAL_YEARLY_CAP), size = 12f)
        LinkText(str(R.string.loyalty_terms_apply), { nav.go(Routes.legal("referral")) }, color = C.Ink, underline = true)
    }
}

@Composable
private fun FriendRow(f: ReferredFriend, now: Long) {
    val initials = f.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    ListRow(f.name, flat = true, minHeight = 62.dp,
        subtitle = when {
            f.status == FriendStatus.SIGNED_UP -> str(R.string.loyalty_joined_x_reward_once_they_send, Text.relativeDay(f.joinedAt, now).lowercase())
            f.rewardMinor > 0 -> str(R.string.loyalty_sent_x_reward_paid_x, Money.format(f.qualifyingMinor, Currency.GBP, false), Text.dayMonth(f.qualifiedAt))
            else -> str(R.string.loyalty_sent_x_yearly_reward_limit_reached, Money.format(f.qualifyingMinor, Currency.GBP, false))
        },
        leading = { Avatar(initials, (f.name.length % 6) + 1, 36.dp) },
        trailing = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (f.rewardMinor > 0) Txt("+" + Money.format(f.rewardMinor, Currency.GBP), T.Num, color = C.Ok)
                else Pill(if (f.status == FriendStatus.SIGNED_UP) str(R.string.loyalty_waiting) else str(R.string.loyalty_no_reward), PillKind.Neutral)
                if (f.sample) Txt(str(R.string.loyalty_sample), T.Cap.copy(fontSize = 11.sp), color = C.Slate)
            }
        })
}
