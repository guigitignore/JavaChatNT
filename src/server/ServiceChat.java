package server;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;

import packetchat.IPacketChatInterface;
import packetchat.IServiceChat;
import packetchat.PacketChat;
import packetchat.PacketChatException;
import packetchat.PacketChatOutput;
import shared.PacketChatRawInterface;
import shared.PacketChatSanitizer;
import shared.PacketChatTelnetInterface;
import user.IUserConnection;
import user.User;
import util.ClientType;
import util.Logger;
import util.NounceManager;
import worker.SocketWorker;
import worker.WorkerManager;


public class ServiceChat extends SocketWorker implements IServiceChat,IPacketChatInterface,IUserConnection{
    private ServerChat server;
    private ServiceChatInput inputServiceChat=null;
    private ServiceChatOutput outputServiceChat=null;
    private IPacketChatInterface packetChatInterface=null;
    private ClientType clientType;
    private NounceManager incomingFiles=new NounceManager();
    private NounceManager outgoingFiles=new NounceManager();

    private final static int LINE_FEED=10;
    public final static String SERVER_NAME="[SERVER]";

    public ServiceChat(Socket socket,ServerChat server) {
        super(socket,server);
    }

    public PacketChatOutput getInput(){
        return new PacketChatOutput(inputServiceChat,SERVER_NAME);
    }

    public PacketChatOutput getOutput(){
        return new PacketChatOutput(outputServiceChat,SERVER_NAME);
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

    public NounceManager getIncomingFiles(){
        return incomingFiles;
    }

    public NounceManager getOutgoingFiles(){
        return outgoingFiles;
    }

    private int readInputStreamByte(InputStream in) throws IOException{
        int result=in.read();
        if (result==-1) throw new IOException("end of stream");
        return result;
    }

    private ClientType identifyClientType(InputStream in,OutputStream out) throws IOException{
        ClientType clientType;

        out.write("Welcome to JavaChatNT. Press [enter] to continue...".getBytes());
        int identifierByte=readInputStreamByte(in);

        if (identifierByte==0xFF){
            clientType=ClientType.PACKETCHAT_CLIENT;
            //read the remaining bytes of hello packet to make sure alignment is correct.
            for (int i=0;i<7;i++) readInputStreamByte(in);
        }else{
            clientType=ClientType.TELNET_CLIENT;
            while (identifierByte!=LINE_FEED) identifierByte=readInputStreamByte(in);
        }
        return clientType;
    }

    private void setupClientInterface() throws IOException{
        InputStream in=getSocket().getInputStream();
        OutputStream out=getSocket().getOutputStream();
        clientType=identifyClientType(in,out);
            
        switch (clientType) {
            case PACKETCHAT_CLIENT:
                packetChatInterface=new PacketChatRawInterface(in,out);
                break;
            case TELNET_CLIENT:
                packetChatInterface=new PacketChatTelnetInterface(in,new PrintStream(out));
                break;
            default:
                throw new IOException("Unhandled client type");
        }
    }

    private void initStreams() throws IOException{
        inputServiceChat= new ServiceChatInput(this);
        outputServiceChat=new ServiceChatOutput(this);
    }

    private void mainloop(){
        PacketChat packet;
        PacketChatSanitizer sanitizer=new PacketChatSanitizer(this);

        while (true){
            try{
                packet=getPacketChat();
            }catch(PacketChatException e){
                //e.printStackTrace();
                break;
            }
            try{
                sanitizer.server(packet);
                inputServiceChat.putPacketChat(packet);
            }catch(PacketChatException e){
                if (getUser()==null){
                    Logger.w("Packet dropped on input from %s: %s",getDescription(),e.getMessage());
                }else{
                    Logger.w("Packet dropped on input from user \"%s\" : %s",getUser().getName(),e.getMessage());
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

        ServerChatManager.getInstance().remove(this);
        server.remove(this);
        WorkerManager.getInstance().remove(this);
        if (outputServiceChat!=null) outputServiceChat.cancel();
        cancel();
    }

    public String getDescription() {
        return String.format("ServiceChat in %s", getSocket().getRemoteSocketAddress().toString());
    }

    public PacketChat getPacketChat() throws PacketChatException {
        PacketChat packet=packetChatInterface.getPacketChat();
        if (getUser()==null){
            Logger.i("got packet from %s: %s",getDescription(),packet);
        }else{
            Logger.i("got packet from user \"%s\": %s",getUser().getName(), packet);
        }
        return packet;
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        if (getUser()==null){
            Logger.i("send packet for %s: %s",getDescription(),packet);
        }else{
            Logger.i("send packet for user \"%s\": %s",getUser().getName(),packet);
        }
        packetChatInterface.putPacketChat(packet);
    }

}
