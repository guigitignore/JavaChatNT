import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class ServiceChatCollection extends ArrayList<ServiceChat> implements IServiceChat{
    public ServiceChatCollection(){
        super();
    }

    public ServiceChatCollection(Collection<ServiceChat> clients){
        super(clients);
    }

    public Collection<User> getUsers(){
        return stream().map(ServiceChat::getUser).collect(Collectors.toList());
    }

    public Collection<String> getUsernames(){
        return stream().map(ServiceChat::getUser).map(User::getName).collect(Collectors.toList());
    }

    public Collection<IPacketChatOutput> getOutputInterfaces(){
        return stream().map(ServiceChat::getOutput).map(PacketChatOutput::getInterface).collect(Collectors.toList());
    }

    public Collection<IPacketChatOutput> getInputInterfaces(){
        return stream().map(ServiceChat::getInput).map(PacketChatOutput::getInterface).collect(Collectors.toList());
    }

    public PacketChatOutput getOutput(){
        return new PacketChatOutput(new PacketChatMulticast(getOutputInterfaces()),ServiceChat.SERVER_NAME);
    }

    public PacketChatOutput getInput(){
        return new PacketChatOutput(new PacketChatMulticast(getInputInterfaces()),ServiceChat.SERVER_NAME);
    }
    
}
