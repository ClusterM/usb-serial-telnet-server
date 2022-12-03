# USB Serial Telnet Server
Android application that binds a USB serial converter to a Telnet client

Just connect a USB serial adapter into USB OTG port of your Android device, start this app and connect to it using any Telnet client like
* [JuiceSSH](https://play.google.com/store/apps/details?id=com.sonelli.juicessh) using the same Android device (connect to localhost)
* Telnet client on a computer on the same network (connect over Wi-Fi)

![Screenshot](https://user-images.githubusercontent.com/4236181/170989042-b82958ff-372b-4733-bbcb-ce98ebf331fc.png)

This method allows to use all console features like colors and special keys. So you can easily control/install something like network devices with serial port using only your Android device. Also, you can use it as remote console transmitter.

![Photo](https://user-images.githubusercontent.com/4236181/170874522-11253639-8eb8-4a95-b70d-e875a2f2baad.jpg)

## Compatible Devices
This app uses [usb-serial-for-android  library by mik3y](https://github.com/mik3y/usb-serial-for-android) and supports USB to serial converter chips:
* FTDI FT232R, FT232H, FT2232H, FT4232H, FT230X, FT231X, FT234XD
* Prolific PL2303
* Silabs CP2102 and all other CP210x
* Qinheng CH340, CH341A

and devices implementing the CDC/ACM protocol like
* Arduino using ATmega32U4
* Digispark using V-USB software USB
* BBC micro:bit using ARM mbed DAPLink firmware
* ...

## Download on Google Play

https://play.google.com/store/apps/details?id=com.clusterrr.usbserialtelnetserver

## Contacts
* My site (Russian): https://clusterrr.com
* Email: clusterrr@clusterrr.com
* Telegram: https://t.me/Cluster_M
* Donate: https://www.donationalerts.com/r/clustermeerkat

## Docker + adb Usage Example

```shell
docker build -t android-builder:0.0.1 .
docker run --rm -it -v $(pwd):/mnt android-builder:0.0.1 bash "cd /mnt && gradle assembleDebug"
adb uninstall com.clusterrr.usbserialtelnetserver
adb install ~/Desktop/UsbSerialTelnetServer-*.apk # from app/build/outputs/apk/debug/UsbSerialTelnetServer-*.apk
adb shell monkey -p com.clusterrr.usbserialtelnetserver 1
adb logcat
python -m http.server --directory examples/ 8080
# go to http://localhost:8080 in browser
```