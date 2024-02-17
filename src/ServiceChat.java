import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;


public class ServiceChat extends SocketWorker{
    private InputStream input;
    private OutputStream output;
    private IChallenge challenge=null;
    private String username=null;

    public ServiceChat(Socket socket) {
        super(socket);
    }

    private void initStreams() throws IOException{
        input=socket.getInputStream();
        output=socket.getOutputStream();
    }

    private void mainloop() throws IOException{
        PacketChat packet;
        BlockingQueue<PacketChat> queue=ServerChatManager.getInstance().getPacketQueue();
        PacketChatSanitizer sanitizer=new PacketChatSanitizer(this);

        while (true){
            try{
                packet=new PacketChat(input);
            }catch(PacketChatException e){
                break;
            }
            try{
                sanitizer.sanitize(packet);
            }catch(PacketChatException e){
                Logger.w("Packet dropped: "+packet + " -> " + e.getMessage());
                continue;
            }
            if (getUserName()==null){
                try{
                    handleLogin(packet);
                }catch(PacketChatException e){
                    throw new IOException(e.getMessage());
                }
            }else{
                try{
                    queue.put(packet);
                }catch(InterruptedException e){
                    Logger.e("Packet cannot be queued:"+packet);
                    continue;
                }
            }
        }
    }

    public OutputStream getOutputStream(){
        return output;
    }

    public String getUserName(){
        return username;
    }

    private void handleLogin(PacketChat packet) throws PacketChatException{
        switch(packet.getCommand()){
            case PacketChat.AUTH:
                String username=new String(packet.getField(0));
                User selectedUser=ServerChatManager.getInstance().getDataBase().getUser(username);

                if (selectedUser==null){
                    Logger.i("register challenge");
                    this.challenge=new RegisterChallenge(username);
                }else{
                    this.challenge=selectedUser.getChallenge();
                }

                PacketChatFactory.createChallengePacket(challenge.get()).send(output);
                break;

            case PacketChat.CHALLENGE:
                byte[] response=packet.getField(0);
                PacketChat responsePacket;

                if (this.challenge!=null && challenge.submit(response)){
                    //set username
                    this.username=challenge.getUser().getName();
                    //clear challenge so it cannot be used again
                    this.challenge=null;

                    if (ServerChatManager.getInstance().register(this)){
                        responsePacket=PacketChatFactory.createAuthPacket(true);
                    }else{
                        responsePacket=PacketChatFactory.createAuthPacket(false,"logged in another location");
                        //cancel login
                        this.username=null;
                    }
                }else{
                    responsePacket=PacketChatFactory.createAuthPacket(false,"challenge failed");
                }
                responsePacket.send(output);
                break;
        }
    }

    public void run(){
        

        try{
            initStreams();
            mainloop();

        }catch(IOException e){}

        WorkerManager.getInstance().remove(this);
    }

    public String getDescription() {
        return String.format("service chat in %s", socket.getRemoteSocketAddress().toString());
    }
}
