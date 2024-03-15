import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.bouncycastle.util.Arrays;

public class ClientChatFileOutput extends LoopWorker{
    private String filename;
    private String dest;
    private ClientChat client;
    private byte nounce;
    private PacketChatOutput output;
    private PacketChatOutput server;
    private FileInputStream fileInputStream;
    private byte[] chunk;

    public final static int CHUNK_SIZE=512;

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
        chunk=new byte[CHUNK_SIZE];

        output=new PacketChatOutput(client.getMessageInterface(),ClientChat.CLIENT_NAME);
        server=new PacketChatOutput(client,client.getUser().getName());
    }

    public void init() throws Exception {
        Byte testNounce=client.getOutgoingFiles().generateNounce(dest);
        String sentFilename=new File(filename).getName();
        byte[] encryptedFilename=null;

        if (testNounce==null){
            output.sendFormattedMessage("Cannot get a valid nounce for file \"%s\"",filename);
            throw new InterruptedException();
        }
        nounce=testNounce;

        if (client.getOutput().getEncryptionStatus()){
            try{
                encryptedFilename=DESEncoder.getInstance().encode(sentFilename.getBytes());
            }catch(Exception e){
                Logger.w("Cannot encrypt filename");
            }
        }

        if (encryptedFilename==null){
            server.sendFileInitRequest(nounce, sentFilename, dest);
        }else{
            server.sendFileEncryptedInitRequest(nounce, encryptedFilename, dest);
        }
        
        PacketChat res=client.getBucket().waitPacketAckByNounce(nounce);

        if (res.getStatus()==PacketChat.STATUS_ERROR){
            output.sendFormattedMessage("user \"%s\" reject your file \"%s\"",dest,filename);
            throw new InterruptedException();
        }


        try{
            fileInputStream=new FileInputStream(filename);
        }catch(IOException e){
            output.sendFormattedMessage("Cannot open file \"%s\"",filename);
            server.sendFileOverRequest(nounce, dest);
            client.getBucket().waitPacketAckByNounce(nounce);
            throw e;
        }
        
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
