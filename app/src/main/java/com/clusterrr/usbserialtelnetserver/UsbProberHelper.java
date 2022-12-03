package com.clusterrr.usbserialtelnetserver;

public class UsbProberHelper {
  public static UsbSerialProber buildProberWithCustomTable() {
     ProbeTable customTable = new ProbeTable();
     int vendorId = 0x0403; // TODO: do not hardcode?
     int productId = 0xCC4D; // TODO: do not hardcode?
     customTable.addProduct(vendorId, productId, CdcAcmSerialDriver.class);
     UsbSerialProber prober = new UsbSerialProber(customTable);
     return prober;
  }
}
