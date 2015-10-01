
public class ReceiverThread extends java.lang.Thread {
    Sender sender;

    public ReceiverThread(Sender sender) {
        this.sender = sender;
    }

    public void run() {
        sender.receivePackets();
    }
}