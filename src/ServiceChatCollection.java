import java.util.ArrayList;
import java.util.Collection;

public class ServiceChatCollection extends ArrayList<ServiceChat> {
    public ServiceChatCollection(){
        super();
    }

    public ServiceChatCollection(Collection<ServerChat> clients){
        super(clients);

    }

    public Collection<User> getUsers(){
        Collection<User> result=new ArrayList<>();

        for (ServiceChat client:this){
            User user=client.getUser();
            if (user!=null) result.add(user);
        }
        return result;
    }

    public Collection<String> getUsernames(){
        Collection<String> result=new ArrayList<>();
        for (User user:getUsers()){
            result.add(user.getName());
        }
        return result;
    }

    public Collection<IPacketChatOutput> getOutputs(){
        Collection<IPacketChatOutput> result=new ArrayList<>();

        for (ServiceChat client:this){
            result.add(client.getOutput().getOutput());
        }
        return result;
    }

    public PacketChatOutput toPacketChatOutput(){
        return new PacketChatOutput(new PacketChatMulticast(getOutputs()));
    }
    
}
