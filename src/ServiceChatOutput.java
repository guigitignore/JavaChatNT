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

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        if (!queue.add(packet)) throw new PacketChatException("Cannot queue packet");
    }

    

    private void handlePacket(PacketChat packet) throws PacketChatException{
        
        //send packet 
        output.putPacketChat(packet);
    }

    public void run() {

        while (true){
            try{
                PacketChat packet=queue.take();
                try{
                    handlePacket(packet);
                }catch(PacketChatException e){
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
