import java.io.IOException;
import java.net.Socket;

public class ServerChat extends ServerSocketWorker{
    
    public final static int SERVER_CHAT_PORT=1234;
    private String[] tags;

    public ServerChat(int port,String...allowedTags){
        super(port,(Object[])allowedTags);
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

    public String getDescription(){
        return "ServerChat";
    }

    public String[] getTags(){
        return tags;
    }

    public void run(){
        super.run();
        this.tags=(String[])getArgs();

        if (getServer()!=null){
            Logger.i(String.format("Server Chat listening on port %d...", getPort()));

            while (true){
                try{
                    Socket client=getServer().accept();
                    new ServiceChat(client,this);
                }catch(IOException e){
                    break;
                }
                
            }
        }
        WorkerManager.getInstance().remove(this);
    }
        
}

