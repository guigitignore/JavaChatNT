import java.io.IOException;
import java.net.Socket;

public class ServerTelnet extends ServerSocketWorker{
    public final static int SERVER_TELNET_PORT=2345;

    public int getPort(){
        return SERVER_TELNET_PORT;
    }

    public String getDescription(){
        return "ServerTelnet";
    }

    public void run(){
        super.run();

        if (server!=null){
            Logger.i(String.format("Server Telnet listening on port %d...", getPort()));

            while (true){
                try{
                    Socket client=server.accept();
                    new ServiceTelnet(client);
                }catch(IOException e){
                    break;
                }
                
            }
        }
        WorkerManager.getInstance().remove(this);
    }
}
