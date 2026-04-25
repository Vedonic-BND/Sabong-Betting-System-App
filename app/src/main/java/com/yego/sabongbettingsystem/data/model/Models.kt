package com.yego.sabongbettingsystem.data.model

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val role: String,
    val token: String?,
    val cashin_token: String? = null,
    val cashout_token: String? = null,
    val user: UserData
)

data class UserData(
    val id: Int,
    val name: String,
    val role: String,
    val app: String
)

data class TellerModeSelection(
    val mode: String // "cashin" or "cashout"
)

data class Fight(
    val id              : Int,
    val fight_number    : String,
    val status          : String,
    val meron_status    : String,
    val wala_status     : String,
    val winner          : String?,
    val commission_rate : String,
    val meron_total     : String,
    val wala_total      : String
)

data class CreateFightRequest(
    val fight_number: String
)

data class UpdateStatusRequest(
    val status: String
)

data class UpdateSideStatusRequest(
    val side   : String,
    val status : String
)

data class UpdateAllSideStatusRequest(
    val status       : String,
    val fight_status : String? = null
)

data class DeclareWinnerRequest(
    val winner: String
)

data class PlaceBetRequest(
    val side: String,
    val amount: Double
)

data class BetResponse(
    val message: String? = null,
    val reference: String,
    val qr: String? = null,
    val barcode: String? = null,
    val receipt: ReceiptData,
    val bet: BetData? = null,
    var winner: String? = null,
    var won: Boolean? = null,
    var status: String? = null,
    var payout_date: String? = null,
    var payout_time: String? = null,
    var net_payout: String? = null
)

// Wrapper for history list if the server returns { "data": [...] }
data class BetHistoryResponse(
    val data: List<BetResponse>
)

data class ReceiptData(
    val fight_number: String?,
    val side: String?,
    val amount: String?,
    val reference: String?,
    val teller: String?,
    val date: String?,
    val time: String?
)

data class BetData(
    val id: Int,
    val reference: String,
    val fight_number: String,
    val side: String,
    val amount: String
)

data class PayoutResponse(
    val reference: String,
    val fight: String,
    val side: String,
    val bet_amount: String,
    val winner: String?,
    val won: Boolean,
    val gross_payout: String,
    val commission: String,
    val net_payout: String,
    val status: String,
    val teller: String,
    val payout_date: String? = null,
    val payout_time: String? = null
)

data class MessageResponse(
    val message: String
)

// ── Runner Models ──────────────────────────────────────
data class RunnerTransactionRequest(
    val teller_id: Int,
    val amount: Double,
    val type: String // "collect" or "provide"
)

data class RunnerTransactionResponse(
    val id: Int,
    val runner_name: String,
    val teller_name: String,
    val amount: String,
    val type: String,
    val status: String,
    val date: String,
    val time: String
)

data class TellerCashStatus(
    val id: Int,
    val name: String,
    val on_hand_cash: String,
    val last_transaction: String?
)
