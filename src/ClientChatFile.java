import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

public class ClientChatFile extends LoopWorker implements IPacketChatOutput {
    public final static int QUEUE_CAPACITY=16;

    private ClientChat client;
    private HashMap<Byte,String> nounces;
    private byte nounceCounter=0;
    private ArrayBlockingQueue<String> fileQueue;

    public ClientChatFile(ClientChat client){
        super(client);
    }

    public String getDescription() {
        return "ClientChatFile";
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        
    }

    public void queueFile(String filename){
        fileQueue.add(filename);
    }

    public void setup() throws Exception {
        client=(ClientChat)getArgs()[0];
        fileQueue=new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        nounces=new HashMap<>();
    }

    public void init() throws Exception {}

    public void loop() throws Exception {
        String filename=fileQueue.take();
        
    }

    public void end() throws Exception {
        WorkerManager.getInstance().cancelAll();
    }
    
}
