public class ClientChatFileInput extends LoopWorker {
    private String filename;
    private String sender;
    private ClientChat client;
    private byte nounce;
    private PacketChatOutput output;
    private PacketChatOutput server;

    public ClientChatFileInput(ClientChat client,PacketChat fileInit){
        super(client,fileInit);
    }

    public String getDescription() {
        return String.format("ClientChatFileInput - file=%s",filename );
    }

    public void setup() throws Exception {
        client=(ClientChat)getArgs()[0];
        PacketChat fileInit=(PacketChat)getArgs()[1];
        nounce=fileInit.getParam();
        sender=new String(fileInit.getField(0));
        filename=new String(fileInit.getField(1));

        output=new PacketChatOutput(client.getMessageInterface());
        server=new PacketChatOutput(client);
    }

    private boolean sendClientRequest() throws InterruptedException{
        return client.getRequestManager().sendConfirmationRequest(String.format("%s want to send you a file named %s.", sender,filename));
    }

    public void init() throws Exception {
        if (sendClientRequest()){

        }else{

        }
    }

    public void loop() throws Exception {
        
    }


    public void end() throws Exception {
        
    }    
}
