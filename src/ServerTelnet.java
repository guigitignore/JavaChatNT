import java.io.IOException;
import java.net.Socket;

public class ServerTelnet extends ServerSocketWorker{
    private String upstreamHost;
    private int upStreamPort;

    public ServerTelnet(int port,String upstreamHost,int upstreamPort){
        super(port,upstreamHost,upstreamPort);
    }

    public ServerTelnet(int port,int upstreamPort){
        this(port,"localhost",upstreamPort);
    }

    public String getUpstreamHost(){
        return upstreamHost;
    }

    public int getUpstreamPort(){
        return upStreamPort;
    }


    public String getDescription(){
        return "ServerTelnet on port "+getPort();
    }

    public void run(){
        super.run();

        upstreamHost=(String)getArgs()[0];
        upStreamPort=(int)getArgs()[1];

        if (getServer()!=null){
            Logger.i(String.format("Server Telnet listening on port %d...", getPort()));

            while (true){
                try{
                    Socket client=getServer().accept();
                    new ServiceTelnet(client,this);
                }catch(IOException e){
                    break;
                }
                
            }
        }
        WorkerManager.getInstance().remove(this);
    }
}
