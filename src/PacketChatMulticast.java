import java.util.Collection;

public class PacketChatMulticast implements IPacketChatOutput {
    private IPacketChatOutput[] outputs;

    public PacketChatMulticast(IPacketChatOutput...packetChatOutputs){
        outputs=packetChatOutputs;
    }

    public PacketChatMulticast(Collection<IPacketChatOutput> packetChatOutputs){
        this(packetChatOutputs.toArray(new IPacketChatOutput[packetChatOutputs.size()]));
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
