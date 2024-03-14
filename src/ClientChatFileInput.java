import java.io.FileOutputStream;
import java.io.IOException;

public class ClientChatFileInput extends LoopWorker {
    private String filename;
    private String sender;
    private ClientChat client;
    private byte nounce;
    private PacketChatOutput output;
    private PacketChatOutput server;
    private FileOutputStream fileOutputStream;

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

        output=new PacketChatOutput(client.getMessageInterface(),ClientChat.CLIENT_NAME);
        server=new PacketChatOutput(client,client.getUser().getName());
    }

    private boolean sendClientRequest() throws InterruptedException{
        return client.getRequestManager().sendConfirmationRequest(String.format("%s want to send you a file named %s.", sender,filename));
    }

    public void init() throws Exception {
        if (!client.getIncomingFiles().isNounceDefined(nounce)
        && sendClientRequest()
        && client.getIncomingFiles().registerNounce(nounce,sender)){
            try{
                fileOutputStream=new FileOutputStream(String.format("$%s",filename));
                server.sendFileInitSucess(nounce);
            }catch(IOException e){
                server.sendFileInitFailure(nounce);
                throw e;
            }
        }else{
            server.sendFileInitFailure(nounce);
            //stop process
            throw new InterruptedException();
        }
    }

    public void loop() throws Exception {
        PacketChat res=client.getBucket().waitPacketByType(PacketChat.FILE_DATA,PacketChat.FILE_OVER);
        if (res.getCommand()==PacketChat.FILE_OVER){
            try{
                fileOutputStream.close();
                server.sendFileOverSuccess(nounce);
                output.sendFormattedMessage("File %s sucessfully received!",filename);
                throw new InterruptedException();
            }catch(IOException e){
                output.sendFormattedMessage("Cannot close filename %s !",filename);
                server.sendFileOverFailure(nounce);
                throw e;
            }
        }

        try{
            fileOutputStream.write(res.getField(1));
            server.sendFileDataSuccess(nounce);
        }catch(IOException e){
            server.sendFileDataFailure(nounce);
        }
    }
    
    public void cleanup() throws Exception {}
}
