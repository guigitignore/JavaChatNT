package client;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.atomic.AtomicBoolean;

import packetchat.IPacketChatOutput;
import packetchat.PacketChat;
import packetchat.PacketChatException;
import packetchat.PacketChatFactory;
import packetchat.PacketChatOutput;
import user.IUserConnection;
import user.PasswordUser;
import user.RSAUser;
import user.User;
import util.ClientType;
import util.Logger;
import util.RSAEncoder;
import util.UserStructEncoder;
import worker.LoopWorker;
import worker.WorkerManager;

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
        WorkerManager.getInstance().remove(this);
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
        String username=new String(packet.getField(0));

        client.getCardInterface().clearUser();
        try{
            client.getCardInterface().select(username);
            byte[] pubKey=RSAEncoder.getInstance().encode(client.getCardInterface().getPublicKey());
            packet.addField(pubKey);
            user=new RSAUser(username, pubKey);
        }catch(Exception e){
            Logger.w("cannot select user \"%s\": %s. Falling back on password authentification",username,e.getMessage());
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
            String args=tokens.hasMoreTokens()?tokens.nextToken("").trim():"";

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
                    packet.replaceField(1,client.getCardInterface().encryptDES(packet.getField(1)));
                    packet.setFlag(PacketChat.ENCRYPTION_FLAG);
                }catch(Exception e){
                    Logger.w("Cannot encrypt message data: %s",e.getMessage());
                }
            }
        }
        return packet;
    }


    private Collection<SimpleEntry<String,ClientType>> getConnectedUsers() throws PacketChatException{
        putPacketChat(PacketChatFactory.createListUserPacket());
        PacketChat res=client.getBucket().waitPacketByType(PacketChat.LIST_USERS);

        byte[][] fields=res.getFields();
        Collection<SimpleEntry<String,ClientType>> result=new ArrayList<SimpleEntry<String,ClientType>>();
        
        if (res.isFlagSet(PacketChat.LEGACY_USERLIST_FLAG)){
            for (byte[] field:fields){
                result.add(new SimpleEntry<>(new String(field),ClientType.TELNET_CLIENT));
            }
        }else{
            for (byte[] field:fields){
                try{
                    result.add(UserStructEncoder.getInstance().decode(field));
                }catch(NoSuchFieldException e){
                    Logger.w("Cannot read user struct: %s", Arrays.toString(field));
                }
            }
        }
        return result;
    }

    private void setEncryptionMode(String arg) throws PacketChatException{
        switch(arg.trim().toLowerCase()){
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
        StringBuilder builder=new StringBuilder();

        builder.append("List of connected users:\n");
        for (SimpleEntry<String,ClientType> user:getConnectedUsers()){
            builder.append(String.format("- name=\"%s\" - client=%s\n",user.getKey(),user.getValue().name()));
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
        int fileSize;

        if (tokens.countTokens()<2){
            output.sendMessage("this command expects at least 2 arguments");
        }else{
            dest=tokens.nextToken();
            do{
                filename=tokens.nextToken();
                fileSize=checkFile(filename);
                if (fileSize>0){
                    new ClientChatFileOutput(client, filename,fileSize, dest);
                }   
            }while(tokens.hasMoreTokens());
        }
    }

    private void sendFileToAll(String args) throws PacketChatException{
        StringTokenizer tokens=new StringTokenizer(args," ");

        do{
            String filename=tokens.nextToken();
            int fileSize=checkFile(filename);
            if (fileSize>0){
                for (SimpleEntry<String,ClientType> user:getConnectedUsers()){
                    if (!user.getKey().equals(client.getUser().getName())){
                        new ClientChatFileOutput(client, filename,fileSize, user.getKey());
                    }
                }
            }
        }while(tokens.hasMoreTokens());
    }

    //return file size or -1 in case of error
    private int checkFile(String filename) throws PacketChatException{
        int result=-1;

        File file=new File(filename);
        if (file.exists()){
            if (!file.isFile()){
                output.sendFormattedMessage("File \"%s\" is not a file", filename);
            }else{
                result=(int)file.length();
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
