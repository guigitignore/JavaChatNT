import java.io.IOException;
import java.net.Socket;

public class ServerManager extends ServerSocketWorker{
    public final static int SERVER_MANAGER_PORT=4567;

    public int getPort(){
        return SERVER_MANAGER_PORT;
    }

    public String getDescription(){
        return "ServerManager";
    }

    public void run(){
        super.run();

        if (server!=null){
            while (true){
                try{
                    Socket client=server.accept();
                    new ClientManager(client);
                }catch(IOException e){
                    break;
                }
                
            }
        }
        WorkerManager.getInstance().remove(this);
    }
        
}
