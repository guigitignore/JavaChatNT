import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class ServerTelnet extends ServerSocketWorker{
    public final static int SERVER_TELNET_PORT=2345;

    public ServerTelnet(){
        super(SERVER_TELNET_PORT,1);
    }

    public String getDescription(){
        return "ServerTelnet";
    }

    public void run(){
        super.run();
        int t=(int)getArgs()[0];
        Logger.i(String.format("%d", t));

        if (getServer()!=null){
            Logger.i(String.format("Server Telnet listening on port %d...", getPort()));

            while (true){
                try{
                    Socket client=getServer().accept();
                    new ServiceTelnet(client);
                }catch(IOException e){
                    break;
                }
                
            }
        }
        WorkerManager.getInstance().remove(this);
    }
}
