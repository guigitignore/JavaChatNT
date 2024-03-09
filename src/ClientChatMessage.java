public class ClientChatMessage extends LoopWorker implements IPacketChatOutput{
    public ClientChat client;
    IPacketChatInterface messageInterface;

    public ClientChatMessage(ClientChat client){
        super(client);
    }

    public String getDescription() {
        return "ClientChatMessage";
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        messageInterface.putPacketChat(packet);
    }

    public void setup() throws Exception {
        client=(ClientChat)getArgs()[0];
    }

    public void init() throws Exception {
        messageInterface=new PacketChatTelnetInterface();
    }

    public void loop() throws Exception {
        PacketChat packet=messageInterface.getPacketChat();

        client.putPacketChat(packet);
    }

    public void end() throws Exception {
        WorkerManager.getInstance().cancelAll();
    }
    
}
