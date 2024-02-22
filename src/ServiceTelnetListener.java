public class ServiceTelnetListener extends Thread implements IWorker{
    private ServiceTelnet client;

    public ServiceTelnetListener(ServiceTelnet client){
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
                handlePacket(client.getUpstreamInput().getPacketChat());
            }catch(PacketChatException e){
                break;
            }
        }
        WorkerManager.getInstance().remove(this);
        //trigger parent cancel
        if (client.getStatus()) client.cancel();
    }

    private void handlePacket(PacketChat packet){
        switch(packet.getCommand()){
            case PacketChat.SEND_MSG:
                if (packet.getFieldsNumber()>=2){
                    String sender=new String(packet.getField(0));
                    String message=new String(packet.getField(1));
                    client.getOutput().println(sender+" "+message);
                }
                break;
            case PacketChat.LIST_USERS:
                if (packet.getFieldsNumber()>0){
                    client.getOutput().println("List of connected users:");

                    for (byte[] user:packet.getFields()){
                        client.getOutput().println("-"+new String(user));
                    }
                }
                break;
        }

        
    }


    public void cancel() {
        client.cancel();
    }

    
}
