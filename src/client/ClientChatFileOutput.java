package client;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.bouncycastle.util.Arrays;

import packetchat.PacketChat;
import packetchat.PacketChatOutput;
import util.DESEncoder;
import util.Logger;
import worker.LoopWorker;

public class ClientChatFileOutput extends LoopWorker{
    private String filename;
    private int fileSize;
    private String dest;
    private ClientChat client;
    private byte nounce;
    private PacketChatOutput output;
    private PacketChatOutput server;
    private FileInputStream fileInputStream;
    private byte[] chunk;

    public final static int CHUNK_SIZE=512;

    public ClientChatFileOutput(ClientChat client,String filename,int fileSize,String dest){
        super(client,filename,fileSize,dest);
    }

    public String getDescription() {
        return String.format("ClientChatFileOutput - file=%s",filename );
    }

    public void setup() throws Exception {
        client=(ClientChat)getArgs()[0];
        filename=(String)getArgs()[1];
        fileSize=(int)getArgs()[2];
        dest=(String)getArgs()[3];
        

        try{
            fileInputStream=new FileInputStream(filename);
        }catch(IOException e){
            output.sendFormattedMessage("Cannot open file \"%s\"",filename);
            throw e;
        }
        filename=new File(filename).getName();

        Byte testNounce=client.getOutgoingFiles().generateNounce(dest);
        if (testNounce==null){
            output.sendFormattedMessage("Cannot get a valid nounce for file \"%s\"",filename);
            throw new InterruptedException();
        }
        nounce=testNounce;        

        output=new PacketChatOutput(client.getMessageInterface(),ClientChat.CLIENT_NAME);
        server=new PacketChatOutput(client,client.getUser().getName());
    }

    public void init() throws Exception {
        //get base name from path
        byte[] encryptedFilename=null;

        if (client.getOutput().getEncryptionStatus()){
            try{
                encryptedFilename=DESEncoder.getInstance().encode(filename.getBytes());
            }catch(Exception e){
                Logger.w("Cannot encrypt filename");
            }
        }

        if (encryptedFilename==null){
            server.sendFileInitRequest(nounce, filename,fileSize, dest);
        }else{
            server.sendFileEncryptedInitRequest(nounce, encryptedFilename,fileSize, dest);
        }
        
        PacketChat res=client.getBucket().waitPacketAckByNounce(nounce);

        if (res.getStatus()==PacketChat.STATUS_ERROR){
            output.sendFormattedMessage("user \"%s\" reject your file \"%s\"",dest,filename);
            throw new InterruptedException();
        }

        chunk=new byte[CHUNK_SIZE];
        
    }

    public void loop() throws Exception {
        PacketChat res;
        byte[] data;
        byte[] encryptedData=null;

        try{
            int n=fileInputStream.read(chunk);
            if (n<=0){
                output.sendFormattedMessage("Finished to send file \"%s\" to user \"%s\"",filename,dest);
                throw new IOException("EOF");
            }
            data=Arrays.copyOfRange(chunk, 0, n);

            if (client.getOutput().getEncryptionStatus()){
                try{
                    encryptedData=DESEncoder.getInstance().encode(data);
                }catch(Exception e){
                    Logger.w("Cannot encrypt data of file \"%d\"",filename);
                }
            }
            
            if (encryptedData==null){
                server.sendFileDataRequest(nounce, data, dest);
            }else{
                server.sendFileEncryptedDataRequest(nounce, encryptedData, dest);
            }
            
            res=client.getBucket().waitPacketAckByNounce(nounce);

            if (res.getStatus()==PacketChat.STATUS_ERROR) throw new IOException("Interrupt");
        }catch(IOException e){
            try{
                fileInputStream.close();
            }catch(IOException e2){
                output.sendFormattedMessage("Cannot close file \"%s\"",filename);
            }
            server.sendFileOverRequest(nounce, dest);
            client.getBucket().waitPacketAckByNounce(nounce);
            throw new InterruptedException();
        }
    }

    public void cleanup() throws Exception {}

}
