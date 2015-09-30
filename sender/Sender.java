import java.lang.Exception;
import java.lang.System;
import java.net.*;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Sender {
    int port = 9877;
    int destPort = 9876;
    ArrayList<DatagramPacket> ThePackets = new ArrayList<DatagramPacket>();
    int windowPoint;
    int windowSize;

    public void createPackets(int numberOfPackets) {
        for(int i = 0; i < numberOfPackets; i++) {
            try {
                InetAddress IPAddress = InetAddress.getByName("localhost");
                byte[] sendData = new byte[1024];
                DatagramPacket sendPkt = new DatagramPacket(sendData, sendData.length, IPAddress, destPort);
                ThePackets.add(sendPkt);
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        windowSize = numberOfPackets / 2;
        windowPoint = 0;
    }

    public void sendPacket() {
        try {
            // send the packet
            if(ThePackets.size() <= 0) {
                System.out.println("No packets in ThePackets list. ");
                System.exit(1);
            }
            DatagramSocket senderSocket = new DatagramSocket(port);
            for(int i = windowPoint; i < windowSize; i++){
                DatagramPacket currentPacket = ThePackets.remove(0);
                senderSocket.send(currentPacket);
                System.out.println("Packet Sent, " + currentPacket);
                TimeUnit.SECONDS.sleep(1);
            }

            // receive ack
            InetAddress IPAddress = InetAddress.getByName("localhost");
            byte[] rcvData = new byte[1];
            DatagramPacket rcvPkt = new DatagramPacket(rcvData, rcvData.length);
            senderSocket.receive(rcvPkt);
            IPAddress = rcvPkt.getAddress();
            int port = rcvPkt.getPort();
            System.out.println("Packet Received, length: " + rcvData.length + ", ip: " + IPAddress + ", port: "
                    + senderSocket.getPort() + ", name: " + rcvPkt.getData().toString());
        } catch (Exception e) {
            // System.out.println(e);
        }
    }

    public void printPackets() {
        System.out.println("Packets: ");
        for(int i = 0; i < ThePackets.size(); i++) {
            System.out.println(ThePackets.get(i));
        }
        System.out.println();
    }

    public static void main(String[] args) {
        System.out.println("Running...");
        Sender program = new Sender();
        program.createPackets(10);
        program.sendPacket();
        program.printPackets();
    }
}
