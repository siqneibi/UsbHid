package co.sedco.usbhid

interface IUsbConnectionHandler {
    fun onDeviceConnected()
    fun onDeviceDisconnected()
    fun onDeviceNotFound()
}