import java.io.IOException;
import java.net.Socket;

public class ServerManager extends ServerSocketWorker{
    

    public final static int SERVER_MANAGER_PORT=4567;

    public ServerManager() {
        super(SERVER_MANAGER_PORT);
    }

    public String getDescription(){
        return "ServerManager";
    }

    public void run(){
        super.run();

        if (getServer()!=null){
            Logger.i(String.format("Server manager listening on port %d...", getPort()));

            while (true){
                try{
                    Socket client=getServer().accept();
                    new ClientManager(client);
                }catch(IOException e){
                    break;
                }
                
            }
        }
        WorkerManager.getInstance().remove(this);
    }
        
}
