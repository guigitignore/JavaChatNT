public class ClientChatMessageInput extends Thread implements IWorker{
    private ClientChat client;

    public ClientChatMessageInput(ClientChat client){
        this.client=client;
        WorkerManager.getInstance().registerAndStart(this);
    }

    public boolean getStatus() {
        return !isInterrupted();
    }

    public String getDescription() {
        return "ClientChatInput";
    }

    public void cancel() {
        interrupt();
    }

    public void run(){
        while (true){
            try{
                client.getUpstreamInterface().putPacketChat(client.getMessageInterface().getPacketChat());
            }catch(PacketChatException e){
                break;
            }
        }

        WorkerManager.getInstance().remove(this);
    }
    
}
