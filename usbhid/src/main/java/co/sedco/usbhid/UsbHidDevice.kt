package co.sedco.usbhid

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.util.Log

open class UsbHidDevice(appContext: Context, connectionHandler: IUsbConnectionHandler, vid: Int, pid: Int) {
    companion object {
        const val TAG = "UsbHidDevice"

        //USB permission
        const val ACTION_USB_PERMISSION = "co.sedco.android.USB_PERMISSION"
    }

    private val mApplicationContext: Context

    //USB connection monitoring, if it is your own USB, the connection will return true, and the disconnect will return false
    private var mConnectionHandler: IUsbConnectionHandler

    // Vendor ID
    var vendorId: Int

    // Device ID
    var productId: Int

    // USB management
    var mUsbManager: UsbManager

    // Register broadcast
    private var mPermissionIntent: PendingIntent? = null

    // Whether there is USB permission
    var hasPermission = false

    // USB device
    var device: UsbDevice? = null

    // Interface
    var usbInterface: UsbInterface? = null

    // Endpoint, write
    var usbEndpointOut: UsbEndpoint? = null

    // Endpoint, read
    var usbEndpointIn: UsbEndpoint? = null

    // Whether USB is found
    var status = false

    // Connect
    var usbConnection: UsbDeviceConnection? = null

    // Prompting the user whether to grant permission to use the USB device
    private val mUsbReceiver: BroadcastReceiver

    // Find USB device
    private fun find(vendorId: Int, productId: Int): Boolean {
        val deviceList = mUsbManager.deviceList
        if (deviceList.size == 0) Log.w(TAG,"No USB devices")
        val deviceIterator: Iterator<UsbDevice> = deviceList.values.iterator()
        var deviceTemp: UsbDevice
        while (deviceIterator.hasNext()) {
            deviceTemp = deviceIterator.next()
            if ((deviceTemp.vendorId == vendorId) && deviceTemp.productId == productId) {
                device = deviceTemp
                break
            }
        }
        if (device == null) {
            mConnectionHandler.onDeviceNotFound()
            return false
        }
        if (mUsbManager.hasPermission(device)) {
            open()
        } else {
            val mPermissionIntent = PendingIntent.getBroadcast(
                mApplicationContext, 0,
                Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_ONE_SHOT
            )
            mUsbManager.requestPermission(device, mPermissionIntent)
        }
        return true
    }

    private fun open(): Boolean {
        try {
            if (device == null)
                return false

            usbConnection = mUsbManager.openDevice(device)
            if (usbConnection == null)
                return false

            usbInterface = device?.getInterface(0)
            if (usbInterface == null)
                return false

            if (!usbConnection!!.claimInterface(usbInterface, true))
                return false

            for (ii in 0 until usbInterface!!.endpointCount) {
                val type = usbInterface!!.getEndpoint(ii).type
                val direction = usbInterface!!.getEndpoint(ii).direction
                val number = usbInterface!!.getEndpoint(ii).endpointNumber
                if (type == UsbConstants.USB_ENDPOINT_XFER_INT) {
                    if (direction == UsbConstants.USB_DIR_IN)
                        usbEndpointIn = usbInterface!!.getEndpoint(ii)
                    else
                        usbEndpointOut = usbInterface!!.getEndpoint(ii)
                }
            }

            if (usbEndpointOut == null)
                return false

            if (usbEndpointIn == null)
                return false

            status = true
            mConnectionHandler.onDeviceConnected()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Close the USB connection
     */
    fun close() {
        if (usbConnection != null) {
            if (usbInterface != null) {
                usbConnection?.releaseInterface(usbInterface)
                usbInterface = null
            }
            usbConnection?.close()
            usbConnection = null
        }
        usbEndpointIn = null
        usbEndpointOut = null
        device = null
        status = false
        mConnectionHandler.onDeviceDisconnected()
    }

    /**
     * Delay to close the USB connection, close the connection after sleep milliseconds
     * @sleep rest milliseconds
     */
    fun close(sleep: Long) {
        Thread.sleep(sleep)
        close()
    }

    @Synchronized
    fun sendCommandWaitResponse(data: ByteArray): Pair<Boolean, ByteArray> {
        if (!status || (usbConnection == null) || (usbEndpointOut == null) || (usbEndpointIn == null))
            return false to ByteArray(0)

        val response = ByteArray(64)
        var result = usbConnection?.bulkTransfer(usbEndpointOut, data, data.size, 0)
        if (result != -1) {
            result = usbConnection?.bulkTransfer(usbEndpointIn, response, response.size, 3000)
        }
        return (result != -1) to response
    }

    fun onDestroy() {
        close()
        mApplicationContext.unregisterReceiver(mUsbReceiver)
    }

    init {
        mApplicationContext = appContext
        mConnectionHandler = connectionHandler
        mUsbManager = mApplicationContext
            .getSystemService(Context.USB_SERVICE) as UsbManager
        vendorId = vid
        productId = pid
        mUsbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action

                // Permission
                if (ACTION_USB_PERMISSION == action) {
                    synchronized(this) {
                        val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (device != null) {
                                hasPermission = true
                                open()
                            }
                        }
                    }
                }

                // USB connect monitor
                if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                    val dev = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?
                    if (dev != null) {
                        //In case the user needs to judge both vendorId and productId
                        if (dev.vendorId == vendorId && dev.productId == productId) {
                            device = dev
                            if (mUsbManager.hasPermission(device)) {
                                open()
                            } else {
                                val mPermissionIntent = PendingIntent.getBroadcast(
                                    mApplicationContext, 0,
                                    Intent(ACTION_USB_PERMISSION), 0)
                                mUsbManager.requestPermission(device, mPermissionIntent)
                            }
                        }
                    }
                }

                // USB disconnect monitor
                if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                    val dev = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?
                    if (dev != null) {
                        if (device != null && device == dev) {
                            close()
                        }
                    }
                }
            }
        }

        // Initialize broadcast receiver
        mPermissionIntent =  PendingIntent.getBroadcast(mApplicationContext, 0, Intent(
            ACTION_USB_PERMISSION
        ), 0)
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        mApplicationContext.registerReceiver(mUsbReceiver, filter)

        // Find USB device
        find(vendorId, productId)
    }

}