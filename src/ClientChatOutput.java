public class ClientChatOutput implements IPacketChatOutput{
    private ClientChat client;

    public ClientChatOutput(ClientChat client){
        this.client=client;
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        byte command=packet.getCommand();

        if (command==PacketChat.FILE_INIT || command==PacketChat.FILE_DATA || command==PacketChat.FILE_OVER){
            client.getFileInterface().putPacketChat(packet);
        }else{
            client.getMessageInterface().putPacketChat(packet);
        }
    }
}
