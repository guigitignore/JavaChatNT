import java.io.IOException;
import java.net.Socket;

public abstract class SocketWorker extends Thread implements IWorker{
    private Socket socket;
    private Object[] args;

    public SocketWorker(Socket socket,Object...args){
        this.socket=socket;
        this.args=args;
        WorkerManager.getInstance().registerAndStart(this);
    }

    protected Object[] getArgs(){
        return args;
    }

    public Socket getSocket(){
        return socket;
    }

    public boolean getStatus() {
        return !socket.isClosed();
    }

    public void cancel() {
        if (!socket.isClosed()){
            try{
                socket.close();
            }catch(IOException e){}
        }
        //WorkerManager.getInstance().remove(this);
    }
}
