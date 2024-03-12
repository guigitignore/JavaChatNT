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

    private void handleFileInitPacket(PacketChat packet) throws PacketChatException{
        String sender;
        byte nounce=packet.getParam();

        if (packet.getFieldsNumber()>0){
            sender=new String(packet.getField(0));
            if (!client.getIncomingFiles().registerNounce(nounce,sender)){
                throw new PacketChatException("nounce cannot be registered");
            }
        }else{
            if (!client.getOutgoingFiles().allowNounce(nounce)){
                throw new PacketChatException("nounce cannot be allowed");
            }
        }
    }

    private void handleFileOverPacket(PacketChat packet) throws PacketChatException{
        byte nounce=packet.getParam();

        if (packet.getFieldsNumber()==0){
            client.getOutgoingFiles().removeNounce(nounce);
        }
    }

    
    private void handlePacket(PacketChat packet) throws PacketChatException{
        switch (packet.getCommand()){
            case PacketChat.FILE_INIT:
                handleFileInitPacket(packet);
                break;
            case PacketChat.FILE_OVER:
                handleFileOverPacket(packet);
                break;
        }

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
