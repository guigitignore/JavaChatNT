import java.io.IOException;
import java.net.Socket;

public class ServerChat extends ServerSocketWorker{
    
    public final static int SERVER_CHAT_PORT=1234;

    public ServerChat(int port,String...allowedTags){
        super();
    }

    public ServerChat(String...allowedTags){
        this(SERVER_CHAT_PORT,allowedTags);
    }

    public ServerChat(int port){
        this(port,User.DEFAULT_TAG);
    }

    public ServerChat(){
        this(SERVER_CHAT_PORT);
    }

    public int getPort(){
        return SERVER_CHAT_PORT;
    }

    public String getDescription(){
        return "ServerChat";
    }

    public void run(){
        super.run();

        if (server!=null){
            Logger.i(String.format("Server Chat listening on port %d...", getPort()));

            while (true){
                try{
                    Socket client=server.accept();
                    new ServiceChat(client);
                }catch(IOException e){
                    break;
                }
                
            }
        }
        WorkerManager.getInstance().remove(this);
    }
        
}

