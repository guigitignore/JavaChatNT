import java.io.File;
import java.util.StringTokenizer;

import javax.crypto.Cipher;


public class ClientChatMessage extends LoopWorker implements IPacketChatOutput,IUserConnection{
    public final static String SENDER="[CLIENTCHAT]";
    private RSAKeyPair userKeyPair=null;
    private User user=null;
    private boolean connected=false;

    public ClientChat client;
    PacketChatTelnetInterface messageInterface;

    public ClientChatMessage(ClientChat client){
        super(client);
    }

    public String getDescription() {
        return "ClientChatMessage";
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        packet=handleIncomingPacket(packet);
        if (packet!=null) messageInterface.putPacketChat(packet);
    }

    public void setup() throws Exception {
        client=(ClientChat)getArgs()[0];
    }

    public void init() throws Exception {
        messageInterface=new PacketChatTelnetInterface(new InterruptibleInputStream(),System.out);
    }

    public void loop() throws Exception {
        PacketChat packet=messageInterface.getPacketChat();
        packet=handleOutgoingPacket(packet);
        if (packet!=null) client.putPacketChat(packet);
    }

    public void end() throws Exception {
        WorkerManager.getInstance().cancelAll();
    }

    public User getUser() {
        User result;
        if (connected) result=user;
        else result=null;
        return result;
    }

    public void handleOutgoingAuthPacket(PacketChat packet){
        String username=new String(packet.getField(0));
        try{
            userKeyPair=RSAKeyPair.importKeyPair(username);
            user=new RSAUser(username, RSAEncoder.getInstance().encode(userKeyPair.getPublic()));
            packet.addField(user.getKey());
        }catch(Exception e){
            Logger.w("cannot load user RSA key: %s. Falling back on password authentification",e.getMessage());
            user=new PasswordUser(username,"");
        }
    }

    public void sendMessageToClient(String format,Object...args){
        try{
            messageInterface.putPacketChat(PacketChatFactory.createMessagePacket(SENDER, String.format(format, args)));
        }catch(PacketChatException e){
            Logger.w("Cannot send message to client: %s",e.getMessage());
        }
    }

    public boolean checkFile(String filename){
        boolean result=false;

        File file=new File(filename);
        if (file.exists()){
            if (!file.isFile()){
                sendMessageToClient("File \"%s\" is not a file", filename);
            }else{
                result=true;
            }
        }else{
            sendMessageToClient("File \"%s\" does not exists", filename);
        }
        return result;
    }

    public void sendFile(String filename,String dest){
        PacketChat packet;
        packet=PacketChatFactory.createFileInitPacket(getUser().getName(), filename, dest);
        try{
            client.putPacketChat(packet);
        }catch(PacketChatException e){
            Logger.w("Cannot send file init request");
        }
        
    }

    public void sendFileTo(String args){
        StringTokenizer tokens=new StringTokenizer(args," ");
        String filename;
        String dest;

        if (tokens.countTokens()<2){
            sendMessageToClient("this command expects 2 arguments");
        }else{
            filename=tokens.nextToken();
            dest=tokens.nextToken();

            if (checkFile(filename)){
                sendFile(filename,dest);
            }
        }
    }

    public void sendFileToAll(String filename){
        if (checkFile(filename)){
            /* 
            for (String username:ServerChatManager.getInstance().getUsers()){
                //not send to user itself
                if (!username.equals(getUser().getName())){
                    sendFile(filename, username);
                }
            }
            */
        }
    }

    public PacketChat handleOutgoingMessagePacket(PacketChat packet){
        String message=new String(packet.getField(1));
        StringBuilder builder;
        StringTokenizer tokens;

        //insert name to be complient
        packet.replaceField(0,user.getName().getBytes());

        if (message.startsWith("/")){
            tokens=new StringTokenizer(message," ");
            String command=tokens.nextToken().substring(1).toLowerCase();
            String args=tokens.hasMoreTokens()?tokens.nextToken("").strip():"";

            switch (command){
                
                case "sendfileto":
                    //not forward packet
                    sendFileTo(args);
                    packet=null;
                    break;
                case "sendfiletoall":
                    sendFileToAll(args);
                    packet=null;
                    break;

                case "help":
                    builder=new StringBuilder();
                    builder.append("list of heavy client commands:\n");
                    builder.append("/sendFileTo - send message to a client\n");
                    builder.append("/sendFileToAll - send message to all\n");
                    builder.append("/help - print help menu\n");

                    sendMessageToClient(builder.toString());

                    break;
            }
        }
        return packet;
    }
    
    public PacketChat handleOutgoingPacket(PacketChat packet){
        switch (packet.getCommand()){
            case PacketChat.AUTH:
                handleOutgoingAuthPacket(packet);
                break;

            case PacketChat.SEND_MSG:
                packet=handleOutgoingMessagePacket(packet);
                break;
        }

        return packet;
    }

    public PacketChat handleIncomingPacket(PacketChat packet){
        switch(packet.getCommand()){
            case PacketChat.CHALLENGE:
                if (packet.getFieldsNumber()>=1 && userKeyPair!=null){
                    try{
                        Cipher cipher=Cipher.getInstance( "RSA/NONE/NoPadding", "BC" );
                        cipher.init(Cipher.DECRYPT_MODE, userKeyPair.getPrivate());
                        client.putPacketChat(PacketChatFactory.createChallengePacket(cipher.doFinal(packet.getField(0))));
                    }catch(Exception e){
                        Logger.w("cannot solve challenge: %s",e.getMessage());
                    }
                    //drop packet
                    packet=null;
                }
                break;
            case PacketChat.AUTH:
                if (packet.getStatus()==PacketChat.STATUS_SUCCESS) connected=true;
                break;
        }
        return packet;
    }
}
