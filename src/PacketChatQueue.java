import java.util.concurrent.ArrayBlockingQueue;

public class PacketChatQueue implements IPacketChatInterface{
    private ArrayBlockingQueue<PacketChat> queue;

    public PacketChatQueue(int capacity){
        queue=new ArrayBlockingQueue<>(capacity);
    }
    
    public void putPacketChat(PacketChat packet) throws PacketChatException {
        if (!queue.add(packet)){
            throw new PacketChatException("Cannot add PacketChat in queue");
        }
    }

    public PacketChat getPacketChat() throws PacketChatException {
        try{
            return queue.take();
        }catch(InterruptedException e){
            throw new PacketChatException("Cannot get PacketChat in queue");
        }
    }
    
}
