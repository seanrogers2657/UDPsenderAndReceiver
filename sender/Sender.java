import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.Boolean;
import java.lang.Exception;
import java.lang.System;
import java.lang.reflect.Executable;
import java.net.*;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.nio.ByteBuffer;

public class Sender {
    String ip = "153.90.37.97";
    int port = 9877;
    int destPort = 9876;
    int numberOfPackets = 10;
    ArrayList<DatagramPacket> ThePackets = new ArrayList<DatagramPacket>();
    Boolean[] window = new Boolean[numberOfPackets];
    DatagramSocket senderSocket;
    int windowPoint = 0;
    int windowSize = 4;

    public void createPackets() {
        for(int i = 0; i < numberOfPackets; i++) {
            try {
                InetAddress IPAddress = InetAddress.getByName(ip);
                byte[] buffer = ByteBuffer.allocate(1024).putChar('p').putInt(i).putInt(10).array();
                DatagramPacket sendPkt = new DatagramPacket(buffer, buffer.length, IPAddress, destPort);
                ThePackets.add(sendPkt);
            } catch (Exception e) {
                System.out.println(e);
            }
            window[i] = false;
        }
    }

    public void sendConfirmation() throws Exception {
        // create packet
        senderSocket = new DatagramSocket(port);
        InetAddress IPAddress = InetAddress.getByName(ip);
        byte[] buffer = ByteBuffer.allocate(1024).putChar('c').putInt(4).putInt(10).array();
        DatagramPacket confirm = new DatagramPacket(buffer, buffer.length, IPAddress, destPort);

        // send packet
        senderSocket.send(confirm);
        System.out.println("Confirmation Packet Sent");

        // receive ack
        senderSocket.setSoTimeout(5000);
        byte[] rcvData = new byte[1024];
        DatagramPacket rcvPkt = new DatagramPacket(rcvData, rcvData.length);
        senderSocket.receive(rcvPkt);
        IPAddress = rcvPkt.getAddress();
        int port = rcvPkt.getPort();

        // read data
        ByteArrayInputStream bais = new ByteArrayInputStream(rcvPkt.getData());
        DataInputStream dis = new DataInputStream(bais);
        char type = dis.readChar();
        int size = dis.readInt();

        System.out.println("Packet Received, length: " + rcvData.length + ", ip: " + IPAddress + ", data: " + type + size);

        if(type == 'c' && size == 0) {
            System.out.println("Connection Confirmed.\n");
            (new ReceiverThread(this)).start();
            sendPackets();
        } else {
            System.out.println("BAD STUFF - Connection denied. ");
        }
    }

    public void sendPackets() {
        try {
            if(ThePackets.size() <= 0) {
                System.out.println("No packets in ThePackets list. ");
                System.exit(1);
            }
            if((windowPoint + windowSize) < window.length) {
                System.out.println("For loop out of bounds. ");
            }

            for(int i = windowPoint; i < windowPoint + windowSize; i++){
                // send the packet
                if(!window[i]) {
                    DatagramPacket currentPacket = ThePackets.get(i);
                    senderSocket.send(currentPacket);

                    // read data
                    ByteArrayInputStream bais = new ByteArrayInputStream(currentPacket.getData());
                    DataInputStream dis = new DataInputStream(bais);
                    char type = dis.readChar();
                    int size = dis.readInt();

                    System.out.println("Packet Sent, " + type + size);
                    printWindow();
                } else {
                    System.out.println("Already confirmed: " + i);
                }
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public Boolean goTime() {
        Boolean isItTrue = false;
        for(int i = 0; i < window.length; i++) {
            if(window[i] == false) {
                isItTrue = true;
                break;
            }
        }
        return isItTrue;
    }

    public void receivePackets() {
        // receive ack
        while(goTime()) {
            try {
                senderSocket.setSoTimeout(5000);
                byte[] rcvData = new byte[1024];
                DatagramPacket rcvPkt = new DatagramPacket(rcvData, rcvData.length);
                senderSocket.receive(rcvPkt);
                InetAddress IPAddress = rcvPkt.getAddress();
                int port = rcvPkt.getPort();

                ByteArrayInputStream bais = new ByteArrayInputStream(rcvPkt.getData());
                DataInputStream dis = new DataInputStream(bais);
                char type = dis.readChar();
                int size = dis.readInt();

                System.out.println("Packet Received, length: " + rcvData.length + ", ip: " + IPAddress + ", data: " + type + size);

                if(type == 'p' && (size >= windowPoint && size < window.length)) {
                    System.out.println("Increase Window Size..." + size + ", length: " + window.length);
                    window[size] = true;
                    windowPoint++;
                    sendPackets();
                }
                printWindow();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public void printPackets() {
        System.out.println("Packets: " + ThePackets.size());
//        for(int i = 0; i < ThePackets.size(); i++) {
//            System.out.println(ThePackets.get(i));
//        }
//        System.out.println();
    }

    public void printWindow() {
        System.out.println("The Window: ");
        for(int i = 0; i < window.length; i++) {
            System.out.println(window[i]);
        }
        System.out.println();
    }

    public static void main(String[] args) {
        try {
            System.out.println("Running...");
            Sender program = new Sender();
            program.createPackets();
            program.printPackets();
            program.sendConfirmation();
            program.printPackets();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
