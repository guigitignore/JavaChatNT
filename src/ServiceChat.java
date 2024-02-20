import java.io.IOException;
import java.net.Socket;


public class ServiceChat extends SocketWorker{
    private ServerChat server;
    private ServiceChatInput input=null;
    private ServiceChatOutput output=null;

    public ServiceChat(Socket socket,ServerChat server) {
        super(socket,server);
    }

    public IPacketChatOutput getInput(){
        return input;
    }

    public IPacketChatOutput getOutput(){
        return output;
    }

    public User getUser(){
        return input.getUser();
    }

    public ServerChat getServer(){
        return server;
    }

    public void run(){
        this.server=(ServerChat)getArgs()[0];
        
        try{
            IPacketChatInput primaryInput=new InputStreamPacketChat(getSocket().getInputStream());
            IPacketChatOutput primaryOutput=new OutputStreamPacketChat(getSocket().getOutputStream());
            input=new ServiceChatInput(this);
            output=new ServiceChatOutput(this, primaryOutput);

            while (true){
                PacketChat packet=primaryInput.getPacketChat();
                try{
                    input.putPacketChat(packet);
                }catch(IOException e){
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
        return String.format("service chat in %s", getSocket().getRemoteSocketAddress().toString());
    }
}
