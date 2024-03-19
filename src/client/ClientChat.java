package client;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javacard.IJavacardInterface;
import packetchat.IPacketChatInterface;
import packetchat.PacketChat;
import packetchat.PacketChatBucket;
import packetchat.PacketChatException;
import packetchat.PacketChatFactory;
import shared.PacketChatRawInterface;
import shared.PacketChatTelnetInterface;
import user.IUserConnection;
import user.User;
import util.InterruptibleInputStream;
import util.Logger;
import util.NounceManager;
import worker.SocketWorker;
import worker.WorkerManager;

public class ClientChat extends SocketWorker implements IPacketChatInterface,IUserConnection{
    public final static int BUCKET_CAPACITY=100;
    public final static String CLIENT_NAME="[ClientChat]";

    private IPacketChatInterface packetInterface;
    private IPacketChatInterface messageInterface;

    private PacketChatBucket bucket=null;

    private ClientChatInput clientInput;
    private ClientChatOutput clientOutput;

    private ClientChatRequest requestManager;

    private NounceManager incomingFiles;
    private NounceManager outgoingFiles;

    private IJavacardInterface cardInterface;

    public ClientChat(IJavacardInterface cardInterface,String host,int port) throws Exception{
        super(new Socket(host, port),cardInterface);
    }

    public String getDescription() {
        return String.format("ClientChat -> ServerChat %s", getSocket().getRemoteSocketAddress().toString());
    }

    public IPacketChatInterface getMessageInterface(){
        return messageInterface;
    }

    public PacketChat getPacketChat() throws PacketChatException {
        PacketChat packet=packetInterface.getPacketChat();
        Logger.i("got packet: %s",packet.toString());
        return packet;
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        packetInterface.putPacketChat(packet);
        Logger.i("send packet: %s",packet.toString());
    }

    public User getUser() {
        return clientInput.isConnected()?clientOutput.getUser():null;
    }

    public PacketChatBucket getBucket(){
        return bucket;
    }

    public ClientChatInput getInput(){
        return clientInput;
    }

    public ClientChatOutput getOutput(){
        return clientOutput;
    }

    public ClientChatRequest getRequestManager(){
        return requestManager;
    }

    public NounceManager getIncomingFiles(){
        return incomingFiles;
    }

    public NounceManager getOutgoingFiles(){
        return outgoingFiles;
    }

    public IJavacardInterface getCardInterface(){
        return cardInterface;
    }

    private void skipWelcomeMessage(InputStream in) throws IOException{
        while(true){
            int available=in.available();
            if (available>0){
                in.skip(available);
                break;
            }
            try{
                Thread.sleep(10);
            }catch(InterruptedException e){
                throw new IOException(e.getMessage());
            }
        }
    }

    private void sendHelloPacket(OutputStream out) throws IOException{
        try{
            PacketChatFactory.createHelloPacket().send(out);
        }catch(PacketChatException e){
            throw new IOException(e.getMessage());
        }
    }

    private void initStreams() throws IOException{
        InputStream in=getSocket().getInputStream();
        OutputStream out=getSocket().getOutputStream();
        InputStream stdin=System.getProperty("java.version").startsWith("1.")?System.in:new InterruptibleInputStream();

        skipWelcomeMessage(in);
        sendHelloPacket(out);

        packetInterface=new PacketChatRawInterface(in,out);
        messageInterface=new PacketChatTelnetInterface(stdin,System.out);

        bucket=new PacketChatBucket(BUCKET_CAPACITY);
        incomingFiles=new NounceManager();
        outgoingFiles=new NounceManager();

        clientInput=new ClientChatInput(this);
        clientOutput=new ClientChatOutput(this);

        requestManager=new ClientChatRequest(this);
    }

    private void mainloop() throws IOException{
        PacketChat packet;

        while (true){
            try{
                packet=getPacketChat();
            }catch(PacketChatException e){
                break;
            }
            try{
                bucket.putPacketChat(packet);
            }catch(PacketChatException e){
                Logger.w("Cannot put packet in bucket: %s",e.getMessage());
            }         
        }
    }

    private void closeCardInterface(){
        try{
            cardInterface.close();
            Logger.i("Card interface sucessfully closed");
        }catch(Exception e){
            Logger.e("Cannot close card interface");
        }
    }

    public void run(){
        cardInterface=(IJavacardInterface)getArgs()[0];

        try{
            initStreams();
            mainloop();
        }catch(IOException e){
            Logger.e("IO error: %s",e.getMessage());
        }

        closeCardInterface();
        WorkerManager.getInstance().remove(this);
        WorkerManager.getInstance().cancelAll();
             
    }
}
