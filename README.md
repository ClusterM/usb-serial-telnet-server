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

## Contacts
* My site (Russian): https://clusterrr.com
* Email: clusterrr@clusterrr.com
* Telegram: https://t.me/Cluster_M
* Donations: https://www.donationalerts.com/r/clustermeerkat
