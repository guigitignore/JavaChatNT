import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class ServerChatManager {
    public final static int PACKET_QUEUE_SIZE=100;
    public final static String DATABASE_FILE="users.txt";

    private final static ServerChatManager instance=new ServerChatManager();

    public static ServerChatManager getInstance(){
        return instance;
    }

    private BlockingQueue<PacketChat> queue=new ArrayBlockingQueue<>(PACKET_QUEUE_SIZE);
    private HashMap<String,ServiceChat> connectedUsers=new HashMap<>();

    private UserDatabase userDatabase=new UserDatabase(DATABASE_FILE);

    public BlockingQueue<PacketChat> getPacketQueue(){
        return queue;
    }

    public UserDatabase getDataBase(){
        return userDatabase;
    }

    public boolean isConnected(String username){
        return connectedUsers.containsKey(username);
    }

    public boolean register(ServiceChat user){
        return connectedUsers.putIfAbsent(user.getUser().getName(), user)==null;
    }

    public boolean remove(ServiceChat user){
        boolean status;
        User infos=user.getUser();
        if (infos==null){
            status=false;
        }else{
            status=connectedUsers.remove(infos.getName(),user);
        }
        return status;
    }

    public Collection<ServiceChat> getConnectedUsers(){
        return connectedUsers.values();
    }

    public Set<String> getConnectedUsernames(){
        return connectedUsers.keySet();
    }

    public PacketChat getListUsersPacket(){
        Set<String> users=connectedUsers.keySet();
        return PacketChatFactory.createListUserPacket(users.toArray(new String[users.size()]));
    }

    //returns null if user is not found
    public ServiceChat getConnectedUser(String username){
        return connectedUsers.get(username);
    }

    private ServerChatManager(){
        new ServerChatWorker();
    }
}
