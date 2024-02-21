import java.util.ArrayList;
import java.util.Collection;

public class ServiceChatCollection extends ArrayList<ServiceChat> {
    public ServiceChatCollection(){
        super();
    }

    public ServiceChatCollection(Collection<ServiceChat> clients){
        super(clients);
    }

    public Collection<User> getUsers(){
        return stream().map(ServiceChat::getUser).toList();
    }

    public Collection<String> getUsernames(){
        return stream().map(ServiceChat::getUser).map(User::getName).toList();
    }

    public Collection<IPacketChatOutput> getOutputs(){
        return stream().map(ServiceChat::getOutput).map(PacketChatOutput::getInterface).toList();
    }

    public PacketChatOutput toPacketChatOutput(){
        return new PacketChatOutput(new PacketChatMulticast(getOutputs()));
    }
    
}
