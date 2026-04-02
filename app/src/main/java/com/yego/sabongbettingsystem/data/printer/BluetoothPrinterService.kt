package com.yego.sabongbettingsystem.data.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections

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
     * Print bet receipt
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
                        "[C]--------------------------------\n" +
                        "[L]Fight #[R]$fightNumber\n" +
                        "[L]Side[R]<b>$side</b>\n" +
                        "[L]Amount[R]<b>P$amount</b>\n" +
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

            null
        } catch (e: Exception) {
            e.message ?: "Printing failed."
        }
    }

    /**
     * Print payout receipt
     */
    fun printPayoutReceipt(
        context: Context,
        reference: String,
        fight: String,
        side: String,
        betAmount: String,
        netPayout: String,
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

            printer.printFormattedTextAndCut(
                "[C]<b><font size='big'>SABONG</font></b>\n" +
                        "[C]<b>BETTING SYSTEM</b>\n" +
                        "[C]Payout Receipt\n" +
                        "[C]--------------------------------\n" +
                        "[L]Reference[R]$reference\n" +
                        "[L]Fight[R]$fight\n" +
                        "[L]Side[R]<b>$side</b>\n" +
                        "[L]Bet Amount[R]P$betAmount\n" +
                        "[C]--------------------------------\n" +
                        "[C]<b><font size='big'>PAYOUT</font></b>\n" +
                        "[C]<b><font size='big'>P$netPayout</font></b>\n" +
                        "[C]--------------------------------\n" +
                        "[L]Teller[R]$teller\n" +
                        "[C]<barcode type='128' height='10'>$reference</barcode>\n" +
                        "[C]$reference\n" +
                        "[C]--------------------------------\n" +
                        "[C]Thank you!\n"
            )

            null
        } catch (e: Exception) {
            e.message ?: "Printing failed."
        }
    }
}