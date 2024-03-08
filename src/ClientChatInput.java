public class ClientChatInput implements IPacketChatOutput{
    private ClientChat client;
    private IPacketChatOutput messageOutput;

    public ClientChatInput(ClientChat client,IPacketChatOutput messageOutput){
        this.client=client;
        this.messageOutput=messageOutput;
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        byte command=packet.getCommand();

        if (command==PacketChat.FILE_INIT || command==PacketChat.FILE_DATA || command==PacketChat.FILE_OVER){
            client.getFile().putPacketChat(packet);
        }else{
            messageOutput.putPacketChat(packet);
        }
    }

    
    
}
