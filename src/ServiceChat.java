import java.net.Socket;


public class ServiceChat extends SocketWorker{
    public ServiceChat(Socket socket) {
        super(socket);
    }

    public void run(){
        
        WorkerManager.getInstance().remove(this);
    }

    public String getDescription() {
        return String.format("service chat in %s", socket.getRemoteSocketAddress().toString());
    }
}
