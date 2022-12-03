FROM androidsdk/android-31
# install deps
RUN apt-get update && apt-get install -y build-essential wget unzip && rm -rf /var/lib/apt/lists/*
# workdir
WORKDIR /opt
# download r25 ndk
RUN wget https://dl.google.com/android/repository/android-ndk-r25b-linux.zip # 25.1.8937393
RUN unzip android-ndk-r25b-linux.zip
RUN rm android-ndk-r25b-linux.zip
# gradle
RUN wget https://downloads.gradle-dn.com/distributions/gradle-7.3.2-bin.zip
RUN unzip gradle-7.3.2-bin.zip
RUN rm gradle-7.3.2-bin.zip
# set env vars
ENV ANDROID_NDK_ROOT=/opt/android-ndk-r25b
ENV ANDROID_NDK_HOME=/opt/android-ndk-r25b
ENV ANDROID_HOME=/opt/android-sdk-linux
ENV ANDROID_TOOLS=/opt/android-sdk-linux/tools/bin
ENV ANDROID_PLATFORM_TOOLS=/opt/android-sdk-linux/platform-tools
ENV GRADLE_PATH=/opt/gradle-7.3.2/bin
ENV PATH=$PATH:$ANDROID_HOME:$ANDROID_TOOLS:$ANDROID_PLATFORM_TOOLS:$ANDROID_NDK_HOME:$GRADLE_PATH
# docker run --rm -it -v $(pwd):/mnt 32032a83e2c3 bash
# gradle assembleDebug