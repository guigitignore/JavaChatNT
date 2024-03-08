public class ClientChatOutput extends LoopWorker implements IPacketChatOutput{
    private ClientChat client;
    private IPacketChatInterface packetInterface;

    public ClientChatOutput(ClientChat client,IPacketChatInterface packetInterface){
        super(client,packetInterface);
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        packetInterface.putPacketChat(packet);
    }

    public String getDescription() {
        return "ClientChatOutput";
    }

    public void setup() throws Exception {
        client=(ClientChat)getArgs()[0];
        packetInterface=(IPacketChatInterface)getArgs()[1];
    }

    public void init() throws Exception {}

    public void loop() throws Exception {
        PacketChat packet;

        packet=packetInterface.getPacketChat();

        putPacketChat(packet);
    }

    
    public void end() throws Exception {
        WorkerManager.getInstance().cancelAll();
    }
}
