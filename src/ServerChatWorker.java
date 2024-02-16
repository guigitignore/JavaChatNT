import java.util.concurrent.BlockingQueue;

class ServerChatWorker extends Thread implements IWorker{
    private static int workerCounter=0;

    private int workerId;

    public ServerChatWorker(){
        workerId=workerCounter++;
        WorkerManager.getInstance().registerAndStart(this);
    }

    public boolean getStatus() {
        return !isInterrupted();
    }

    public String getDescription() {
        return "ServerChat Worker "+workerId;
    }

    public void run() {
        BlockingQueue<PacketChat> queue=ServerChatManager.getInstance().getPacketQueue();

        while (true){
            try{
                //load balancer
                if (workerId==0){
                    if (queue.remainingCapacity()<queue.size()) new ServerChatWorker();
                }
                else if (queue.size()*2<queue.remainingCapacity()) break;
                
                new ServerChatPacketHandler(queue.take());
            }catch(InterruptedException e){
                break;
            }
        }

        WorkerManager.getInstance().remove(this);
    }

    public void cancel() {
        interrupt();
    }
    
}