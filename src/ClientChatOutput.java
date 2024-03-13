import java.io.File;
import java.util.StringTokenizer;

public class ClientChatOutput extends LoopWorker implements IPacketChatOutput,IUserConnection{
    private ClientChat client;
    private User user=null;
    private PacketChatOutput output;

    public ClientChatOutput(ClientChat client){
        super(client);
    }

    public String getDescription() {
        return "ClientChatOutput";
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        packet=handleOutgoingPacket(packet);
        if (packet!=null){
            client.putPacketChat(packet);
        }
    }

    public void setup() throws Exception {
        client=(ClientChat)getArgs()[0];
        output=new PacketChatOutput(client.getMessageInterface());
    }

    
    public void init() throws Exception {
        output=new PacketChatOutput(client.getMessageInterface());
    }

    public void loop() throws Exception {
        try{
            PacketChat packet;
            packet=client.getMessageInterface().getPacketChat();
            putPacketChat(packet);
        }catch(Exception e){
            e.printStackTrace();
            throw e;
        }
    }

    public void end() throws Exception {
        WorkerManager.getInstance().cancelAll();
    }

    public User getUser() {
        return user;
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

    public void handleOutgoingAuthPacket(PacketChat packet) throws PacketChatException{
        RSAKeyPair userKeyPair;
        String username=new String(packet.getField(0));

        try{
            userKeyPair=RSAKeyPair.importKeyPair(username);
            user=new RSAUser(username, RSAEncoder.getInstance().encode(userKeyPair.getPublic()));
            packet.addField(user.getKey());
            //set private key
            ((ClientChatInput)client.getInput().getInterface()).setPrivateKey(userKeyPair.getPrivate());
        }catch(Exception e){
            Logger.w("cannot load user RSA key: %s. Falling back on password authentification",e.getMessage());
            user=new PasswordUser(username,"");
        }
    }

    private PacketChat handleOutgoingMessagePacket(PacketChat packet) throws PacketChatException{
        String message=new String(packet.getField(1));
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
                    packet=null;
                    break;
                case "allow":
                    handleRequestReponse(args,true);
                    packet=null;
                    break;
                case "deny":
                    handleRequestReponse(args,false);
                    packet=null;
                    break;
                case "listusers":
                    listConnectedUsers();
                    packet=null;
                    break;
                case "help":
                    helpCommand();
                    break;
            }
        }
        return packet;
    }


    private String[] getConnectedUsers() throws PacketChatException{
        putPacketChat(PacketChatFactory.createListUserPacket());
        PacketChat res=client.getBucket().waitPacketByType(PacketChat.LIST_USERS);

        byte[][] fields=res.getFields();
        String[] result=new String[fields.length];
        for (int i=0;i<fields.length;i++) result[i]=new String(fields[i]);
        return result;
    }

    private void listConnectedUsers() throws PacketChatException{
        String[] connectedUsers=getConnectedUsers();
        StringBuilder builder=new StringBuilder();

        builder.append("List of connected users:\n");
        for (String username:connectedUsers){
            builder.append(String.format("- %s \n",username));
        }
        output.sendMessage(ClientChat.CLIENT_NAME,builder.toString());
    }

    private void helpCommand() throws PacketChatException{
        StringBuilder builder;
        builder=new StringBuilder();
        builder.append("list of heavy client commands:\n");
        builder.append("/sendFileTo - send message to a client\n");
        builder.append("/sendFileToAll - send message to all\n");
        builder.append("/listusers - list connected users\n");
        builder.append("/allow - accept a request\n");
        builder.append("/deny - reject a request\n");
        builder.append("/help - print help menu\n");

        output.sendMessage(ClientChat.CLIENT_NAME,builder.toString());
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

    private void sendFileTo(String args) throws PacketChatException{
        StringTokenizer tokens=new StringTokenizer(args," ");
        String filename;
        String dest;

        if (tokens.countTokens()<2){
            output.sendMessage(ClientChat.CLIENT_NAME,"this command expects 2 arguments");
        }else{
            filename=tokens.nextToken();
            dest=tokens.nextToken();

            if (checkFile(filename)){
                sendFile(filename,dest);
            }
        }
    }

    private boolean checkFile(String filename) throws PacketChatException{
        boolean result=false;

        File file=new File(filename);
        if (file.exists()){
            if (!file.isFile()){
                output.sendMessage(ClientChat.CLIENT_NAME,"File \"%s\" is not a file", filename);
            }else{
                result=true;
            }
        }else{
            output.sendMessage(ClientChat.CLIENT_NAME,"File \"%s\" does not exists", filename);
        }
        return result;
    }

    private void handleRequestReponse(String arg,boolean res) throws PacketChatException{
        int confirmationId;

        try{
            confirmationId=Integer.parseInt(arg);
            if (client.getRequestManager().sendConfirmationResponse(confirmationId, res)==false){
                output.sendMessage(ClientChat.CLIENT_NAME,"This request number does not exist");
            }
        }catch(NumberFormatException e){
            output.sendMessage(ClientChat.CLIENT_NAME,"Invalid request number");
        }
    }

    

    
    
}
