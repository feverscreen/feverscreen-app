package nz.org.cacophony.feverscreen

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle

class AlertDialogActivity : Activity() {

    private var receiver: BroadcastReceiver? = null
    private var alertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("No power connected.")
        builder.setMessage("please connect power cable")
        builder.setOnDismissListener { super.onBackPressed() }
        alertDialog = builder.create()
        alertDialog?.show()

        val filter = IntentFilter()
        filter.addAction("android.intent.action.ACTION_POWER_CONNECTED")
        filter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED")

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, i: Intent) {
                Thread.sleep(1000) // wait for USB power state to be updated
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val charging = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB
                if (charging) {
                    this@AlertDialogActivity.onDestroy()
                }
            }
        }
        registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        alertDialog?.cancel()
        if (receiver != null) {
            unregisterReceiver(receiver)
            receiver = null
        }
        super.onDestroy()
    }
}