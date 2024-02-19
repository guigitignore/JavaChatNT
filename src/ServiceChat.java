import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;


public class ServiceChat extends SocketWorker{
    private User user=null;
    private PacketChatSanitizer sanitizer;
    private ServerChat server;
    private PacketChatInterface packetInterface;

    public ServiceChat(Socket socket,ServerChat server) {
        super(socket,server);
    }

    public PacketChatInterface getPacketInterface(){
        return packetInterface;
    }

    public User getUser(){
        return user;
    }

    public ServerChat getServer(){
        return server;
    }

    private void mainloop() throws IOException{
        PacketChat packet;

        while (true){
            packet=packetInterface.getPacket();
            try{
                sanitizer.loginSanitize(packet);
            }catch(PacketChatException e){
                Logger.w("Packet dropped: "+packet + " -> " + e.getMessage());
                continue;
            }
            
            ServerChatManager.getInstance().putPacket(packet);
        }
    }

    private void loginLoop() throws IOException{
        PacketChat packet;
        IChallenge challenge=null;
        String username=null;
        User selectedUser;

        while (this.user==null){
            packet=packetInterface.getPacket();
            try{
                sanitizer.logoutSanitize(packet);
            }catch(PacketChatException e){
                Logger.w("Packet dropped: "+packet + " -> " + e.getMessage());
                continue;
            }
            switch(packet.getCommand()){
                case PacketChat.AUTH:
                    username=new String(packet.getField(0));
                    selectedUser=ServerChatManager.getInstance().getDataBase().getUser(username);
    
                    if (selectedUser==null){
                        challenge=new RegisterChallenge(username);
                    }else{
                        challenge=selectedUser.getChallenge();
                    }
    
                    packetInterface.sendChallenge(challenge.get());
                    break;
    
                case PacketChat.CHALLENGE:
                    byte[] response=packet.getField(0);
    
                    if (challenge!=null && challenge.submit(response)){        
                        if (ServerChatManager.getInstance().isConnected(username)){
                            packetInterface.sendAuthFailure("logged in another location");
                            
                        }else{
                            selectedUser=ServerChatManager.getInstance().getDataBase().getUser(username);
                            if (Arrays.asList(getServer().getTags()).contains(selectedUser.getTag())){
                                this.user=selectedUser;
                                packetInterface.sendAuthSuccess();
                                ServerChatManager.getInstance().register(this);
                            }else{
                                packetInterface.sendAuthFailure("Unauthorized connection");
                            }
                        }
                    }else{
                        packetInterface.sendAuthFailure("challenge failed");
                    }
                    //clear challenge so it cannot be used again
                    challenge=null;
                    username=null;

                    break;
            }
        }
    }

    private void welcome() throws IOException{
        switch(user.getTag()){
            case User.ADMIN_TAG:
                packetInterface.sendMessage("Welcome to admin prompt (type /help to see available commands):");
                break;
            case User.USER_TAG:
                packetInterface.sendMessage("Welcome to the server!");
                break;
        }
        packetInterface.sendPacket(ServerChatManager.getInstance().getListUsersPacket());

    }

    public void run(){
        this.server=(ServerChat)getArgs()[0];

        try{
            packetInterface=new PacketChatInterface(getSocket());
            sanitizer=new PacketChatSanitizer(this);
            loginLoop();
            welcome();
            mainloop();

        }catch(IOException e){}

        ServerChatManager.getInstance().remove(this);
        WorkerManager.getInstance().remove(this);
    }

    public String getDescription() {
        return String.format("service chat in %s", getSocket().getRemoteSocketAddress().toString());
    }
}
