import java.io.File;
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
        return String.format("ClientChatFileInput - file=%s",filename==null?"<error>":filename);
    }

    public void setup() throws Exception {
        client=(ClientChat)getArgs()[0];
        PacketChat packet=(PacketChat)getArgs()[1];
        nounce=packet.getParam();
        sender=new String(packet.getField(0));
        byte[] filenameBytes=packet.getField(1);

        try{
            filename=new String(DESEncoder.getInstance().decode(filenameBytes));
        }catch(Exception e){
            Logger.w("Cannot decrypt filename");
            filename=null;
        }
        
        output=new PacketChatOutput(client.getMessageInterface(),ClientChat.CLIENT_NAME);
        server=new PacketChatOutput(client,client.getUser().getName());
    }

    private boolean sendClientRequest() throws InterruptedException{
        return client.getRequestManager().sendConfirmationRequest(String.format("user \"%s\" want to send you a file named \"%s\".", sender,filename));
    }

    public void init() throws Exception {
        if (filename!=null && !filename.contains("/") && !filename.contains("..")
        && !client.getIncomingFiles().isNounceDefined(nounce)
        && sendClientRequest()
        && client.getIncomingFiles().registerNounce(nounce,sender)){
            try{
                File file=new File("downloads/"+filename);
                file.getParentFile().mkdirs(); 
                fileOutputStream=new FileOutputStream(file,false);
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
        byte[] data;
        PacketChat res=client.getBucket().waitPacketByType(PacketChat.FILE_DATA,PacketChat.FILE_OVER);

        if (res.getCommand()==PacketChat.FILE_OVER){
            try{
                fileOutputStream.close();
                server.sendFileOverSuccess(nounce);
                output.sendFormattedMessage("File \"%s\" sucessfully received!",filename);
                throw new InterruptedException();
            }catch(IOException e){
                output.sendFormattedMessage("Cannot close filename \"%s\" !",filename);
                server.sendFileOverFailure(nounce);
                throw e;
            }
        }

        try{
            if (res.getFieldsNumber()!=3){
                Logger.w("Malformed packet packet file data");
                throw new IOException();
            }
            data=res.getField(1);

            if (res.getFlag()==PacketChat.ENCRYPTION_FLAG){
                try{
                    data=DESEncoder.getInstance().decode(data);
                }catch(Exception e){
                    Logger.w("Cannot decode data of file \"%s\"",filename);
                    throw new IOException();
                }
            }
            fileOutputStream.write(data);
            server.sendFileDataSuccess(nounce);
        }catch(IOException e){
            server.sendFileDataFailure(nounce);
        }
    }
    
    public void cleanup() throws Exception {}
}
