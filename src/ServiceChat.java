import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;


public class ServiceChat extends SocketWorker implements IServiceChat{
    private ServerChat server;
    private PacketChatOutput input=null;
    private PacketChatOutput output=null;
    private ServiceChatInput inputServiceChat=null;
    private ServiceChatOutput outputServiceChat=null;
    private IPacketChatInterface packetChatInterface=null;
    private ClientType clientType;

    private final static int LINE_FEED=10;

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
        return inputServiceChat==null?null:inputServiceChat.getUser();
    }

    public ServerChat getServer(){
        return server;
    }

    public ClientType getClientType(){
        return clientType;
    }

    private ClientType identifyClientType() throws IOException{
        ClientType clientType;

        getSocket().getOutputStream().write("Welcome to JavaChatNT. Press [enter] to continue...".getBytes());
        InputStream inputStream=getSocket().getInputStream();

        int inputValue=inputStream.read();

        if (inputValue==0xFF){
            clientType=ClientType.PACKETCHAT_CLIENT;
        }else{
            clientType=ClientType.TELNET_CLIENT;
            while (inputValue!=LINE_FEED){

                if (inputValue==-1){
                    throw new IOException("end of stream");
                }
                inputValue=inputStream.read();
            }
        }

        return clientType;
    }

    private void setupClientInterface() throws IOException{
        ClientType clientType=identifyClientType();
            
        switch (clientType) {
            case PACKETCHAT_CLIENT:
                packetChatInterface=new PacketChatRawInterface(getSocket());
                break;
            case TELNET_CLIENT:
                packetChatInterface=new PacketChatTelnetInterface(getSocket());
                break;
            default:
                throw new IOException("Unhandled client type");
        }
    }

    private void initStreams() throws IOException{
        inputServiceChat= new ServiceChatInput(this);
        input=new PacketChatOutput(inputServiceChat);
        outputServiceChat=new ServiceChatOutput(this, packetChatInterface);
        output=new PacketChatOutput(outputServiceChat);
    }

    private void mainloop() throws IOException{
        PacketChat packet;

        while (true){
            try{
                packet=packetChatInterface.getPacketChat();
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
    }

    public void run(){
        this.server=(ServerChat)getArgs()[0];
        
        try{
            setupClientInterface();
            initStreams();
            mainloop();
        }catch(IOException e){}

        cancel();
        WorkerManager.getInstance().remove(this);
    }

    public String getDescription() {
        return String.format("ServiceChat in %s", getSocket().getRemoteSocketAddress().toString());
    }

    public void cancel(){
        ServerChatManager.getInstance().remove(this);
        if (outputServiceChat!=null) outputServiceChat.cancel();
        super.cancel();
    }
}
