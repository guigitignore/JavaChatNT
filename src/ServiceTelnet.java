import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.StringTokenizer;

public class ServiceTelnet extends SocketWorker {
    private BufferedReader input;
    private PrintStream output;

    private PacketChatInterface packetInterface=null;

    private ServerTelnet server;

    public ServiceTelnet(Socket socket,ServerTelnet server) {
        super(socket,server);
    }

    private void initStreams() throws IOException{
        input=new BufferedReader(new InputStreamReader(getSocket().getInputStream()));
        output=new PrintStream(getSocket().getOutputStream());

        Socket upstreamSocket=new Socket(server.getUpstreamHost(),server.getUpstreamPort());
        packetInterface=new PacketChatInterface(upstreamSocket);

    }

    public BufferedReader getInput(){
        return input;
    }

    public PacketChatInterface getPacketInterface(){
        return packetInterface;
    }

    public PrintStream getOutput(){
        return output;
    }

    public ServerTelnet getServer(){
        return server;
    }

    public boolean sendLogin(String username,String password) throws IOException{
        boolean status=false;
        PacketChat packet;
        
        packetInterface.sendUsername(username);
        packet=packetInterface.getPacket();

        Logger.i("CHALLENGE  "+packet.toString());

        if (packet.getCommand()==PacketChat.CHALLENGE){
            packetInterface.sendPassword(password);
            packet=packetInterface.getPacket();

            Logger.i("AUTH "+packet.toString());

            if (packet.getCommand()==PacketChat.AUTH){
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
    
                }
            }
            
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
                if (!line.isEmpty()) packetInterface.sendMessage(line);
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
        this.server=(ServerTelnet)getArgs()[0];

        try{
            
            initStreams();
            login();
            new ServiceTelnetPacketHandler(this);
            mainLoop();


        }catch(IOException e){}

        closeUpstreamSocket();

        WorkerManager.getInstance().remove(this);
    }

    public String getDescription() {
        return String.format("service telnet in %s", getSocket().getRemoteSocketAddress().toString());
    }


    private void closeUpstreamSocket(){
        if (packetInterface!=null){
            try{
                packetInterface.close();
            }catch(IOException e){}
        }
    }

    public void cancel(){
        super.cancel();
        closeUpstreamSocket();
    }
}
