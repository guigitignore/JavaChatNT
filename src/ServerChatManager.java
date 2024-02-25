import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;


public class ServerChatManager {
    public final static String DATABASE_FILE="users.txt";

    private final static ServerChatManager instance=new ServerChatManager();

    public static ServerChatManager getInstance(){
        return instance;
    }
    private HashMap<String,ServiceChat> connectedUsers=new HashMap<>();

    private UserDatabase userDatabase=new UserDatabase(DATABASE_FILE);

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
        if (status){
            try{
                getClients().getOutput().sendFormattedMessage("\"%s\" is now connected",user.getUser().getName());
            }catch(PacketChatException e){
                Logger.w("Error occured while sending connection message: %s",e.getMessage());
            }
        }
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
            if (status){
                try{
                    getClients().getOutput().sendFormattedMessage("\"%s\" has disconnected",user.getUser().getName());
                }catch(PacketChatException e){
                    Logger.w("Error occured while sending diconnection message: %s",e.getMessage());
                }
            }
        }
        return status;
    }

    public ServiceChat getClient(String name){
        synchronized(connectedUsers){
            return connectedUsers.get(name);
        }
    }

    public ServiceChatCollection getClients(){
        synchronized(connectedUsers){
            return new ServiceChatCollection(connectedUsers.values());
        }
    }

    public ServiceChatCollection getClientsByTag(String tag){
        return new ServiceChatCollection(getClients().stream().filter(client -> client.getUser().getTag().equals(tag)).toList());
    }

    public ServiceChatCollection getClientsByName(Collection<String> names){
        return new ServiceChatCollection(names.stream().map(this::getClient).filter(Objects::nonNull).toList());
    }

    public ServiceChatCollection getClientsByName(String...names){
        return getClientsByName(Arrays.asList(names));
    }

    public Collection<String> getUsers(){
        synchronized(connectedUsers){
            return connectedUsers.keySet();
        }
    }

    //returns null if user is not found
    public ServiceChat getConnectedUser(String username){
        synchronized(connectedUsers){
            return connectedUsers.get(username);
        }
    }
}
