import java.io.IOException;
import java.net.Socket;

public abstract class SocketWorker extends Thread implements IWorker{
    protected Socket socket;

    public SocketWorker(Socket socket){
        this.socket=socket;
        WorkerManager.getInstance().registerAndStart(this);
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
