import java.security.interfaces.RSAPrivateKey;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.Cipher;


public class ClientChatInput extends LoopWorker implements IPacketChatOutput{
    private ClientChat client;
    private AtomicBoolean isConnected;
    private RSAPrivateKey privateKey=null;

    public ClientChatInput(ClientChat client){
        super(client);
    }

    public boolean isConnected(){
        return isConnected.get();
    }

    public String getDescription() {
        return "ClientChatInput";
    }

    public void setPrivateKey(RSAPrivateKey key){
        privateKey=key;
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        Logger.i("got packet in input %s",packet==null?"null":packet.toString());
        if (!isConnected.get()){
            packet=handleLogoutPacket(packet);
        }
        
        if (packet!=null){
            client.getMessageInterface().putPacketChat(packet);
        }
    }

    public void setup() throws Exception {
        client=(ClientChat)getArgs()[0];
    }

    
    public void init() throws Exception {
        isConnected=new AtomicBoolean(false);
    }

    public void loop() throws Exception {
        try{
            PacketChat packet;

            if (isConnected.get()){
                packet=client.getBucket().waitPacketByType(PacketChat.SEND_MSG,PacketChat.FILE_INIT);
            }else{
                packet=client.getBucket().waitPacketByType(PacketChat.AUTH,PacketChat.CHALLENGE);
            }
            
            putPacketChat(packet);
        }catch(Exception e){
            e.printStackTrace();
        }
        
    }

    public void end() throws Exception {
        WorkerManager.getInstance().cancelAll();
    }

    private PacketChat handleLogoutPacket(PacketChat packet) throws PacketChatException{
        byte[] result=null;

        switch(packet.getCommand()){
            case PacketChat.CHALLENGE:
                if (packet.getFieldsNumber()>=1 && privateKey!=null){
                    try{
                        Cipher cipher=Cipher.getInstance( "RSA/NONE/NoPadding", "BC" );
                        cipher.init(Cipher.DECRYPT_MODE, privateKey);
                        result=cipher.doFinal(packet.getField(0));
                    }catch(Exception e){
                        Logger.w("cannot solve challenge: %s",e.getMessage());
                    }
                    if (result!=null){
                        client.putPacketChat(PacketChatFactory.createChallengePacket(result));
                        //drop packet
                        packet=null;
                    }
                    privateKey=null;
                }
                break;
            case PacketChat.AUTH:
                if (packet.getStatus()==PacketChat.STATUS_SUCCESS) isConnected.set(true);
                break;
        }
        return packet;
    }
    
}