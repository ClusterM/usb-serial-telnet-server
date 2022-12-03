package com.clusterrr.usbserialtelnetserver;

import android.util.Log;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class WebSocketService {
  private InputStream inputStream = null;
  private OutputStream outputStream = null;
  private UsbSerialTelnetService usbSerialTelnetService = null;

  public WebSocketService(InputStream inputStream, OutputStream outputStream, UsbSerialTelnetService usbSerialTelnetService) {
    this.inputStream = inputStream;
    this.outputStream = outputStream;
    this.usbSerialTelnetService = usbSerialTelnetService;
  }

  public void start() {
    // handshake
    try {
      doHandShakeToInitializeWebSocketConnection(inputStream, outputStream);
    } catch (Exception exception) {
      throw new IllegalStateException("Could not connect to client input stream", exception);
    }
    // input stream
    try {
      handleInputStream(inputStream);
    } catch (Exception exception) {
      throw new IllegalStateException("Handle input stream failed", exception);
    }
  }

  private byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte)((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }

  private void onMessageReceived(String messageHex) {
    try {
      // TODO: hex to binary or something?
      Log.i(UsbSerialTelnetService.TAG, "Writing to serial port: " + messageHex);
      byte[] messageBytes = hexStringToByteArray(messageHex);
      usbSerialTelnetService.writeSerialPort(messageBytes);
    } catch (Exception exception) {
      throw new IllegalStateException("Write serial port failed", exception);
    }
  }

  public void sendMessage(String messageHex) {
    try {
      Log.i(UsbSerialTelnetService.TAG, "Writing to socket: " + messageHex);
      outputStream.write(encode(messageHex));
      outputStream.flush();
    } catch (Exception exception) {
      throw new IllegalStateException("Write output stream failed", exception);
    }
  }

  //Source for encoding and decoding:
  //https://stackoverflow.com/questions/8125507/how-can-i-send-and-receive-websocket-messages-on-the-server-side
  //this will handle incoming text only up to 64K only
  //it will handle multiple messages in one read and messages split over a read
  private void handleInputStream(InputStream inputStream) throws IOException {
    byte[] b = new byte[8000]; //incoming buffer
    byte[] message = null; //buffer to assemble message in
    byte[] masks = new byte[4];
    boolean isSplit = false; //has a message been split over a read
    int length = 0; //length of message 
    int totalRead = 0; //total read in message so far
    while (true) {
      int len = 0; //length of bytes read from socket
      try {
        len = inputStream.read(b);
      } catch (IOException e) {
        break;
      }
      if (len != -1) {
        boolean more = false;
        int totalLength = 0;
        do {
          int j = 0;
          int i = 0;
          if (!isSplit) {
            byte rLength = 0;
            int rMaskIndex = 2;
            int rDataStart = 0;
            // b[0] assuming text
            byte data = b[1];
            byte op = (byte) 127;
            rLength = (byte)(data & op);
            length = (int) rLength;
            if (rLength == (byte) 126) {
              rMaskIndex = 4;
              length = Byte.toUnsignedInt(b[2]) << 8;
              length += Byte.toUnsignedInt(b[3]);
            } else if (rLength == (byte) 127)
              rMaskIndex = 10;
            for (i = rMaskIndex; i < (rMaskIndex + 4); i++) {
              masks[j] = b[i];
              j++;
            }

            rDataStart = rMaskIndex + 4;

            message = new byte[length];
            totalLength = length + rDataStart;
            for (i = rDataStart, totalRead = 0; i < len && i < totalLength; i++, totalRead++) {
              message[totalRead] = (byte)(b[i] ^ masks[totalRead % 4]);
            }

          } else {
            for (i = 0; i < len && totalRead < length; i++, totalRead++) {
              message[totalRead] = (byte)(b[i] ^ masks[totalRead % 4]);
            }
            totalLength = i;
          }

          if (totalRead < length) {
            isSplit = true;
          } else {
            isSplit = false;
            onMessageReceived(new String(message));
            b = new byte[8000];
          }

          if (totalLength < len) {
            more = true;
            for (i = totalLength, j = 0; i < len; i++, j++)
              b[j] = b[i];
            len = len - totalLength;
          } else
            more = false;
        } while (more);
      } else
        break;
    }
  }

  private byte[] encode(String mess) throws IOException {
    byte[] rawData = mess.getBytes();

    int frameCount = 0;
    byte[] frame = new byte[10];

    frame[0] = (byte) 129;

    if (rawData.length <= 125) {
      frame[1] = (byte) rawData.length;
      frameCount = 2;
    } else if (rawData.length >= 126 && rawData.length <= 65535) {
      frame[1] = (byte) 126;
      int len = rawData.length;
      frame[2] = (byte)((len >> 8) & (byte) 255);
      frame[3] = (byte)(len & (byte) 255);
      frameCount = 4;
    } else {
      frame[1] = (byte) 127;
      long len = rawData.length; //note an int is not big enough in java
      frame[2] = (byte)((len >> 56) & (byte) 255);
      frame[3] = (byte)((len >> 48) & (byte) 255);
      frame[4] = (byte)((len >> 40) & (byte) 255);
      frame[5] = (byte)((len >> 32) & (byte) 255);
      frame[6] = (byte)((len >> 24) & (byte) 255);
      frame[7] = (byte)((len >> 16) & (byte) 255);
      frame[8] = (byte)((len >> 8) & (byte) 255);
      frame[9] = (byte)(len & (byte) 255);
      frameCount = 10;
    }

    int bLength = frameCount + rawData.length;

    byte[] reply = new byte[bLength];

    int bLim = 0;
    for (int i = 0; i < frameCount; i++) {
      reply[bLim] = frame[i];
      bLim++;
    }
    for (int i = 0; i < rawData.length; i++) {
      reply[bLim] = rawData[i];
      bLim++;
    }

    return reply;
  }

  private static void doHandShakeToInitializeWebSocketConnection(InputStream inputStream, OutputStream outputStream) throws UnsupportedEncodingException, NoSuchAlgorithmException, IOException {
    String data = new Scanner(inputStream, "UTF-8").useDelimiter("\\r\\n\\r\\n").next();
    Matcher get = Pattern.compile("^GET").matcher(data);
    if (get.find()) {
      Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
      match.find();
      byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n" +
        "Connection: Upgrade\r\n" +
        "Upgrade: websocket\r\n" +
        "Sec-WebSocket-Accept: " +
        Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8"))) +
        "\r\n\r\n").getBytes("UTF-8");
      outputStream.write(response, 0, response.length);
    }
  }
}