package nz.org.cacophony.feverscreen

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle

class AlertDialogActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Device is not charging")
        builder.setMessage("Please attach the tablet to a charger")
        builder.setOnDismissListener { super.onBackPressed() }
        builder.show()
    }
}