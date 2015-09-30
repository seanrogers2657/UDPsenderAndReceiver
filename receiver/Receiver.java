import java.net.*;
import java.util.concurrent.TimeUnit;

public class Receiver {
    int port = 9876;
    int destPort = 9877;

    public void receivePacket(){
        try {
            // receive the packet
            DatagramSocket receiverSocket = new DatagramSocket(port);
            byte[] rcvData = new byte[1024];
            DatagramPacket rcvPkt = new DatagramPacket(rcvData, rcvData.length);
            receiverSocket.receive(rcvPkt);
            InetAddress IPAddress = rcvPkt.getAddress();
            int port = rcvPkt.getPort();
            System.out.println("Received Packet, length: " + rcvPkt.getData().length + ", ip: " + IPAddress + ", port: " + receiverSocket.getPort());

            // send ack
            IPAddress = InetAddress.getByName("localhost");
            byte[] sendData = new byte[1];
            DatagramPacket sendPkt = new DatagramPacket(sendData, sendData.length, IPAddress, destPort);
            receiverSocket.send(sendPkt);
            System.out.println("Packet Sent, length: " + sendData.length + ", ip: " + IPAddress + ", port: " + receiverSocket.getPort());
        } catch (Exception e) {
            // System.out.println(e);
        }
    }

    public static void main(String[] args) {
        System.out.println("Running...");
        Receiver program = new Receiver();
        while(true) {
            program.receivePacket();
        }
    }
}
