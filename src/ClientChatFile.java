import java.util.HashMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.ArrayBlockingQueue;

public class ClientChatFile extends LoopWorker implements IPacketChatOutput {
    public final static int QUEUE_CAPACITY=16;

    private ClientChat client;
    
    private ArrayBlockingQueue<SimpleEntry<String,String>> fileQueue;

    public ClientChatFile(ClientChat client){
        super(client);
    }

    public String getDescription() {
        return "ClientChatFile";
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        
    }

    public void queueFile(String filename,String dest){
        fileQueue.add(new SimpleEntry<>(filename,dest));
    }

    public void setup() throws Exception {
        client=(ClientChat)getArgs()[0];
        fileQueue=new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        
    }

    public void init() throws Exception {}

    public void loop() throws Exception {
        SimpleEntry<String,String> entry=fileQueue.take();

        
        
    }

    public void end() throws Exception {
        WorkerManager.getInstance().cancelAll();
    }
    
}
