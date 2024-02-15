import java.io.IOException;
import java.net.Socket;

public class ServerChat extends ServerSocketWorker{
    
    public final static int SERVER_CHAT_PORT=1234;

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

