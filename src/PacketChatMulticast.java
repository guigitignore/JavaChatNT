import java.util.Arrays;
import java.util.Collection;

public class PacketChatMulticast implements IPacketChatOutput {
    private Collection<IPacketChatOutput> outputs;

    public PacketChatMulticast(Collection<IPacketChatOutput> packetChatOutputs){
        outputs=packetChatOutputs;
    }

    public PacketChatMulticast(IPacketChatOutput...packetChatOutputs){
        this(Arrays.asList(packetChatOutputs));
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        int errors=0;
        for (IPacketChatOutput output:outputs){
            try{
                output.putPacketChat(packet);
            }catch(PacketChatException e){
                errors++;
            }
        }
        if (errors!=0){
            throw new PacketChatException("an error occured during packet multicast");
        }
    }
    
}
