package co.sedco.customhiddevice

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import co.sedco.customhiddevice.CustomHidDevice.Companion.DEVICE_BLINK
import co.sedco.customhiddevice.CustomHidDevice.Companion.DEVICE_LED1
import co.sedco.customhiddevice.CustomHidDevice.Companion.DEVICE_OFF
import co.sedco.usbhid.IUsbConnectionHandler


class  MainActivity : Activity() {
    companion object {
        private lateinit var hidDevice: CustomHidDevice
        const val TAG = "UsbControllerActivity"
    }

    private val mConnectionHandler: IUsbConnectionHandler = object :
        IUsbConnectionHandler {
        override fun onDeviceConnected() {
            Log.d(TAG,"onDeviceConnected")
            runOnUiThread {
                deviceStatusText.text = getString(R.string.connected)
            }
        }
        override fun onDeviceDisconnected() {
            Log.d(TAG,"onDeviceDisconnected")
            runOnUiThread {
                deviceStatusText.text = getString(R.string.disconnected)
            }
        }
        override fun onDeviceNotFound() {
            Log.d(TAG,"onDeviceNotFound")
            runOnUiThread {
                deviceStatusText.text = getString(R.string.not_found)
            }
        }
        override fun onDevicePermissionDenied() {
            Log.d(TAG,"onDevicePermissionDenied")
            runOnUiThread {
                deviceStatusText.text = getString(R.string.permission_denied)
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createView()
        hidDevice = CustomHidDevice(application.applicationContext, mConnectionHandler)
    }

    private lateinit var parentLayout: ViewGroup
    private lateinit var sensorText: TextView
    private lateinit var deviceStatusText: TextView
    private fun createView() {
        setContentView(R.layout.main)
        parentLayout = findViewById(R.id.parent_layout)
        sensorText = findViewById(R.id.sensor_text)
        deviceStatusText = findViewById(R.id.device_status_text)
        (findViewById<View>(R.id.led1_off) as Button).setOnClickListener {
            hidDevice.setDevice(DEVICE_LED1, DEVICE_OFF)
        }
        (findViewById<View>(R.id.led1_blink) as Button).setOnClickListener {
            hidDevice.setDevice(DEVICE_LED1, DEVICE_BLINK)
        }
        (findViewById<View>(R.id.read_sensor) as Button).setOnClickListener {
            val (result, value) = hidDevice.getSensor1()
            if (result) {
                sensorText.text = value.toString()
            }
        }
    }

}

