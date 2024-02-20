import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ServiceChatOutput extends Thread implements IWorker,IPacketChatOutput{
    public final static int CAPACITY=100;

    private ServiceChat client;
    private IPacketChatOutput output;
    private BlockingQueue<PacketChat> queue=new ArrayBlockingQueue<>(CAPACITY);

    public ServiceChatOutput(ServiceChat client,IPacketChatOutput output){
        this.client=client;
        this.output=output;

        WorkerManager.getInstance().registerAndStart(this);
    }

    public boolean getStatus() {
        return !isInterrupted();
    }

    public String getDescription() {
        return "ServiceChatOutput of"+client.getDescription();
    }

    public void putPacketChat(PacketChat packet) throws IOException {
        if (!queue.add(packet)) throw new IOException("Cannot queue packet");
    }

    private void logoutPacketHandler(PacketChat packet) throws IOException{
        byte command=packet.getCommand();
        if (command!=PacketChat.AUTH && command!=PacketChat.CHALLENGE){
            throw new IOException(String.format("unauthorized packet for logout user: %s", packet));
        }
    }

    private void loginPacketHandler(PacketChat packet) throws IOException{
        switch(packet.getCommand()){

        }
    }

    private void handlePacket(PacketChat packet) throws IOException{
        if (client.getUser()==null){
            logoutPacketHandler(packet);
        }else{
            loginPacketHandler(packet);
        }
        //send packet after filtering
        output.putPacketChat(packet);
    }

    public void run() {

        while (true){
            try{
                PacketChat packet=queue.take();
                try{
                    handlePacket(packet);
                }catch(IOException e){
                    if (client.getUser()==null){
                        Logger.w("Packet dropped on output for %s: %s",client.getDescription(),e.getMessage());
                    }else{
                        Logger.w("Packet dropped on output for user \"%s\" : %s",client.getUser().getName(),e.getMessage());
                    }
                }

            }catch(InterruptedException e){
                break;
            }
        }

        WorkerManager.getInstance().remove(this);
    }

    public void cancel() {
        interrupt();
    }
    
}
