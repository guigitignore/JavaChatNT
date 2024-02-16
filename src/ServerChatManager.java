import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ServerChatManager {
    public final static int PACKET_QUEUE_SIZE=100;

    private final static ServerChatManager instance=new ServerChatManager();

    public static ServerChatManager getInstance(){
        return instance;
    }

    private BlockingQueue<PacketChat> queue=new ArrayBlockingQueue<>(PACKET_QUEUE_SIZE);
    private HashMap<String,ServiceChat> connectedUsers=new HashMap<>();

    public BlockingQueue<PacketChat> getPacketQueue(){
        return queue;
    }

    public boolean register(ServiceChat user){
        return connectedUsers.putIfAbsent(user.getName(), user)==null;
    }

    public boolean remove(ServiceChat user){
        return connectedUsers.remove(user.getUserName(),user);
    }

    public Collection<ServiceChat> getConnectedUsers(){
        return connectedUsers.values();
    }

    //returns null if user is not found
    public ServiceChat getConnectedUser(String username){
        return connectedUsers.get(username);
    }

    private ServerChatManager(){
        new ServerChatWorker();
    }
}
