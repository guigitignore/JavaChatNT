import java.util.ArrayDeque;

public class PacketChatBucket implements IPacketChatOutput{

    private ArrayDeque<PacketChat> stack;
    private int capacity;

    public PacketChatBucket(int capacity){
        stack=new ArrayDeque<>(capacity);
        this.capacity=capacity;
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        synchronized(stack){
            if (stack.size()==capacity){
                stack.removeLast();
            }
            stack.addFirst(packet);
        }
    }

    public PacketChat getPacketByType(byte command) throws PacketChatException{
        
        return null;
    }


    
}
