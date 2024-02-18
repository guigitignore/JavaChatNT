import java.io.IOException;

public class ServerTelnetPacketHandler extends Thread implements IWorker{
    private ServiceTelnet client;

    public ServerTelnetPacketHandler(ServiceTelnet client){
        this.client=client;
        WorkerManager.getInstance().registerAndStart(this);
    }

    public boolean getStatus() {
        return client.getStatus();
    }


    public String getDescription() {
        return "listener of "+client.getDescription();
    }

    public void run(){
        while(true){
            try{
                handlePacket(client.getPacketInterface().getPacket());
            }catch(IOException e){
                break;
            }
        }
        WorkerManager.getInstance().remove(this);
    }

    private void handlePacket(PacketChat packet){
        if (packet.getCommand()==PacketChat.SEND_MSG && packet.getFieldsNumber()>=2){
            String sender=new String(packet.getField(0));
            String message=new String(packet.getField(1));
            client.getOutput().println(sender+" "+message);
        }
    }


    public void cancel() {
        client.cancel();
    }

    
}
