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

## Docker

```dockerfile
FROM androidsdk/android-31
# install deps
RUN apt-get update && apt-get install -y build-essential && rm -rf /var/lib/apt/lists/*
# download r25 ndk
WORKDIR /opt
RUN wget https://dl.google.com/android/repository/android-ndk-r25b-linux.zip # 25.1.8937393
RUN unzip android-ndk-r25b-linux.zip
RUN rm android-ndk-r25b-linux.zip
# set env vars
ENV ANDROID_NDK_ROOT=/opt/android-ndk-r25b
ENV ANDROID_NDK_HOME=/opt/android-ndk-r25b
ENV ANDROID_HOME=/opt/android-sdk-linux
ENV ANDROID_TOOLS=/opt/android-sdk-linux/tools/bin
ENV ANDROID_PLATFORM_TOOLS=/opt/android-sdk-linux/platform-tools
ENV PATH=$PATH:$ANDROID_HOME:$ANDROID_TOOLS:$ANDROID_PLATFORM_TOOLS:$ANDROID_NDK_HOME
```