import java.io.File;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;


public class ClientChatMessage extends LoopWorker implements IPacketChatOutput,IUserConnection{
    public final static String SENDER="[CLIENTCHAT]";
    private RSAKeyPair userKeyPair=null;
    private User user=null;
    private boolean connected=false;
    private HashMap<Integer,AtomicBoolean> confirmations=new HashMap<>();
    private AtomicInteger confirmationCounter=new AtomicInteger(0);

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
        if (packet!=null){
            messageInterface.putPacketChat(packet);
        }
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

    private void sendFile(String filename,String dest){
        PacketChat packet;
        packet=PacketChatFactory.createFileInitPacket((byte)0,getUser().getName(), filename, dest);
        try{
            client.putPacketChat(packet);
        }catch(PacketChatException e){
            Logger.w("Cannot send file init request");
        }
        
    }

    private void sendFileTo(String args){
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

    public boolean sendConfirmationRequest(String message) throws InterruptedException{
        int confirmationId=confirmationCounter.incrementAndGet();
        AtomicBoolean request=new AtomicBoolean(false);

        synchronized(confirmations){
            confirmations.put(confirmationId,request);
        }
        
        StringBuilder builder=new StringBuilder();
        builder.append(message);
        builder.append(String.format("\n\"/allow %d\" to accept",confirmationId));
        builder.append(String.format("\n\"/deny %d\" to reject\n",confirmationId));

        sendMessageToClient(builder.toString());
        
        synchronized(request){
            request.wait();
        }
        return request.get();
    }

    private void sendConfirmationResponse(String arg,boolean res){
        AtomicBoolean request;
        int confirmationId;

        try{
            confirmationId=Integer.parseInt(arg);
            synchronized(confirmations){
                request=confirmations.remove(confirmationId);
            }
            if (request==null){
                sendMessageToClient("This request number does not exist");
            }else{
                request.set(res);
                synchronized(request){
                    request.notify();
                }
            }
        }catch(NumberFormatException e){
            sendMessageToClient("Invalid request number");
        }
    }

    private void sendFileToAll(String filename){
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

    private PacketChat handleOutgoingMessagePacket(PacketChat packet){
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
                case "allow":
                    sendConfirmationResponse(args,true);
                    packet=null;
                    break;
                case "deny":
                    sendConfirmationResponse(args,false);
                    packet=null;
                    break;

                case "help":
                    builder=new StringBuilder();
                    builder.append("list of heavy client commands:\n");
                    builder.append("/sendFileTo - send message to a client\n");
                    builder.append("/sendFileToAll - send message to all\n");
                    builder.append("/allow - accept a request\n");
                    builder.append("/deny - reject a request\n");
                    builder.append("/help - print help menu\n");

                    sendMessageToClient(builder.toString());

                    break;
            }
        }
        return packet;
    }
    
    private PacketChat handleOutgoingPacket(PacketChat packet) throws PacketChatException{
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

    private void listUsers() throws PacketChatException{
        PacketChat packet;
        StringBuilder builder;

        client.putPacketChat(PacketChatFactory.createListUserPacket());
        packet=client.getBucket().waitPacketByType(PacketChat.LIST_USERS);

        builder=new StringBuilder();
        builder.append("List of connected users:\n");
        for (byte[] field:packet.getFields()){
            builder.append(String.format("- %s \n",new String(field)));
        }
        sendMessageToClient(builder.toString());
    }

    private PacketChat handleIncomingPacket(PacketChat packet) throws PacketChatException{
        byte[] result=null;

        switch(packet.getCommand()){
            case PacketChat.CHALLENGE:
                if (packet.getFieldsNumber()>=1 && userKeyPair!=null){
                    try{
                        Cipher cipher=Cipher.getInstance( "RSA/NONE/NoPadding", "BC" );
                        cipher.init(Cipher.DECRYPT_MODE, userKeyPair.getPrivate());
                        result=cipher.doFinal(packet.getField(0));
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
                if (packet.getStatus()==PacketChat.STATUS_SUCCESS) connected=true;
                break;
        }
        return packet;
    }
}
