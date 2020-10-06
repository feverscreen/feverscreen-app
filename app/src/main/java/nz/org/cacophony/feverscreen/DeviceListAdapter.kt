package nz.org.cacophony.feverscreen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceListAdapter(private val devices: DeviceList) : RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val deviceNameView = v.findViewById(R.id.device_name) as TextView
        val clickDevice = v.findViewById(R.id.device_info) as LinearLayout
        val deviceSettings = v.findViewById(R.id.device_settings) as ImageView
        val usbConnection = v.findViewById(R.id.device_connection_usb) as ImageView
        val wifiConnection = v.findViewById(R.id.device_connection_wifi) as ImageView
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): DeviceViewHolder {
        val rowView = LayoutInflater.from(parent.context)
                .inflate(R.layout.device_row, parent, false)
        return DeviceViewHolder(rowView)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices.elementAt(position)
        holder.deviceNameView.text = device.name
        holder.clickDevice.setOnClickListener { device.openFeverPage() }
        holder.deviceSettings.setOnClickListener { device.openSettingsIntent() }
        when (device.connectionInterface) {
            "usb" -> {
                holder.usbConnection.visibility = View.VISIBLE
                holder.wifiConnection.visibility = View.GONE
            }
            "wifi" -> {
                holder.wifiConnection.visibility = View.VISIBLE
                holder.usbConnection.visibility = View.GONE
            }
            else -> {
                holder.wifiConnection.visibility = View.GONE
                holder.usbConnection.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = devices.size()
}

