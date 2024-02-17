import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;


public class ServiceChat extends SocketWorker{
    private InputStream input;
    private OutputStream output;
    private User user=null;
    private PacketChatSanitizer sanitizer;
    private ServerChat server;

    public ServiceChat(Socket socket,ServerChat server) {
        super(socket,server);
    }

    private void initStreams() throws IOException{
        input=getSocket().getInputStream();
        output=getSocket().getOutputStream();
    }

    private PacketChat getPacket() throws IOException{
        PacketChat packet;
        try{
            packet=new PacketChat(input);
        }catch(PacketChatException e){
            throw new IOException(e.getMessage());
        }
        return packet;
    }

    public User getUser(){
        return user;
    }

    public boolean isConnected(){
        return user!=null;
    }

    public ServerChat getServer(){
        return server;
    }

    private void mainloop() throws IOException{
        PacketChat packet;
        BlockingQueue<PacketChat> queue=ServerChatManager.getInstance().getPacketQueue();

        while (true){
            packet=getPacket();
            try{
                sanitizer.loginSanitize(packet);
            }catch(PacketChatException e){
                Logger.w("Packet dropped: "+packet + " -> " + e.getMessage());
                continue;
            }
            try{
                queue.put(packet);
            }catch(InterruptedException e){
                Logger.e("Packet cannot be queued:"+packet);
                continue;
            }
        }
    }

    public OutputStream getOutputStream(){
        return output;
    }

    private void loginLoop() throws IOException{
        PacketChat packet;
        IChallenge challenge=null;
        String username=null;
        User selectedUser;

        while (!isConnected()){
            packet=getPacket();
            try{
                sanitizer.logoutSanitize(packet);
            }catch(PacketChatException e){
                Logger.w("Packet dropped: "+packet + " -> " + e.getMessage());
                continue;
            }
            try{
                switch(packet.getCommand()){
                    case PacketChat.AUTH:
                        username=new String(packet.getField(0));
                        selectedUser=ServerChatManager.getInstance().getDataBase().getUser(username);
        
                        if (selectedUser==null){
                            challenge=new RegisterChallenge(username);
                        }else{
                            challenge=selectedUser.getChallenge();
                        }
        
                        PacketChatFactory.createChallengePacket(challenge.get()).send(output);
                        break;
        
                    case PacketChat.CHALLENGE:
                        byte[] response=packet.getField(0);
                        PacketChat responsePacket;
        
                        if (challenge!=null && challenge.submit(response)){        
                            if (ServerChatManager.getInstance().isConnected(username)){
                                responsePacket=PacketChatFactory.createAuthPacket(false,"logged in another location");
                                
                            }else{
                                selectedUser=ServerChatManager.getInstance().getDataBase().getUser(username);
                                if (Arrays.asList(getServer().getTags()).contains(selectedUser.getTag())){
                                    this.user=selectedUser;
                                    ServerChatManager.getInstance().register(this);
                                    responsePacket=PacketChatFactory.createAuthPacket(true);
                                }else{
                                    responsePacket=PacketChatFactory.createAuthPacket(false,"Unauthorized connection");
                                }
                            }
                        }else{
                            responsePacket=PacketChatFactory.createAuthPacket(false,"challenge failed");
                        }
                        //clear challenge so it cannot be used again
                        challenge=null;
                        username=null;

                        responsePacket.send(output);
                        break;
                }
            }catch(PacketChatException e){
                throw new IOException(e.getMessage());
            }
        }
    }

    public void run(){
        this.server=(ServerChat)getArgs()[0];

        try{
            initStreams();
            sanitizer=new PacketChatSanitizer(this);
            loginLoop();
            mainloop();

        }catch(IOException e){}

        ServerChatManager.getInstance().remove(this);
        WorkerManager.getInstance().remove(this);
    }

    public String getDescription() {
        return String.format("service chat in %s", getSocket().getRemoteSocketAddress().toString());
    }
}
