/**
 * Created by Josh on 9/26/15.
 */
public class Packet {
    private boolean recived = false;

    public boolean getRecived(){
        return recived;
    }

    public void recive(){
        recived = true;
    }
}
