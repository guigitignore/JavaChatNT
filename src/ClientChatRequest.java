import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientChatRequest {
    private HashMap<Integer,AtomicBoolean> confirmations=new HashMap<>();
    private AtomicInteger confirmationCounter=new AtomicInteger(0);
    private PacketChatOutput output;

    public ClientChatRequest(ClientChat client){
        output=new PacketChatOutput(client.getMessageInterface(),ClientChat.CLIENT_NAME);
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

        try{
            output.sendMessage(builder.toString());
        }catch(PacketChatException e){
            throw new InterruptedException("Cannot communicate with client");
        }
        
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
