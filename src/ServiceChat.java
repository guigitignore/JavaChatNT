import java.io.IOException;
import java.net.Socket;


public class ServiceChat extends SocketWorker implements IServiceChat{
    private ServerChat server;
    private PacketChatOutput input=null;
    private PacketChatOutput output=null;
    private ServiceChatInput inputServiceChat;

    public ServiceChat(Socket socket,ServerChat server) {
        super(socket,server);
    }

    public PacketChatOutput getInput(){
        return input;
    }

    public PacketChatOutput getOutput(){
        return output;
    }

    public User getUser(){
        return inputServiceChat.getUser();
    }

    public ServerChat getServer(){
        return server;
    }

    public void run(){
        PacketChat packet;

        this.server=(ServerChat)getArgs()[0];
        
        try{
            IPacketChatInput primaryInput=new InputStreamPacketChat(getSocket().getInputStream());
            IPacketChatOutput primaryOutput=new OutputStreamPacketChat(getSocket().getOutputStream());
            inputServiceChat= new ServiceChatInput(this);
            input=new PacketChatOutput(inputServiceChat);
            output=new PacketChatOutput(new ServiceChatOutput(this, primaryOutput));

            while (true){
                try{
                    packet=primaryInput.getPacketChat();
                }catch(PacketChatException e){
                    break;
                }
                try{
                    input.sendPacket(packet);
                }catch(PacketChatException e){
                    if (getUser()==null){
                        Logger.w("Packet dropped on input for %s: %s",getDescription(),e.getMessage());
                    }else{
                        Logger.w("Packet dropped on input for user \"%s\" : %s",getUser().getName(),e.getMessage());
                    }
                }
            }

        }catch(IOException e){}

        ServerChatManager.getInstance().remove(this);
        WorkerManager.getInstance().remove(this);
    }

    public String getDescription() {
        return String.format("ServiceChat in %s", getSocket().getRemoteSocketAddress().toString());
    }
}
