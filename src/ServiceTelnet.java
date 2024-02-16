import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.StringTokenizer;

public class ServiceTelnet extends SocketWorker {
    private BufferedReader input;
    private PrintStream output;
    private InputStream upstreamInput;
    private OutputStream upstreamOutput;
    private Socket upstreamSocket;

    public ServiceTelnet(Socket socket) {
        super(socket);
    }

    private void initStreams() throws IOException{
        input=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output=new PrintStream(socket.getOutputStream());

        upstreamSocket=new Socket("localhost",ServerChat.SERVER_CHAT_PORT);
        upstreamInput=upstreamSocket.getInputStream();
        upstreamOutput=upstreamSocket.getOutputStream();
    }

    public BufferedReader getInputStream(){
        return input;
    }

    public PrintStream getOutputStream(){
        return output;
    }

    public void sendMessage(String message,String... dests){
        PacketChat outgoing=PacketChatFactory.createMessagePacket("inconito", message,dests);
        try{
            outgoing.send(upstreamOutput);
        }catch(PacketChatException e){
            output.println("Cannot send message");
        }
    }

    private void mainLoop() throws IOException{
        String line;
        while (!socket.isClosed() && (line=input.readLine())!=null){
            if (line.startsWith("/")){
                StringTokenizer tokens=new StringTokenizer(line," ");

                String command=tokens.nextToken().substring(1).toLowerCase();
                String args=tokens.hasMoreTokens()?tokens.nextToken("").strip():"";

                new ClientCommand(this, command, args);
            }else{
                sendMessage(line);
            }
            
        }
    }

    public void run(){
        try{
            
            initStreams();
            new ServerTelnetPacketHandler(this);
            mainLoop();


        }catch(IOException e){}

        closeUpstreamSocket();

        WorkerManager.getInstance().remove(this);
    }

    public InputStream getUpstreamInput(){
        return upstreamInput;
    }

    public PrintStream getOutput(){
        return output;
    }

    public String getDescription() {
        return String.format("service telnet in %s", socket.getRemoteSocketAddress().toString());
    }


    private void closeUpstreamSocket(){
        if (upstreamSocket!=null && !upstreamSocket.isClosed()){
            try{
                upstreamSocket.close();
            }catch(IOException e){}
        }
    }

    public void cancel(){
        super.cancel();
        closeUpstreamSocket();
    }
}
