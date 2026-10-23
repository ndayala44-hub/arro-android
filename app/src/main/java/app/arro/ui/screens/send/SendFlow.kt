package app.arro.ui.screens.send

import app.arro.R
import app.arro.ui.str
import androidx.navigation.NavHostController
import app.arro.AppContainer
import app.arro.core.Catalog
import app.arro.core.KycStatus
import app.arro.core.PayoutType
import app.arro.core.Recipient
import app.arro.ui.BannerKind
import app.arro.ui.BannerState
import app.arro.ui.Routes
import app.arro.ui.go

/**
 * Starts a transfer from anywhere in the app: checks the account can send, prepares the draft and opens the first
 * screen that still needs an answer.
 */
fun startSend(c: AppContainer, nav: NavHostController, banner: BannerState, recipient: Recipient? = null, country: String? = null, hub: Boolean = false) {
    val blocker = c.backend.sendBlocker()
    if (blocker != null) {
        if (blocker.code == "kyc") {
            val inReview = c.backend.state.value.account?.kyc?.status == KycStatus.IN_REVIEW
            banner.show(if (inReview) str(R.string.send_flow_we_re_still_checking_your_id) else str(R.string.send_flow_verify_your_identity_first), blocker.message ?: "", BannerKind.Info)
            nav.go(if (inReview) Routes.KYC_STATUS else Routes.KYC)
        } else {
            banner.show(str(R.string.send_flow_you_can_t_send_right_now), blocker.message ?: "", BannerKind.Error,
                if (blocker.code == "frozen") str(R.string.send_flow_security) else null) { nav.go(Routes.SECURITY) }
        }
        return
    }
    val d = c.session.send
    when {
        recipient != null -> {
            val m = recipient.defaultMethod
            d.reset(recipient.country)
            d.recipientId = recipient.id
            d.methodId = m?.id
            d.payoutType = m?.type ?: Catalog.country(recipient.country).options.first().type
            d.arroUser = m?.type == PayoutType.ARRO
            nav.go(Routes.SEND_AMOUNT)
        }
        country != null -> {
            d.reset(country)
            nav.go(Routes.SEND_AMOUNT)
        }
        hub -> {
            d.reset(d.country)
            nav.go(Routes.SEND)
        }
        else -> {
            d.reset(d.country)
            nav.go(Routes.SEND_COUNTRY)
        }
    }
}
