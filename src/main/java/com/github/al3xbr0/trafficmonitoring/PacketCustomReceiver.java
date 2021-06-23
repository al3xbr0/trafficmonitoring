package com.github.al3xbr0.trafficmonitoring;

import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;
import org.pcap4j.core.*;

public class PacketCustomReceiver extends Receiver<Integer> {

    private static final int SNAPLEN = 64 * 1024;
    private static final int READ_TIMEOUT = 10;


    private final String nifName;
    private final String filter;

    public PacketCustomReceiver(String nifName, String filter) {
        super(StorageLevel.MEMORY_AND_DISK_2());
        this.nifName = nifName;
        this.filter = filter;
    }

    @Override
    public void onStart() {
        new Thread(this::capture).start();
    }

    @Override
    public void onStop() {
    }

    private void capture() {
        try {
            PcapNetworkInterface nif = Pcaps.getDevByName(nifName);
            try (PcapHandle handle = nif.openLive(SNAPLEN, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, READ_TIMEOUT)) {
                handle.setFilter(filter, BpfProgram.BpfCompileMode.OPTIMIZE);
                PacketListener listener =
                        packet ->
                                store(packet.length());

                try {
                    handle.loop(-1, listener);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (PcapNativeException | NotOpenException e) {
            restart("Problems with pcap", e, 5 * 1000);
        }
    }
}
