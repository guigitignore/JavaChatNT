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

    private PacketChat handleFileInitPacket(PacketChat packet){
        String sender;
        byte nounce=packet.getParam();

        if (packet.getFieldsNumber()>0){
            sender=new String(packet.getField(0));
            if (!client.getIncomingFiles().registerNounce(nounce,sender)){
                Logger.w("nounce %d cannot be registered",nounce);
                packet=null;
            }
        }else{
            if (!client.getOutgoingFiles().allowNounce(nounce)){
                Logger.w("nounce %d cannot be allowed",nounce);
                packet=null;
            }
        }
        return packet;
    }

    private PacketChat handleFileOverPacket(PacketChat packet){
        byte nounce=packet.getParam();

        if (packet.getFieldsNumber()==0){
            client.getOutgoingFiles().removeNounce(nounce);
        }

        return packet;
    }

    private PacketChat handleMessagePacket(PacketChat packet){
        if (packet.getFlag()==PacketChat.ENCRYPTION_FLAG && client.getClientType()==ClientType.TELNET_CLIENT){
            Logger.i("cannot send encrypted message to telnet client");
            packet=null;
        }
        return packet;
    }
    
    private void handlePacket(PacketChat packet) throws PacketChatException{
        PacketChat packetCopy=packet;

        switch (packet.getCommand()){
            case PacketChat.FILE_INIT:
                packet=handleFileInitPacket(packet);
                break;
            case PacketChat.FILE_OVER:
                packet=handleFileOverPacket(packet);
                break;
            case PacketChat.SEND_MSG:
                packet=handleMessagePacket(packet);
                break;
        }

        //send packet 
        if (packet==null){
            if (client.getUser()==null){
                Logger.i("drop packet in output for %s: %s",client.getDescription(),packetCopy.toString());
            }else{
                Logger.i("drop packet in output for user \"%s\": %s",client.getUser().getName(),packetCopy.toString());
            }
        }else{
            Logger.i("send packet: %s",packet);
            client.putPacketChat(packet);
        }
    }

    public void setup() throws Exception {
        this.queue=new PacketChatQueue(CAPACITY);
        this.client=(ServiceChat)getArgs()[0];
    }

    public void loop() throws Exception {
        PacketChat packet=queue.getPacketChat();
        handlePacket(packet);
    }

    public void init() throws Exception {}

    public void cleanup() throws Exception {}
    
}
