package packetchat;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class PacketChatBucket implements IPacketChatOutput{
 
    private ArrayDeque<PacketChat> stack;
    private int capacity;
    private HashMap<String,ArrayDeque<AtomicReference<PacketChat>>> waiters;
    

    public PacketChatBucket(int capacity){
        stack=new ArrayDeque<>(capacity);
        waiters=new HashMap<>();
        this.capacity=capacity;
    }

    private boolean isAckPacket(PacketChat packet){
        boolean result=false;
        byte command=packet.getCommand();
        if (command==PacketChat.FILE_ACK || command==PacketChat.FILE_INIT || command==PacketChat.FILE_OVER){
            if (packet.getFieldsNumber()==0){
                result=true;
            }
        }
        return result;
    }

    private String getCommandEventId(byte command){
        return String.format("command{%d}", command&0xFF);
    }

    private String getNounceEventId(byte nounce){
        return String.format("nounce{%d}", nounce&0xFF);
    }

    private boolean handleWaiters(String event,PacketChat packet){
        boolean result=false;
        ArrayDeque<AtomicReference<PacketChat>> locks;
        AtomicReference<PacketChat> lock;

        synchronized(waiters){
            locks=waiters.get(event);
        }
        if (locks!=null){
            synchronized(locks){
                while(locks.size()>0){
                    lock=locks.removeLast();
                    if (lock==null) break;
                    
                    synchronized(lock){
                        //if lock!=null then it means that it has already been consumed
                        if (lock.get()==null){
                            lock.set(packet);
                            lock.notify();
                            result=true;
                            break;
                        }
                    }
                }
            }
            
        }

        return result;
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        if (isAckPacket(packet)){
            if (handleWaiters(getNounceEventId(packet.getParam()), packet)) return;
        }else{
            if (handleWaiters(getCommandEventId(packet.getCommand()), packet)) return;
        }
        
        synchronized(stack){
            if (stack.size()==capacity){
                stack.removeLast();
            }
            stack.addFirst(packet);
        }
    }

    private PacketChat waitPacket(String...events) throws PacketChatException{
        ArrayDeque<AtomicReference<PacketChat>> locks;
        AtomicReference<PacketChat> lock,temp;
        PacketChat result;

        lock=new AtomicReference<>();
        
        for (String event:events){
            synchronized(waiters){
                waiters.putIfAbsent(event, new ArrayDeque<>());
                locks=waiters.get(event);
            }
            synchronized(locks){
                //clean consumed events
                if (locks.size()>0){
                    while (true){
                        temp=locks.peekLast();
                        if (temp==null || temp.get()==null) break;
                        locks.removeLast();
                    }
                }
                locks.addFirst(lock);
            }
        }
        
        try{
            synchronized(lock){
                lock.wait();
                result=lock.get();
            }
        }catch(InterruptedException e){
            throw new PacketChatException("interrupted");
        }
            
        return result;
    }

    public PacketChat waitPacketByType(byte...commands) throws PacketChatException{
        PacketChat result=getPacketByType(commands);
        
        if (result==null){
            String[] events=new String[commands.length];
            for (int i=0;i<commands.length;i++) events[i]=getCommandEventId(commands[i]);
            result=waitPacket(events);
        }
        
        return result;
    }

    public PacketChat waitPacketAckByNounce(byte nounce) throws PacketChatException{
        PacketChat result=getPacketAckByNounce(nounce);
        if (result==null) result=waitPacket(getNounceEventId(nounce));
        return result;
    }

    private PacketChat getPacketByType(byte...commands){
        PacketChat result=null;
        byte eltCommand;

        synchronized(stack){
            for (PacketChat elt:stack){
                eltCommand=elt.getCommand();
                for (byte command:commands){
                    if (eltCommand==command && stack.remove(elt)){
                        result=elt;
                        break;
                    }
                }
            }
        }
        return result;
    }

    private PacketChat getPacketAckByNounce(byte nounce){
        PacketChat result=null;

        synchronized(stack){
            for (PacketChat elt:stack){
                if (isAckPacket(elt) && elt.getParam()==nounce && stack.remove(elt)){
                    result=elt;
                    break;
                }
            }
        }
        return result;
    }    
}
