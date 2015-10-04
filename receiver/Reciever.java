/**
 * Created by Josh on 9/23/15.
 */
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class Reciever {

    public static void main(String args[])throws Exception{
        new Reciever();
    }

    public Reciever()throws Exception{

        int PORT = 9876;
        int fport = 0;
        int windowSize = 0;
        int maxSequenceNum = 0;
        int pNum = -1;
        char type;
        String response;
        ByteArrayInputStream bais;
        DataInputStream input;
        InetAddress fAddress;
        byte[] rcvData = new byte[1024];
        byte[] sendData = new byte[0];
        DatagramPacket rcvPkt = new DatagramPacket(rcvData, rcvData.length);
        DatagramPacket sendPkt;
        DatagramSocket receiverSocket = new DatagramSocket(PORT);

        //The main reciever loop
        while(true){

            //This is to make the first handshake with the sender to agree upon
            // the window size and the max Sequence number for the duration of the connection
            while(true){
                receiverSocket.receive(rcvPkt);
                fport = rcvPkt.getPort();
                fAddress = rcvPkt.getAddress();
                bais = new ByteArrayInputStream(rcvPkt.getData());
                input = new DataInputStream(bais);
                type = input.readChar();
                windowSize = input.readInt();
                maxSequenceNum = input.readInt();
                System.out.println("FirstPack type: " + type + " windowS: " + windowSize + " Sequnce: " + maxSequenceNum);

                // if everything is good send a c0 for the acknowledgment
                if(type == 'c' && windowSize <= (maxSequenceNum/2)){
                    byte[] buffer = ByteBuffer.allocate(1024).putChar('c').putInt(0).array();
                    sendPkt = new DatagramPacket(buffer, buffer.length, fAddress, fport);
                    //System.out.println("Sent c0");
                    receiverSocket.send(sendPkt);
                    break;
                }
                else{
                    // assumes a packet acknowledgments were not received by the server. and send the acknowledgment
                    if(type == 'p'){
                        byte[] buffer = ByteBuffer.allocate(1024).putChar('p').putInt(windowSize).array();
                        sendPkt = new DatagramPacket(buffer, buffer.length, fAddress, fport);
                        System.out.println("responding to missed ack");
                        receiverSocket.send(sendPkt);
                    }
                    else{
                        int error;
                        // sending a c1 for when the type is neither a c or p. to indicate that a wrong type was sent
                        if( type != 'c'){
                            error = 1;
                        }

                        //If the windowSize was more than half the maxSequenceNum send a error
                        else{
                            error = 2;
                        }
                        byte[] buffer = ByteBuffer.allocate(1024).putChar('c').putInt(error).array();
                        sendPkt = new DatagramPacket(buffer, buffer.length, fAddress, fport);
                        System.out.println("Sender was in error");
                        receiverSocket.send(sendPkt);
                    }

                }
            }

            //creates the window and the list of expected packets
            Packet list[] = new Packet[maxSequenceNum];
            for(int i = 0; i < maxSequenceNum; i++){
                list[i] = new Packet();
            }

            int shift = 0;
            while(true){
                //System.out.println("InLoop");
                receiverSocket.receive(rcvPkt);
                //System.out.println("gotPacket");
                if( fAddress == rcvPkt.getAddress() && fport == rcvPkt.getPort()){
                    bais = new ByteArrayInputStream(rcvPkt.getData());
                    input = new DataInputStream(bais);
                    type = input.readChar();
                    pNum = input.readInt();
                   // System.out.println("FirstPack type: " + type + " number: " + pNum);

                    // packet of right thype
                    if(type =='p'){
                        if(pNum < shift + windowSize && pNum < list.length){
                            // already recieved
                            if(list[pNum].getRecived() == true){
                                System.out.println("Packet " + pNum +
                                        " was already recieved, send Ack" + pNum +
                                        " window " + printWindow(list,shift,windowSize));
                                byte[] buffer = ByteBuffer.allocate(1024).putChar('p').putInt(pNum).array();
                                sendPkt = new DatagramPacket(buffer, buffer.length, fAddress, fport);
                                //TimeUnit.SECONDS.sleep(1);
                                receiverSocket.send(sendPkt);
                            }
                            // has not been recieved yet
                            else{
                                list[pNum].recive();
                                if(pNum == shift){
                                    while( shift < list.length && list[shift].getRecived() == true){
                                        shift += 1;
                                    }
                                }
                                System.out.println("Packet " + pNum +
                                        " is recieved, send Ack" + pNum +
                                        " window " + printWindow(list,shift,windowSize));
                                byte[] buffer = ByteBuffer.allocate(1024).putChar('p').putInt(pNum).array();
                                sendPkt = new DatagramPacket(buffer, buffer.length, fAddress, fport);
                                receiverSocket.send(sendPkt);
                            }
                        }
                        // if a packet is sent out of the window
                        else{
                            System.out.println("Packet " + pNum +
                                    " Is an Error!" + " window " + printWindow(list,shift,windowSize));
                        }
                    }
                    // The acknowledgment was not received by the sender
                    else if(type =='c'){
                        byte[] buffer = ByteBuffer.allocate(1024).putChar('c').putInt(0).array();
                        sendPkt = new DatagramPacket(buffer, buffer.length, fAddress, fport);
                        receiverSocket.send(sendPkt);
                    }
                    else{
                        System.out.println("Received packet with type " + type + "this is an unacceptable type");
                    }

                    // all packets are received from the sender
                    if(shift == maxSequenceNum){
                        break;
                    }
                }
            }

        }

    }


    //Prints out the current window status
    public String printWindow(Packet [] list, int shift, int wSize ){
        String window = "[";
        for(int i = shift; i < (shift + wSize); i++){
            if(i < list.length){
                window += i;
                if(list[i].getRecived()){
                    window+= '#';
                }
            }
            else{
                window += '-';
            }
            if(i != (shift + wSize -1)){
                window += ',';
            }
            else{
                window += ']';
            }
        }
        return window;
    }
}
