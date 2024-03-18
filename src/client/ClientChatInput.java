package client;
import java.util.concurrent.atomic.AtomicBoolean;

import packetchat.IPacketChatOutput;
import packetchat.PacketChat;
import packetchat.PacketChatException;
import packetchat.PacketChatFactory;
import shared.PacketChatSanitizer;
import util.Logger;
import worker.LoopWorker;
import worker.WorkerManager;


public class ClientChatInput extends LoopWorker implements IPacketChatOutput{
    private ClientChat client;
    private AtomicBoolean isConnected;
    private PacketChatSanitizer sanitizer;

    public ClientChatInput(ClientChat client){
        super(client);
    }

    public boolean isConnected(){
        return isConnected.get();
    }

    public String getDescription() {
        return "ClientChatInput";
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        Logger.i("got packet: %s",packet);
        try{
            sanitizer.client(packet);
        }catch(PacketChatException e){
            Logger.w("Packet dropped: %s",e.getMessage());
            packet=null;
        }

        if (packet!=null){
            packet=isConnected.get()?handleLoginPacket(packet):handleLogoutPacket(packet);
            if (packet!=null) client.getMessageInterface().putPacketChat(packet);
        }
    
    }

    public void setup() throws Exception {
        client=(ClientChat)getArgs()[0];
        sanitizer=new PacketChatSanitizer(client);
    }

    
    public void init() throws Exception {
        isConnected=new AtomicBoolean(false);
    }

    public void loop() throws Exception {
        PacketChat packet;

        if (isConnected.get()){
            packet=client.getBucket().waitPacketByType(PacketChat.SEND_MSG,PacketChat.FILE_INIT);
        }else{
            packet=client.getBucket().waitPacketByType(PacketChat.AUTH,PacketChat.CHALLENGE);
        }
        
        putPacketChat(packet);
    }

    public void cleanup() throws Exception {
        WorkerManager.getInstance().remove(this);
        WorkerManager.getInstance().cancelAll();
    }


    private PacketChat handleLoginPacket(PacketChat packet) throws PacketChatException{

        switch(packet.getCommand()){
            case PacketChat.FILE_INIT:
                if (packet.getFieldsNumber()>0) new ClientChatFileInput(client, packet);
                packet=null;
                break;
            case PacketChat.SEND_MSG:
                if (packet.getFlag()==PacketChat.ENCRYPTION_FLAG){
                    try{
                        packet.replaceField(1,client.getCardInterface().decryptDES(packet.getField(1)));       
                    }catch(Exception e){
                        Logger.w("Cannot decrypt message data: %s",e.getMessage());
                        packet=null;
                    }
                }
                break;
        }

        return packet;
    }

    private PacketChat handleLogoutPacket(PacketChat packet) throws PacketChatException{
        byte[] result=null;

        switch(packet.getCommand()){
            case PacketChat.CHALLENGE:
                if (packet.getFieldsNumber()>=1 && client.getCardInterface().getSelectedUser()!=null){
                    try{
                        result=client.getCardInterface().solveChallenge(packet.getField(0));
                    }catch(Exception e){
                        Logger.w("cannot solve challenge: %s",e.getMessage());
                    }
                    if (result!=null){
                        client.putPacketChat(PacketChatFactory.createChallengePacket(result));
                        //drop packet
                        packet=null;
                    }
                }
                break;
            case PacketChat.AUTH:
                if (packet.getStatus()==PacketChat.STATUS_SUCCESS){
                    isConnected.set(true);
                    Logger.i("Connection success for user \"%s\"",client.getUser().getName());
                    client.getOutput().putPacketChat(PacketChatFactory.createMessagePacket("","/listusers"));
                }
                break;
        }
        return packet;
    }
    
}
