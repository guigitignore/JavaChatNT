public class ServiceChatOutput  extends LoopWorker implements IPacketChatOutput{
    public final static int CAPACITY=100;

    private ServiceChat client;
    private IPacketChatInterface queue;

    public ServiceChatOutput(ServiceChat client){
        super(client);
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
        client.putPacketChat(packet);
    }

    public void setup() throws Exception {
        this.queue=new PacketChatQueue(CAPACITY);
        this.client=(ServiceChat)getArgs()[0];
    }

    public void loop() throws Exception {
        PacketChat packet=queue.getPacketChat();
        handlePacket(packet);
    }

    public void end() throws Exception {}

    public void init() throws Exception {}

    
}
