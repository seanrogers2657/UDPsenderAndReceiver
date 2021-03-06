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
import java.util.*;

public class Sender {
    //String ip = "localhost";
    String ip = "153.90.39.124";
    int port = 9877;
    int destPort = 9876;
    int numberOfPackets = 10;
    ArrayList<DatagramPacket> ThePackets = new ArrayList<DatagramPacket>();
    int[] window;
	ArrayList<Integer> dont = new ArrayList<Integer>();
    DatagramSocket senderSocket;
    int windowPoint = 0;
    int windowSize = 5;
	int timeout = 3;

    public void createPackets() {
        for(int i = 0; i < numberOfPackets; i++) {
            try {
                InetAddress IPAddress = InetAddress.getByName(ip);
                byte[] buffer = ByteBuffer.allocate(1024).putChar('p').putInt(i).array();
                DatagramPacket sendPkt = new DatagramPacket(buffer, buffer.length, IPAddress, destPort);
                ThePackets.add(sendPkt);
            } catch (Exception e) {
                System.out.println(e);
            }
            window[i] = 0;
        }
    }

    public void sendConfirmation() {
        try {
			// create packet
			InetAddress IPAddress = InetAddress.getByName(ip);
	        byte[] buffer = ByteBuffer.allocate(1024).putChar('c').putInt(windowSize).putInt(numberOfPackets).array();
	        DatagramPacket confirm = new DatagramPacket(buffer, buffer.length, IPAddress, destPort);

	        // send packet
	        senderSocket.send(confirm);
	        System.out.println("Confirmation Packet Sent");

	        // receive ack
	        byte[] rcvData = new byte[1024];
	        DatagramPacket rcvPkt = new DatagramPacket(rcvData, rcvData.length);
			senderSocket.setSoTimeout(5000);
	        senderSocket.receive(rcvPkt);
	        IPAddress = rcvPkt.getAddress();
	        int port = rcvPkt.getPort();

	        // read data
	        ByteArrayInputStream bais = new ByteArrayInputStream(rcvPkt.getData());
	        DataInputStream dis = new DataInputStream(bais);
	        char type = dis.readChar();
	        int size = dis.readInt();

	        System.out.println("Packet Received, length: " + rcvData.length + ", ip: " + IPAddress + ", data: " + type + size);

			senderSocket.setSoTimeout(0);
	        if(type == 'c' && size == 0) {
	            System.out.println("Connection Confirmed.\n");
	            (new ReceiverThread(this)).start();
	            sendPackets();
	        } else {
	            System.out.println("Connection denied. ");
				sendConfirmation();
	        }
		} catch (Exception e) {
			sendConfirmation();
		}
    }

	public boolean checkIfExists(int number) {
		int index = dont.indexOf(number);
		if (index > -1) {
			dont.remove(index);
			return true;
		}
		return false;
	}

	public void sendPackets() throws Exception {
		while (goTime()) {
			for (int i = windowPoint; (i < windowPoint + windowSize) && (i < window.length); i++) {
				if (window[i] == 0) {
					window[i] = 1;
					(new SenderThread(this, i)).start();
				}
			}
			TimeUnit.MILLISECONDS.sleep(50);
		}
	}

	public void sendPacket(int number) {
		while (window[number] != 2){
			try {
				if(ThePackets.size() <= 0) {
	                System.out.println("No packets in ThePackets list. ");
	                System.exit(1);
	            }
				DatagramPacket currentPacket = ThePackets.get(number);

				// send the packet
				if(window[number] != 2 && number >= windowPoint && number < windowPoint + windowSize && !checkIfExists(number)) {
					senderSocket.send(currentPacket);
				}
				// read data
				ByteArrayInputStream bais = new ByteArrayInputStream(currentPacket.getData());
				DataInputStream dis = new DataInputStream(bais);
				char type = dis.readChar();
				int size = dis.readInt();

				System.out.println("Packet Sent, " + type + size + " " + getWindow());

	            TimeUnit.SECONDS.sleep(timeout);

				if (window[number] != 2) {
					System.out.println("Packet " + number + " times out, resend packet " + number);
				}
			} catch (Exception e) {
				System.out.println(e);
			}
		}
	}

    public Boolean goTime() {
        Boolean runIt = false;
        for(int i = 0; i < window.length; i++) {
            if(window[i] != 2) {
                runIt = true;
                break;
            }
        }
		if (!runIt) {
			System.out.println("DEAD");
		}
        return runIt;
    }

    public void receivePackets() {
        // receive ack
        while(goTime()) {
            try {
                byte[] rcvData = new byte[1024];
                DatagramPacket rcvPkt = new DatagramPacket(rcvData, rcvData.length);
                senderSocket.receive(rcvPkt);
                InetAddress IPAddress = rcvPkt.getAddress();
                int port = rcvPkt.getPort();

                ByteArrayInputStream bais = new ByteArrayInputStream(rcvPkt.getData());
                DataInputStream dis = new DataInputStream(bais);
                char type = dis.readChar();
                int size = dis.readInt();

                System.out.println("Packet Received, length: " + rcvData.length + ", ip: " + IPAddress + ", data: " + type + size + " " + getWindow());

                if(type == 'p' && (size >= windowPoint && size < window.length)) {
					window[size] = 2;
					if (size == windowPoint) {
						while (size < window.length && window[size] == 2) {
							windowPoint++;
							size++;
						}
					}
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

	public String getWindow() {
		String theString = "[";
		for (int i = windowPoint; i < windowPoint + windowSize; i++) {
			if (i < numberOfPackets) {
				theString += i;
				if (window[i] == 1) {
					theString += "*";
				} else if (window[i] == 2) {
					theString += "#";
				}
			} else {
				theString += "-";
			}
			if (i != windowPoint + windowSize - 1) {
				theString += ",";
			}
		}
		theString += "]";
		return theString;
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
            System.out.print(window[i] + ", ");
        }
        System.out.println();
    }

    public static void main(String[] args) {
		while (true) {
			try {
	            System.out.println("Running...");
	            Sender program = new Sender();

				Scanner scan = new Scanner(System.in);
				System.out.print("Enter number of packets: ");
				String a = scan.nextLine();
				program.numberOfPackets = Integer.parseInt(a);

				do {
					System.out.print("Enter window size: ");
					String b = scan.nextLine();
					program.windowSize = Integer.parseInt(b);
				}
				while (program.windowSize > (program.numberOfPackets / 2));

				System.out.print("Enter packets to be dropped: ");
				String c = scan.nextLine();
				if (!c.isEmpty()) {
					String temp[] = c.split(" ");

					for (int i = 0; i < temp.length; i++) {
						program.dont.add(Integer.parseInt(temp[i]));
					}
				}

				program.senderSocket = new DatagramSocket(program.port);

				program.window = new int[program.numberOfPackets];
	            program.createPackets();
	            program.sendConfirmation();
				break;
	        } catch (Exception e) {
	            System.out.println(e);
	        }
		}
    }
}
