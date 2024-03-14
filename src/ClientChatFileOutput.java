public class ClientChatFileOutput extends LoopWorker{
    private String filename;
    private String dest;
    private ClientChat client;
    private byte nounce;
    private PacketChatOutput output;
    private PacketChatOutput server;

    public ClientChatFileOutput(ClientChat client,String filename,String dest){
        super(client,filename,dest);
    }

    public String getDescription() {
        return String.format("ClientChatFileOutput - file=%s",filename );
    }

    public void setup() throws Exception {
        client=(ClientChat)getArgs()[0];
        filename=(String)getArgs()[1];
        dest=(String)getArgs()[2];

        output=new PacketChatOutput(client.getMessageInterface(),ClientChat.CLIENT_NAME);
        server=new PacketChatOutput(client,client.getUser().getName());
    }

    public void init() throws Exception {
        nounce=(byte)0;
        server.sendFileInitRequest(nounce, filename, dest);
        PacketChat res=client.getBucket().waitPacketAckByNounce(nounce);

        if (res.getStatus()==PacketChat.STATUS_ERROR){
            output.sendFormattedMessage("%s reject file %s",dest,filename);
        }
    }

    public void loop() throws Exception {
        Thread.sleep(10);
    }


    public void end() throws Exception {}

    public void cleanup() throws Exception {}

}
