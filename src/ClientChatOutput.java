import java.io.File;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientChatOutput extends LoopWorker implements IPacketChatOutput,IUserConnection{
    private ClientChat client;
    private User user=null;
    private PacketChatOutput output;
    private AtomicBoolean encryption=new AtomicBoolean(false);

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
        output=new PacketChatOutput(client.getMessageInterface(),ClientChat.CLIENT_NAME);
    }

    public boolean getEncryptionStatus(){
        return encryption.get();
    }
    
    public void init() throws Exception {}

    public void loop() throws Exception {
        PacketChat packet;
        packet=client.getMessageInterface().getPacketChat();
        putPacketChat(packet);
    }

    public void cleanup() throws Exception {
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
            client.getInput().setPrivateKey(userKeyPair.getPrivate());
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
                    sendFileTo(args);
                    packet=null;
                    break;
                case "sendfiletoall":
                    sendFileToAll(args);
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
                case "encryption":
                    setEncryptionMode(args);
                    packet=null;
                    break;
                case "help":
                    helpCommand();
                    break;
            }
        }else{
            if (getEncryptionStatus()){
                try{
                    packet.replaceField(1,DESEncoder.getInstance().encode(packet.getField(1)));
                    packet.setFlag(PacketChat.ENCRYPTION_FLAG);
                }catch(Exception e){
                    Logger.w("Cannot encrypt message data: %s",e.getMessage());
                }
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

    private void setEncryptionMode(String arg) throws PacketChatException{
        switch(arg.strip().toLowerCase()){
            case "on":
                encryption.set(true);
                output.sendMessage("activate encryption");
                break;
            case "off":
                encryption.set(false);
                output.sendMessage("deactivate encryption");
                break;
            default:
                output.sendMessage("Expected on/off as argument");
        }
    }

    private void listConnectedUsers() throws PacketChatException{
        String[] connectedUsers=getConnectedUsers();
        StringBuilder builder=new StringBuilder();

        builder.append("List of connected users:\n");
        for (String username:connectedUsers){
            builder.append(String.format("- %s \n",username));
        }
        output.sendMessage(builder.toString());
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
        builder.append("/encryption - manage DES encryption of data\n");
        builder.append("/help - print help menu\n");

        output.sendMessage(builder.toString());
    }

    private void sendFileTo(String args) throws PacketChatException{
        StringTokenizer tokens=new StringTokenizer(args," ");
        String filename;
        String dest;

        if (tokens.countTokens()<2){
            output.sendMessage("this command expects at least 2 arguments");
        }else{
            filename=tokens.nextToken();

            if (checkFile(filename)){
                do{
                    dest=tokens.nextToken();
                    new ClientChatFileOutput(client, filename, dest);
                }while(tokens.hasMoreTokens());
            }else{
                output.sendFormattedMessage("the file \"%s\" does not exists",filename);
            }
        }
    }

    private void sendFileToAll(String filename) throws PacketChatException{
        if (checkFile(filename)){
            for (String user:getConnectedUsers()){
                if (!user.equals(client.getUser().getName())){
                    new ClientChatFileOutput(client, filename, user);
                }
            }
        }else{
            output.sendFormattedMessage("the file \"%s\" does not exists",filename);
        }
    }

    private boolean checkFile(String filename) throws PacketChatException{
        boolean result=false;

        File file=new File(filename);
        if (file.exists()){
            if (!file.isFile()){
                output.sendFormattedMessage("File \"%s\" is not a file", filename);
            }else{
                result=true;
            }
        }else{
            output.sendFormattedMessage("File \"%s\" does not exists", filename);
        }
        return result;
    }

    private void handleRequestReponse(String arg,boolean res) throws PacketChatException{
        int confirmationId;

        try{
            confirmationId=Integer.parseInt(arg);
            if (client.getRequestManager().sendConfirmationResponse(confirmationId, res)==false){
                output.sendMessage("This request number does not exist");
            }
        }catch(NumberFormatException e){
            output.sendMessage("Invalid request number");
        }
    }
  
}
