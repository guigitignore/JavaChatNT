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

        output=new PacketChatOutput(client.getMessageInterface());
        server=new PacketChatOutput(client);
    }

    public void init() throws Exception {
        server.sendFileInitRequest((byte)0, client.getUser().getName(), filename, dest);
    }

    public void loop() throws Exception {
        
    }


    public void end() throws Exception {
        
    }

}
