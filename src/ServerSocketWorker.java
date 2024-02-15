import java.io.IOException;
import java.net.ServerSocket;

public abstract class ServerSocketWorker extends Thread implements IWorker{
    protected ServerSocket server;

    public abstract int getPort();

    public boolean getStatus(){
        boolean status;
        if (server==null) status=false;
        else{
            if (server.isClosed()) status=false;
            else status=true;
        }
        return status;
    }

    public void run(){
        try{
            server=new ServerSocket(getPort());
        }catch(IOException e){
            server=null;
        }
    }

    public void cancel(){
        if (!server.isClosed()){
            try{
                server.close();
            }catch(IOException e){}
        }
    }
}
