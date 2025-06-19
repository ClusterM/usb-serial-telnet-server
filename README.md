# USB Serial Telnet Server
Android application that binds a USB serial converter to a Telnet client

Just connect a USB serial adapter into USB OTG port of your Android device, start this app and connect to it using any Telnet client like
* [JuiceSSH](https://play.google.com/store/apps/details?id=com.sonelli.juicessh) using the same Android device (connect to the localhost)
* [Termux](https://play.google.com/store/apps/details?id=com.termux) and standard Linux telnet client (also, connect to the localhost)
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

Some other device specific drivers:
* GsmModem devices, e.g. for Unisoc based Fibocom GSM modems
* Chrome OS CCD (Closed Case Debugging)

And devices implementing the generic CDC/ACM protocol like:
* Qinheng CH9102
* Microchip MCP2221
* Arduino using ATmega32U4
* Digispark using V-USB software USB
* ...

## Download
You can download USB Serial Telnet Server:
* On [Google Play](https://play.google.com/store/apps/details?id=com.clusterrr.usbserialtelnetserver) - best method, but new versions can be delayed
* At [releases page](https://github.com/ClusterM/usb-serial-telnet-server/releases) - always actual release version
* Also, you can download automatic interim builds at [http://clusterm.github.io/usb-serial-telnet-server/](http://clusterm.github.io/usb-serial-telnet-server/) - if you want to test some unreleased features, can be buggy, not signed

## Donate
* [Become a sponsor on GitHub](https://github.com/sponsors/ClusterM)
* [Buy Me A Coffee](https://www.buymeacoffee.com/cluster)
* [Donation Alerts](https://www.donationalerts.com/r/clustermeerkat)
* [Boosty](https://boosty.to/cluster)

## Contacts
* My site (Russian): https://cluster.wtf
* Email: cluster@cluster.wtf
* Telegram: https://t.me/Cluster_M
