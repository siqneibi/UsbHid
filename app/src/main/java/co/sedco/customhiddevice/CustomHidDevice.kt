package co.sedco.customhiddevice

import android.content.Context
import co.sedco.usbhid.IUsbConnectionHandler
import co.sedco.usbhid.UsbHidDevice

class CustomHidDevice(appContext: Context, connectionHandler: IUsbConnectionHandler): UsbHidDevice(appContext, connectionHandler, MyVendorId, MyProductId) {
    companion object {
        const val MyVendorId = 0x1F52
        const val MyProductId = 0x0010

        const val TAG = "CustomHidDevice"
        const val SET_DEVICE_COMMAND = 0xC4.toByte()
        const val GET_DEVICE_COMMAND = 0xC5.toByte()

        var DEVICE_LED1: Byte = 0x00
        var DEVICE_LED2: Byte = 0x04
        var DEVICE_OFF: Byte = 0x00
        var DEVICE_ON: Byte = 0x01
        var DEVICE_BLINK: Byte = 0x02
        var DEVICE_SENSOR1: Byte = 0x40
        var DEVICE_SENSOR2: Byte = 0x44
    }

    //
    // Public functions
    //
    fun setDevice(devNum: Byte, state: Byte) {
        val array = byteArrayOf(3, SET_DEVICE_COMMAND, devNum, state)
        sendCommandWaitResponse(array)
    }

    fun getSensor1(): Pair<Boolean, Byte> {
        val array = byteArrayOf(2, GET_DEVICE_COMMAND, DEVICE_SENSOR1)
        var value: Byte = 1
        val (result, response) = sendCommandWaitResponse(array)
        if (result) {
            if ((response[0] == 2.toByte()) &&
                (response[1] == GET_DEVICE_COMMAND)) {
                value = response[2]
            }
        }
        return result to value
    }
}