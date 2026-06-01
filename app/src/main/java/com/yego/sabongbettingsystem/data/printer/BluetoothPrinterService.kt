package com.yego.sabongbettingsystem.data.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("MissingPermission")
object BluetoothPrinterService {

    // store selected device name
    var selectedPrinterName: String? = null

    /**
     * Get all paired Bluetooth devices
     */
    fun getPairedDevices(context: Context): List<Pair<String, String>> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE)
                as BluetoothManager
        val adapter = bluetoothManager.adapter ?: return emptyList()

        return adapter.bondedDevices?.map { device ->
            Pair(device.name ?: "Unknown", device.address)
        } ?: emptyList()
    }

    /**
     * Get connection for a specific device by address
     */
    @SuppressLint("MissingPermission")
    private fun getConnectionByAddress(
        context: Context,
        address: String
    ): BluetoothConnection? {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE)
                as BluetoothManager
        val adapter = bluetoothManager.adapter ?: return null

        val device = adapter.bondedDevices?.find { it.address == address }
            ?: return null

        return BluetoothConnection(device)
    }

    /**
     * Print bet receipt (Type 1: Betting)
     * Layout matching: http://192.168.1.10:8000/receipt/{reference}
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
        qrData: String,
        printerAddress: String? = null,
        systemTitle: String = "SABONG BETTING SYSTEM"
    ): String? {
        var connection: BluetoothConnection? = null
        return try {
            // Get or create connection with timeout
            connection = if (printerAddress != null) {
                getConnectionByAddress(context, printerAddress)
            } else {
                BluetoothPrintersConnections.selectFirstPaired()
            } ?: return "No Bluetooth printer found. Please select a printer first."

            // Explicitly connect
            connection.connect()

            // Create printer with appropriate settings for faster printing (80mm)
            val printer = EscPosPrinter(connection, 203, 80f, 48)
            
            // Build the receipt text once
            val receiptText = "[C]<b>${systemTitle.trim().uppercase()}</b>\n" +
                "[C]Official Bet Receipt\n" +
                "[C]------------------------------------------------\n" +
                "[L]Fight No.[R]<b>$fightNumber</b>\n" +
                "[L]Bet Side[R]<b>$side</b>\n" +
                "[L]Bet Amount[R]<b>P$amount</b>\n" +
                "[C]------------------------------------------------\n" +
                "[L]Reference #[R]$reference\n" +
                "[L]Teller[R]$teller\n" +
                "[L]Date[R]$date\n" +
                "[L]Time[R]$time\n" +
                "[C]------------------------------------------------\n" +
                "[C]Scan to verify\n" +
                "[C]<barcode type='128' height='15'>$qrData</barcode>\n" +
                "[C]<b>$reference</b>"

            // Print with timeout handling
            printer.printFormattedTextAndCut(receiptText)
            null
        } catch (e: Exception) {
            android.util.Log.e("BluetoothPrinter", "Print error: ${e.message}", e)
            e.message ?: "Printing failed. Please check printer connection."
        } finally {
            try {
                connection?.disconnect()
            } catch (e: Exception) { }
        }
    }

    /**
     * Print payout receipt (Type 2: Payout)
     * Layout matching: http://192.168.1.10:8000/payout-receipt/{reference}
     */
    fun printPayoutReceipt(
        context: Context,
        reference: String,
        fight: String,
        side: String,
        betAmount: String,
        netPayout: String,
        status: String,
        payoutDate: String?,
        payoutTime: String?,
        teller: String,
        printerAddress: String? = null,
        systemTitle: String = "SABONG BETTING SYSTEM"
    ): String? {
        var connection: BluetoothConnection? = null
        return try {
            connection = if (printerAddress != null) {
                getConnectionByAddress(context, printerAddress)
            } else {
                BluetoothPrintersConnections.selectFirstPaired()
            } ?: return "No Bluetooth printer found. Please select a printer first."

            connection.connect()

            val printer = EscPosPrinter(connection, 203, 80f, 48)

            // Calculate multiplier
            val multiplier = try {
                val net = netPayout.toDouble()
                val bet = betAmount.toDouble()
                String.format(Locale.US, "%.2f", net / bet)
            } catch (e: Exception) {
                "0.00"
            }

            // Use provided payout date/time or current if not available
            val displayDate = payoutDate ?: SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
            val displayTime = payoutTime ?: SimpleDateFormat("hh:mm a", Locale.US).format(Date())

            // Build the receipt text once - simplified for faster printing
            val receiptText = "[C]<b>${systemTitle.trim().uppercase()}</b>\n" +
                "[C]Official Payout Receipt\n" +
                "[C]------------------------------------------------\n" +
                "[L]Reference[R]$reference\n" +
                "[L]Fight[R]<b>$fight</b>\n" +
                "[L]Side[R]<b>$side</b>\n" +
                "[C]------------------------------------------------\n" +
                "[L]Bet Amount[R]P$betAmount\n" +
                "[L]Multiplier[R]x$multiplier\n" +
                "[C]------------------------------------------------\n" +
                "[C]<b><font size='big'>TOTAL PAYOUT</font></b>\n" +
                "[C]<b><font size='big'>P$netPayout</font></b>\n" +
                "[C]------------------------------------------------\n" +
                "[L]Status[R]<b>$status</b>\n" +
                "[L]Date[R]$displayDate\n" +
                "[L]Time[R]$displayTime\n" +
                "[L]Teller[R]$teller\n" +
                "[C]Thank you for playing!"

            printer.printFormattedTextAndCut(receiptText)
            null
        } catch (e: Exception) {
            android.util.Log.e("BluetoothPrinter", "Payout print error: ${e.message}", e)
            e.message ?: "Printing failed. Please check printer connection."
        } finally {
            try {
                connection?.disconnect()
            } catch (e: Exception) { }
        }
    }

    /**
     * Print refund receipt (Type 3: Refund for Cancelled/Draw)
     */
    fun printRefundReceipt(
        context: Context,
        reference: String,
        fight: String,
        side: String,
        betAmount: String,
        refundAmount: String,
        status: String, // "CANCELLED" or "DRAW"
        refundDate: String?,
        refundTime: String?,
        teller: String,
        printerAddress: String? = null,
        systemTitle: String = "SABONG BETTING SYSTEM"
    ): String? {
        var connection: BluetoothConnection? = null
        return try {
            connection = if (printerAddress != null) {
                getConnectionByAddress(context, printerAddress)
            } else {
                BluetoothPrintersConnections.selectFirstPaired()
            } ?: return "No Bluetooth printer found. Please select a printer first."

            connection.connect()

            val printer = EscPosPrinter(connection, 203, 80f, 48)

            // Use provided date/time or current if not available
            val displayDate = refundDate ?: SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
            val displayTime = refundTime ?: SimpleDateFormat("hh:mm a", Locale.US).format(Date())

            // Build the receipt text once - simplified for faster printing
            val receiptText = "[C]<b>${systemTitle.trim().uppercase()}</b>\n" +
                "[C]Official Refund Receipt\n" +
                "[C]------------------------------------------------\n" +
                "[L]Reference[R]$reference\n" +
                "[L]Fight[R]<b>$fight</b>\n" +
                "[L]Side[R]<b>$side</b>\n" +
                "[C]------------------------------------------------\n" +
                "[L]Result[R]<b>$status</b>\n" +
                "[L]Bet Amount[R]P$betAmount\n" +
                "[C]<b>REFUND: P$refundAmount</b>\n" +
                "[C]------------------------------------------------\n" +
                "[L]Date[R]$displayDate\n" +
                "[L]Time[R]$displayTime\n" +
                "[L]Teller[R]$teller\n" +
                "[C]Thank you for playing!"

            printer.printFormattedTextAndCut(receiptText)
            null
        } catch (e: Exception) {
            android.util.Log.e("BluetoothPrinter", "Refund print error: ${e.message}", e)
            e.message ?: "Printing failed. Please check printer connection."
        } finally {
            try {
                connection?.disconnect()
            } catch (e: Exception) { }
        }
    }
}
