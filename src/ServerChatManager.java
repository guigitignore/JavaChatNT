import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.bouncycastle.util.Arrays;


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

    public PacketChat getPacket() throws IOException{
        try{
            return queue.take();
        }catch(InterruptedException e){
            throw new IOException("Cannot take packet from queue");
        }
    }

    public float getQueueFillRate(){
        return (queue.size())/(queue.size()+queue.remainingCapacity());
    }

    public boolean putPacket(PacketChat packet){
        return queue.add(packet);
    }

    public UserDatabase getDataBase(){
        return userDatabase;
    }

    public boolean isConnected(String username){
        synchronized(connectedUsers){
            return connectedUsers.containsKey(username);
        }
    }

    public boolean register(ServiceChat user){
        boolean status;
        synchronized(connectedUsers){
            status=connectedUsers.putIfAbsent(user.getUser().getName(), user)==null;
        }
        if (status) sendMessage("\"%s\" is now connected",user.getUser().getName());
        return status;
    }

    public boolean remove(ServiceChat user){
        boolean status;
        User infos=user.getUser();
        if (infos==null){
            status=false;
        }else{
            synchronized(connectedUsers){
                status=connectedUsers.remove(infos.getName(),user);
            }
            if (status) sendMessage("\"%s\" has disconnected",user.getUser().getName());
        }
        return status;
    }

    public ServiceChatCollection getUsers(){
        synchronized(connectedUsers){
            return connectedUsers.values();
        }
    }

    public ServiceChatCollection getUsersByTag(String tag){
        ServiceChatCollection result=new ServiceChatCollection();
        for (ServiceChat user:getUsers()){
            if (user.getUser().getTag().equals(tag)){
                result.add(user);
            }
        }
        return result;
    }

    public ServiceChatCollection getUsersByName(String...names){
        ServiceChatCollection result=new ServiceChatCollection();
        for (ServiceChat user:getUsers()){
            result.add(user);
        }
        return result;
    }

    public Set<String> getUsernames(){
        synchronized(connectedUsers){
            return connectedUsers.keySet();
        }
    }

    public PacketChat getListUsersPacket(){
        Collection<String> users=getUsernames();
        return PacketChatFactory.createListUserPacket(users.toArray(new String[users.size()]));
    }

    //returns null if user is not found
    public ServiceChat getConnectedUser(String username){
        synchronized(connectedUsers){
            return connectedUsers.get(username);
        }
    }

    public void sendTaggedMessage(String tag,String format,Object...args){
        String message=String.format(format,args);

        for (ServiceChat user:ServerChatManager.getInstance().getUsers()){
            if (user.getUser().getTag().equals(tag)){
                try{
                    user.getOutput().sendMessage(message);
                }catch(PacketChatException e){
                    Logger.w("Cannot send message to %s",user.getUser().getName());
                }
                
            }
        }
    }

    public void sendMessage(String format,Object...args){
        String message=String.format(format,args);

        for (ServiceChat user:ServerChatManager.getInstance().getUsers()){
            try{
                user.getOutput().sendMessage(message);
            }catch(IOException e){
                Logger.w("Cannot send message to %s",user.getUser().getName());
            }
        }
    }

    public void sendAdminMessage(String message,Object...args){
        sendTaggedMessage(User.ADMIN_TAG, message, args);
    }

    private ServerChatManager(){
        new ServerChatWorker();
    }
}
