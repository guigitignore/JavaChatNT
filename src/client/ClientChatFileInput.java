package client;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import packetchat.PacketChat;
import packetchat.PacketChatOutput;
import util.Logger;
import worker.LoopWorker;

public class ClientChatFileInput extends LoopWorker {
    private String filename;
    private int fileSize;
    private boolean fileSizeExeededAllowed;
    private int dataCounter;
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
        return String.format("ClientChatFileInput - file=%s",filename);
    }

    public void setup() throws Exception {
        client=(ClientChat)getArgs()[0];
        PacketChat packet=(PacketChat)getArgs()[1];
        nounce=packet.getParam();
        sender=new String(packet.getField(0));

        byte[] fileSizeBytes=packet.getField(2);
        fileSize=fileSizeBytes.length>=Integer.BYTES?ByteBuffer.wrap(packet.getField(2)).getInt():-1;

        if (client.getOutput().getEncryptionStatus()){
            try{
                filename=new String(client.getCardInterface().decryptDES(packet.getField(1)));
            }catch(Exception e){
                Logger.w("Cannot decrypt filename");
                filename=null;
            }
        }else{
            filename=new String(packet.getField(1));
        }

        //precheck filename and nounce
        if (filename==null || filename.contains("/") || filename.contains("..") || client.getIncomingFiles().isNounceDefined(nounce)){
            server.sendFileInitFailure(nounce);
            //do not start thread
            throw new InterruptedException();
        }
        
        output=new PacketChatOutput(client.getMessageInterface(),ClientChat.CLIENT_NAME);
        server=new PacketChatOutput(client,client.getUser().getName());
    }

    public void init() throws Exception {
        if (client.getRequestManager().sendConfirmationRequest("user \"%s\" want to send you a file named \"%s\" (%d bytes).", sender,filename,fileSize) 
        && client.getIncomingFiles().registerNounce(nounce,sender)){
            try{
                File file=new File(String.format("downloads/%s/%s",client.getUser().getName(),filename));
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

        dataCounter=0;
        fileSizeExeededAllowed=false;
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
                    data=client.getCardInterface().decryptDES(data);
                }catch(Exception e){
                    Logger.w("Cannot decode data of file \"%s\"",filename);
                    throw new IOException();
                }
            }

            dataCounter+=data.length;

            if (!fileSizeExeededAllowed && dataCounter>fileSize){
                if (client.getRequestManager().sendConfirmationRequest("The file size of \"%s\" has been exeeded. Would you like to continue download?")){
                    fileSizeExeededAllowed=true;
                }else{
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
