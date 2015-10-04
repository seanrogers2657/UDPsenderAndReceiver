public class SenderThread extends java.lang.Thread {
    Sender sender;
	int number;

    public SenderThread(Sender sender, int number) {
        this.sender = sender;
		this.number = number;
    }

    public void run() {
        sender.sendPacket(number);
    }
}
