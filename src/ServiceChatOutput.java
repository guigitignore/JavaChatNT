public class ServiceChatOutput extends Thread implements IWorker,IPacketChatOutput{
    public final static int CAPACITY=100;

    private ServiceChat client;
    private IPacketChatOutput output;
    private PacketChatQueue queue=new PacketChatQueue(CAPACITY);

    public ServiceChatOutput(ServiceChat client,IPacketChatOutput output){
        this.client=client;
        this.output=output;

        WorkerManager.getInstance().registerAndStart(this);
    }

    public boolean getStatus() {
        return !isInterrupted();
    }

    public String getDescription() {
        return "ServiceChatOutput of "+client.getDescription();
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        queue.putPacketChat(packet);
    }

    
    private void handlePacket(PacketChat packet) throws PacketChatException{
        Logger.i("send packet: %s",packet);
        //send packet 
        output.putPacketChat(packet);
    }

    public void run() {

        while (true){
            try{
                PacketChat packet=queue.getPacketChat();
                handlePacket(packet);
            }catch(PacketChatException e){
                break;
            }
        }

        WorkerManager.getInstance().remove(this);
    }

    public void cancel() {
        interrupt();
    }
    
}
