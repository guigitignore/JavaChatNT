package worker;
import java.io.IOException;
import java.net.ServerSocket;

import util.Logger;

public abstract class ServerSocketWorker extends Thread implements IWorker{
    private ServerSocket server;
    private int port;
    private Object[] args;

    public ServerSocketWorker(int port,Object...args){
        this.port=port;
        this.args=args;
        WorkerManager.getInstance().registerAndStart(this);
    }

    public ServerSocket getServer(){
        return server;
    }

    protected Object[] getArgs(){
        return args;
    }

    public int getPort(){
        return port;
    }

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
            server=new ServerSocket(port);
        }catch(IOException e){
            Logger.e(e.getMessage());
            server=null;
        }
    }

    public void cancel(){
        synchronized(server){
            if (!server.isClosed()){
                try{
                    server.close();
                }catch(IOException e){}
            }
        }
    }
}
