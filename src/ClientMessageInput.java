public class ClientMessageInput extends LoopWorker implements IPacketChatOutput{
    private ClientChat client;

    public ClientMessageInput(ClientChat client){
        super(client);
    }

    public String getDescription() {
        return "ClientMessageInput";
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        
    }

    public void setup() throws Exception {
        client=(ClientChat)getArgs()[0];
    }

    
    public void init() throws Exception {}

    public void loop() throws Exception {
        PacketChat packet;
        packet=client.getBucket().waitPacketByType(PacketChat.AUTH,PacketChat.CHALLENGE,PacketChat.SEND_MSG);
        putPacketChat(packet);
    }

    public void end() throws Exception {}
    
}
