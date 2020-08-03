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
        holder.deviceSettings.setOnClickListener { device.openHomePage() }
    }

    override fun getItemCount() = devices.size()
}

