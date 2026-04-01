package com.yego.sabongbettingsystem.data.model

data class LoginRequest(
    val username: String,
    val password: String,
    val app: String
)

data class LoginResponse(
    val token: String,
    val user: UserData
)

data class UserData(
    val id: Int,
    val name: String,
    val role: String,
    val app: String
)

data class Fight(
    val id: Int,
    val fight_number: String,
    val status: String,
    val winner: String?,
    val commission_rate: String,
    val meron_total: String,
    val wala_total: String
)

data class CreateFightRequest(
    val fight_number: String
)

data class UpdateStatusRequest(
    val status: String
)

data class DeclareWinnerRequest(
    val winner: String
)

data class PlaceBetRequest(
    val side: String,
    val amount: Double
)

data class BetResponse(
    val message: String,
    val reference: String,
    val qr: String,
    val barcode: String,
    val receipt: ReceiptData,
    val bet: BetData
)

data class ReceiptData(
    val fight_number: String,
    val side: String,
    val amount: String,
    val reference: String,
    val teller: String,
    val date: String,
    val time: String
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
    val status: String
)

data class MessageResponse(
    val message: String
)