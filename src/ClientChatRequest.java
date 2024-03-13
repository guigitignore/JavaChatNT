import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientChatRequest {
    private ClientChat client;
    private HashMap<Integer,AtomicBoolean> confirmations=new HashMap<>();
    private AtomicInteger confirmationCounter=new AtomicInteger(0);

    public ClientChatRequest(ClientChat client){
        this.client=client;
    }

    private void sendMessageToClient(String format,Object...args){
        PacketChat packet;
        try{
            packet=PacketChatFactory.createMessagePacket(ClientChat.CLIENT_NAME,String.format(format, args));
            client.getMessageInterface().putPacketChat(packet);
        }catch(PacketChatException e){
            Logger.e("Cannot send message to client");
        }
    }

    public boolean sendConfirmationRequest(String message) throws InterruptedException{
        int confirmationId=confirmationCounter.incrementAndGet();
        AtomicBoolean request=new AtomicBoolean(false);

        synchronized(confirmations){
            confirmations.put(confirmationId,request);
        }
        
        StringBuilder builder=new StringBuilder();
        builder.append(message);
        builder.append(String.format("\n\"/allow %d\" to accept",confirmationId));
        builder.append(String.format("\n\"/deny %d\" to reject\n",confirmationId));

        sendMessageToClient(message);
        
        synchronized(request){
            request.wait();
        }
        return request.get();
    }

    public boolean sendConfirmationResponse(int requestId,boolean res){
        AtomicBoolean request;
        boolean result=false;

        synchronized(confirmations){
            request=confirmations.remove(requestId);
        }
        if (request!=null){
            request.set(res);
            synchronized(request){
                request.notify();
            }
            result=true;
        }
        return result;
    }
}
