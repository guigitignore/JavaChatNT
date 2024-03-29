package server;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import packetchat.IPacketChatOutput;
import packetchat.PacketChat;
import packetchat.PacketChatException;
import user.IChallenge;
import user.User;
import util.ClientType;
import util.Logger;
import util.UserStructEncoder;

public class ServiceChatInput implements IPacketChatOutput{
    private ServiceChat client;
    private User user=null;
    private String loginUsername=null;
    private IChallenge challenge=null;
    

    public ServiceChatInput(ServiceChat client){
        this.client=client;
    }

    public User getUser(){
        return user;
    }

    private void handleAuthPacket(PacketChat packet) throws PacketChatException{
        loginUsername=new String(packet.getField(0));
        User selectedUser=ServerChatManager.getInstance().getDataBase().getUser(loginUsername);

        if (selectedUser==null){
            if (packet.getFieldsNumber()==2){
                challenge=new RSARegisterChallenge(loginUsername,packet.getField(1),client.getServer().getTags()[0]);
            }else{
                challenge=new PasswordRegisterChallenge(loginUsername,client.getServer().getTags()[0]);
            }
        }else{
            challenge=selectedUser.getChallenge();
        }
        client.getOutput().sendChallenge(challenge.get());
    }

    private void handleChallengePacket(PacketChat packet) throws PacketChatException{
        User selectedUser;
        byte[] response=packet.getField(0);

        if (challenge!=null && challenge.submit(response)){        
            if (ServerChatManager.getInstance().isConnected(loginUsername)){
                client.getOutput().sendAuthFailure("logged in another location");
            }else{
                selectedUser=ServerChatManager.getInstance().getDataBase().getUser(loginUsername);
                if (Arrays.asList(client.getServer().getTags()).contains(selectedUser.getTag())){
                    //set connected user
                    this.user=selectedUser;
                    client.getOutput().sendAuthSuccess();
                    ServerChatManager.getInstance().register(client);
                    //send list of connected users
                    if (client.getClientType()==ClientType.TELNET_CLIENT){
                        //send server command
                        client.getInput().sendMessage( "/listusers");
                    }
                }else{
                    client.getOutput().sendAuthFailure("Unauthorized connection");
                }
            }
        }else{
            client.getOutput().sendAuthFailure("challenge failed");
        }
        //clear challenge so it cannot be used again
        challenge=null;
    }

    private void handleMessagePacket(PacketChat packet) throws PacketChatException{
        String senderName=new String(packet.getField(0));
        String message;

        switch (client.getUser().getTag()){
            case User.ADMIN_TAG:
                senderName=String.format("{%s}", senderName);
                break;
            case User.USER_TAG:
                senderName=String.format("<%s>", senderName);
                break;
            default:
                senderName="%unknown%";
                break;
        }
        packet.replaceField(0, senderName.getBytes());

        if (packet.getFlag()!=PacketChat.ENCRYPTION_FLAG && (message=new String(packet.getField(1))).startsWith("/")){
            StringTokenizer tokens=new StringTokenizer(message," ");
            String command=tokens.nextToken().substring(1).toLowerCase();
            String args=tokens.hasMoreTokens()?tokens.nextToken("").trim():"";
            
            switch (client.getUser().getTag()){
                case User.ADMIN_TAG:
                    new AdminCommand(client, command, args);
                    break;
                case User.USER_TAG:
                    new UserCommand(client, command, args);
                    break;
                default:
                    client.getOutput().sendMessage("You are not allowed to execute server commands");
                    break;
            }

        }else{
            int fieldsNumber=packet.getFieldsNumber();

            if (fieldsNumber==2){
                ServerChatManager.getInstance().getClients().getOutput().sendPacket(packet);
            }else{
                ServerChatManager.getInstance().getClientsByName(IntStream.range(2, fieldsNumber).mapToObj(index -> {
                    return new String(packet.getField(index));
                }).collect(Collectors.toList())).getOutput().sendPacket(packet);
            }
        }
    }

    private void handleListUserPacket(PacketChat packet) throws PacketChatException{
        byte[] field;

        if (packet.isFlagSet(PacketChat.LEGACY_USERLIST_FLAG)){
            for (String user:ServerChatManager.getInstance().getUsers()){
                packet.addField(user.getBytes());
            }
        }else{
            for (ServiceChat connectedClient:ServerChatManager.getInstance().getClients()){
                field=UserStructEncoder.getInstance().encode(connectedClient.getUser().getName(),connectedClient.getClientType());
                packet.addField(field);
            }
        }
        
        client.getOutput().sendPacket(packet);
    }

    private void handleFileInitPacket(PacketChat packet) throws PacketChatException{
        String destName;
        ServiceChat dest;
        byte nounce=packet.getParam();

        if (packet.getFieldsNumber()>0){
            destName=new String(packet.getField(3));
            dest=ServerChatManager.getInstance().getClient(destName);

            if (dest==null){
                client.getOutput().sendFileInitFailure(nounce);
                throw new PacketChatException("cannot find target user: %s",destName);
            }else if (client.getClientType()==ClientType.TELNET_CLIENT){
                client.getOutput().sendFileInitFailure(nounce);
                throw new PacketChatException("cannot send file to a telnet client");
            }else{
                if (client.getOutgoingFiles().registerNounce(nounce,destName)==false){
                    client.getOutput().sendFileInitFailure(nounce);
                    throw new PacketChatException("cannot register nounce");
                }
                dest.getOutput().sendPacket(packet);
            }
        }else{
            client.getIncomingFiles().allowNounce(nounce);
            handleFileAckPacket(packet);
        }
    }

    private void handleFilePacket(PacketChat packet,String destName) throws PacketChatException{
        if (client.getOutgoingFiles().isNounceAllowed(packet.getParam(), destName)){
            ServiceChat dest=ServerChatManager.getInstance().getClient(destName);

            if (dest==null){
                //remove nounce if client is disconnected
                client.getOutgoingFiles().removeNounce(packet.getParam());
                throw new PacketChatException("cannot find target user: %s",destName);
            }
            dest.getOutput().sendPacket(packet);
        }else{
            throw new PacketChatException("transaction not allowed");
        }
    }

    private void handleFileDataPacket(PacketChat packet) throws PacketChatException{
        handleFilePacket(packet,new String(packet.getField(2)));
    }

    private void handleFileOverPacket(PacketChat packet) throws PacketChatException{
        byte nounce=packet.getParam();

        if (packet.getFieldsNumber()>0){
            handleFilePacket(packet,new String(packet.getField(1)));
        }else{
            handleFileAckPacket(packet);
            client.getIncomingFiles().removeNounce(nounce);
        }
    }

    private void handleFileAckPacket(PacketChat packet) throws PacketChatException{
        String destName;
        ServiceChat dest;
        byte nounce=packet.getParam();

        if (client.getIncomingFiles().isNounceAllowed(nounce)){
            destName=client.getIncomingFiles().getUsernameFromNounce(nounce);
            dest=ServerChatManager.getInstance().getClient(destName);

            if (dest==null){
                client.getIncomingFiles().removeNounce(packet.getParam());
                throw new PacketChatException("cannot find target user: %s",destName);
            }
            dest.getOutput().sendPacket(packet);
        }else{
            throw new PacketChatException("unauthorized nounce");
        }
    }


    public void putPacketChat(PacketChat packet) throws PacketChatException {
        switch(packet.getCommand()){
            case PacketChat.AUTH:
                handleAuthPacket(packet);
                break;
            case PacketChat.CHALLENGE:
                handleChallengePacket(packet);
                break;
            case PacketChat.SEND_MSG:
                handleMessagePacket(packet);
                break;
            case PacketChat.LIST_USERS:
                handleListUserPacket(packet);
                break;
            case PacketChat.FILE_INIT:
                handleFileInitPacket(packet);
                break;
            case PacketChat.FILE_DATA:
                handleFileDataPacket(packet);
                break;
            case PacketChat.FILE_ACK:
                handleFileAckPacket(packet);
                break;
            case PacketChat.FILE_OVER:
                handleFileOverPacket(packet);
                break;
            default:
                Logger.w("Cannot handle this packet: %s",packet);
                break;
        }
    }
}
