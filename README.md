# UsbHid
Another custom USB HID host library for Android. Inspired by:
https://android.serverbox.ch/?p=549
https://github.com/benlypan/UsbHid
https://github.com/yutils/yutils/blob/master/utils/src/main/java/com/yujing/utils/YUsb.kt

# Benefits
Register device vid:pid and the library will notify you, i.e. via events,  if device is not found, connected, or disconnected.
The library will request permission to use device (if needed)
You can extend the UsbHidDevice class and include all your device related commands in the new class (see example app)

# QuickStart
Create a connection handler in your activity
```
    private val mConnectionHandler: IUsbConnectionHandler = object :
        IUsbConnectionHandler {
        override fun onDeviceConnected() {
            Log.d(TAG,"onDeviceConnected")
        }
        override fun onDeviceDisconnected() {
            Log.d(TAG,"onDeviceDisconnected")
        }
        override fun onDeviceNotFound() {
            Log.d(TAG,"onDeviceNotFound")
        }
    }
```

Extend the the UsbHidDevice class with your own custom behavior
```
class CustomHidDevice(appContext: Context, connectionHandler: IUsbConnectionHandler): UsbHidDevice(appContext, connectionHandler, MyVendorId, MyProductId) {
```

And finally instantiate the CustomHidDevice class in your activity and communicate with your HID device
```
    hidDevice = CustomHidDevice(application.applicationContext, mConnectionHandler)
    hidDevice.setDevice(DEVICE_LED1, DEVICE_BLINK)
    val (result, value) = hidDevice.getDevice(DEVICE_SENSOR1)
    
```

For details, please see example application.

# License
MIT