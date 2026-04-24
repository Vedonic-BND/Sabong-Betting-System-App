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
        printerAddress: String? = null
    ): String? {
        return try {
            val connection = if (printerAddress != null) {
                getConnectionByAddress(context, printerAddress)
            } else {
                BluetoothPrintersConnections.selectFirstPaired()
            } ?: return "No Bluetooth printer found. Please select a printer first."

            val printer = EscPosPrinter(connection, 203, 72f, 32)

            printer.printFormattedTextAndCut(
                "[C]<b><font size='big'>SABONG</font></b>\n" +
                "[C]<b>BETTING SYSTEM</b>\n" +
                "[C]Official Bet Receipt\n" +
                "[C]------------------------------\n" +
                "[L]Fight No.[R]<b>$fightNumber</b>\n" +
                "[L]Bet Side[R]<b>$side</b>\n" +
                "[L]Bet Amount[R]<b>P$amount</b>\n" +
                "[C]------------------------------\n" +
                "[L]Reference #[R]$reference\n" +
                "[L]Teller[R]$teller\n" +
                "[L]Date[R]$date\n" +
                "[L]Time[R]$time\n" +
                "[C]------------------------------\n" +
                "[C]Scan to verify\n" +
                "\n" +
                "[C]<qrcode size='30'>$qrData</qrcode>\n" +
                "[C]<b>$reference</b>\n" +
                "[C]------------------------------\n" +
                "[C]Keep this receipt.\n" +
                "[C]Present upon claiming payout.\n" +
                "[C]Thank you for betting!\n"
            )

            null
        } catch (e: Exception) {
            e.message ?: "Printing failed."
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
        printerAddress: String? = null
    ): String? {
        return try {
            val connection = if (printerAddress != null) {
                getConnectionByAddress(context, printerAddress)
            } else {
                BluetoothPrintersConnections.selectFirstPaired()
            } ?: return "No Bluetooth printer found. Please select a printer first."

            val printer = EscPosPrinter(connection, 203, 72f, 32)

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

            // Payout receipt layout is focused on the winning results
            printer.printFormattedTextAndCut(
                "[C]<b><font size='big'>SABONG</font></b>\n" +
                "[C]<b>BETTING SYSTEM</b>\n" +
                "[C]<b><font size='big'>Official Payout Receipt</font></b>\n" +
                "[C]------------------------------\n" +
                "[L]Fight No.[R]<b>$fight</b>\n" +
                "[L]Bet Side[R]<b>$side</b>\n" + 
                "[C]------------------------------\n" +
                "[L]Result[R]You Won!\n" +
                "[L]Bet Amount[R]P$betAmount\n" +
                "[L]Multiplier[R]x$multiplier\n" +
                "[C]------------------------------\n" +
                "[C]<b><font size='big'>TOTAL PAYOUT</font></b>\n" +
                "[C]<b><font size='big'>P$netPayout</font></b>\n" +
                "[C]------------------------------\n" +
                "[L]Reference[R]$reference\n" +
                "[L]Status[R]<b>$status</b>\n" +
                "[L]Paid On[R]$displayDate $displayTime\n" +
                "[L]Teller[R]$teller\n" +
                "[C]------------------------------\n" +
                "[C]Thank you for playing!\n"
            )

            null
        } catch (e: Exception) {
            e.message ?: "Printing failed."
        }
    }
}
