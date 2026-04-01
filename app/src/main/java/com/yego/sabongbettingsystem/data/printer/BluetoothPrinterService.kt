package com.yego.sabongbettingsystem.data.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections

@SuppressLint("MissingPermission")
object BluetoothPrinterService {

    /**
     * Print receipt via Bluetooth ESC/POS
     * Returns null on success, error message on failure
     */
    fun printReceipt(
        context: Context,
        fightNumber: String,
        side: String,
        amount: String,
        reference: String,
        teller: String,
        date: String,
        time: String,
        qrData: String
    ): String? {
        return try {
            // auto-connect to first available Bluetooth printer
            val connection = BluetoothPrintersConnections.selectFirstPaired()
                ?: return "No Bluetooth printer found. Make sure printer is paired."

            val printer = EscPosPrinter(connection, 203, 72f, 32)

            printer.printFormattedTextAndCut(
                "[C]<img>" + generateQrEscPos(qrData) + "</img>\n" +
                        "[C]<b><font size='big'>🐓 SABONG</font></b>\n" +
                        "[C]<b>BETTING SYSTEM</b>\n" +
                        "[C]Official Bet Receipt\n" +
                        "[C]--------------------------------\n" +
                        "[L]Fight #[R]$fightNumber\n" +
                        "[L]Side[R]<b>$side</b>\n" +
                        "[L]Amount[R]<b>₱$amount</b>\n" +
                        "[L]Reference[R]$reference\n" +
                        "[L]Teller[R]$teller\n" +
                        "[L]Date[R]$date\n" +
                        "[L]Time[R]$time\n" +
                        "[C]--------------------------------\n" +
                        "[C]<barcode type='128' height='10'>$reference</barcode>\n" +
                        "[C]$reference\n" +
                        "[C]--------------------------------\n" +
                        "[C]Keep this receipt.\n" +
                        "[C]Present upon claiming payout.\n"
            )

            null // success
        } catch (e: Exception) {
            e.message ?: "Printing failed."
        }
    }

    fun printPayoutReceipt(
        context: Context,
        reference: String,
        fight: String,
        side: String,
        betAmount: String,
        netPayout: String,
        teller: String
    ): String? {
        return try {
            val connection = BluetoothPrintersConnections.selectFirstPaired()
                ?: return "No Bluetooth printer found. Make sure printer is paired."

            val printer = EscPosPrinter(connection, 203, 72f, 32)

            printer.printFormattedTextAndCut(
                "[C]<b><font size='big'>🐓 SABONG</font></b>\n" +
                        "[C]<b>BETTING SYSTEM</b>\n" +
                        "[C]Payout Receipt\n" +
                        "[C]--------------------------------\n" +
                        "[L]Reference[R]$reference\n" +
                        "[L]Fight[R]$fight\n" +
                        "[L]Side[R]<b>$side</b>\n" +
                        "[L]Bet Amount[R]₱$betAmount\n" +
                        "[C]--------------------------------\n" +
                        "[C]<b><font size='big'>PAYOUT</font></b>\n" +
                        "[C]<b><font size='big'>₱$netPayout</font></b>\n" +
                        "[C]--------------------------------\n" +
                        "[L]Teller[R]$teller\n" +
                        "[C]<barcode type='128' height='10'>$reference</barcode>\n" +
                        "[C]$reference\n" +
                        "[C]--------------------------------\n" +
                        "[C]Thank you!\n"
            )

            null // success
        } catch (e: Exception) {
            e.message ?: "Printing failed."
        }
    }

    private fun generateQrEscPos(data: String): String {
        // the library handles QR via barcode tag
        return ""
    }
}