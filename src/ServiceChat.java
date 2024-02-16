import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;


public class ServiceChat extends SocketWorker{
    private InputStream input;
    private OutputStream output;

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

        while (true){
            try{
                packet=new PacketChat(input);
            }catch(PacketChatException e){
                break;
            }
            try{
                new PacketChatSanitizer(packet,this);
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

    public String getUserName(){
        return "inconito";
    }

    public void run(){
        

        try{
            initStreams();
            ServerChatManager.getInstance().register(this);
            mainloop();

        }catch(IOException e){}

        WorkerManager.getInstance().remove(this);
    }

    public String getDescription() {
        return String.format("service chat in %s", socket.getRemoteSocketAddress().toString());
    }
}
