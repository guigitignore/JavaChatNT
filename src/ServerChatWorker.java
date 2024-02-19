import java.io.IOException;

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
        ServerChatManager manager=ServerChatManager.getInstance();
        float fillRate;

        while (true){
            try{
                //load balancer
                fillRate=manager.getQueueFillRate();
                if (workerId==0){
                    if (fillRate>0.5) new ServerChatWorker();
                }
                else if (fillRate<0.4) break;
                
                new ServerChatPacketHandler(manager.getPacket());
            }catch(IOException e){
                break;
            }
        }

        WorkerManager.getInstance().remove(this);
    }

    public void cancel() {
        interrupt();
    }
    
}