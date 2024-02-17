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
        input=new BufferedReader(new InputStreamReader(getSocket().getInputStream()));
        output=new PrintStream(getSocket().getOutputStream());

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

    public boolean sendLogin(String username,String password){
        boolean status;
        PacketChat packet;
        
        try{
            PacketChatFactory.createLoginPacket(username).send(upstreamOutput);
            packet=new PacketChat(upstreamInput);

            Logger.i("CHALLENGE  "+packet.toString());

            if (packet.getCommand()!=PacketChat.CHALLENGE){
                throw new PacketChatException("Expected challenge packet as response");
            }
            

            PacketChatFactory.createChallengePacket(password.getBytes()).send(upstreamOutput);
            packet=new PacketChat(upstreamInput);

            Logger.i("AUTH "+packet.toString());

            if (packet.getCommand()!=PacketChat.AUTH){
                throw new PacketChatException("Expected auth packet as response");
            }

            switch (packet.getStatus()) {
                case PacketChat.STATUS_SUCCESS:
                    status=true;
                    break;
                case PacketChat.STATUS_ERROR:
                    //the server can send error message
                    int fieldsNumber=packet.getFieldsNumber();
                    if (fieldsNumber>0){
                        for (int i=0;i<fieldsNumber;i++){
                            output.println("Error: "+new String(packet.getField(i)));
                        }
                    }else{
                        output.println("Please retry");
                    }
                    status=false;
                    break;

                default:
                    throw new PacketChatException("Unknown auth status");
            }
            

        }catch(PacketChatException e){
            status=false;
        }
        return status;
    }

    private void mainLoop() throws IOException{
        String line;
        while (!getSocket().isClosed() && (line=input.readLine())!=null){
            if (line.startsWith("/")){
                StringTokenizer tokens=new StringTokenizer(line," ");

                String command=tokens.nextToken().substring(1).toLowerCase();
                String args=tokens.hasMoreTokens()?tokens.nextToken("").strip():"";

                new ClientCommand(this, command, args);
            }else{
                if (!line.isEmpty()) sendMessage(line);
            }
            
        }
    }

    public void login() throws IOException{
        boolean status=false;

        while(!status){
            output.print("username: ");
            String username=input.readLine();
            output.print("password:");
            String password=input.readLine();
            if (username==null || password==null) throw new IOException("null field");
            status=sendLogin(username, password);   
        }
    }

    public void run(){
        try{
            
            initStreams();
            login();
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
        return String.format("service telnet in %s", getSocket().getRemoteSocketAddress().toString());
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
