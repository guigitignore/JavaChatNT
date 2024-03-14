import java.util.ArrayList;
import java.util.Collection;

public class ServiceChatCollection extends ArrayList<ServiceChat> implements IServiceChat{
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

    public Collection<IPacketChatOutput> getOutputInterfaces(){
        return stream().map(ServiceChat::getOutput).map(PacketChatOutput::getInterface).toList();
    }

    public Collection<IPacketChatOutput> getInputInterfaces(){
        return stream().map(ServiceChat::getInput).map(PacketChatOutput::getInterface).toList();
    }

    public PacketChatOutput getOutput(){
        return new PacketChatOutput(new PacketChatMulticast(getOutputInterfaces()),ServiceChat.SERVER_NAME);
    }

    public PacketChatOutput getInput(){
        return new PacketChatOutput(new PacketChatMulticast(getInputInterfaces()),ServiceChat.SERVER_NAME);
    }
    
}
