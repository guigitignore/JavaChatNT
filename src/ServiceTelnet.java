import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.StringTokenizer;

public class ServiceTelnet extends SocketWorker {
    private BufferedReader input;
    private PrintStream output;

    private IPacketChatInput upstreamInput;
    private PacketChatOutput upstreamOutput;
    private Socket upstreamSocket;

    private ServerTelnet server;

    public ServiceTelnet(Socket socket,ServerTelnet server) {
        super(socket,server);
    }

    @SuppressWarnings("resource")
    private void initStreams() throws IOException{
        input=new BufferedReader(new InputStreamReader(getSocket().getInputStream()));
        output=new PrintStream(getSocket().getOutputStream());

        upstreamSocket=new Socket(server.getUpstreamHost(),server.getUpstreamPort());

        upstreamInput=new InputStreamPacketChat(upstreamSocket.getInputStream());
        upstreamOutput=new PacketChatOutput(new OutputStreamPacketChat(upstreamSocket.getOutputStream()));

    }

    public BufferedReader getInput(){
        return input;
    }

    public PrintStream getOutput(){
        return output;
    }

    public ServerTelnet getServer(){
        return server;
    }

    public IPacketChatInput getUpstreamInput(){
        return upstreamInput;
    }

    public PacketChatOutput getUpstreamOutput(){
        return upstreamOutput;
    }

    public boolean sendLogin(String username,String password) throws PacketChatException{
        boolean status=false;
        PacketChat packet;
        
        upstreamOutput.sendUsername(username);
        packet=upstreamInput.getPacketChat();

        Logger.i("CHALLENGE  "+packet.toString());

        if (packet.getCommand()==PacketChat.CHALLENGE){
            if (packet.getFieldsNumber()==0){
                upstreamOutput.sendPassword(password);
                packet=upstreamInput.getPacketChat();

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
            }else{
                output.println("RSA users are not supported from telnet");
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

                try{
                    new ClientCommand(this, command, args);
                }catch(PacketChatException e){
                    throw new IOException(String.format("Cannot execute user command: %s",e.getMessage() ));
                }
                
            }else{
                try{
                    if (!line.isEmpty()) upstreamOutput.sendMessage(line);
                }catch(PacketChatException e){
                    throw new IOException(String.format("Cannot send user message: %s",e.getMessage() ));
                }
                
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
            try{
                status=sendLogin(username, password);   
            }catch(PacketChatException e){
                throw new IOException(String.format("Cannot send login for username %s", username));
            }
        }
    }

    public void run(){
        this.server=(ServerTelnet)getArgs()[0];

        try{
            
            initStreams();
            login();
            new ServiceTelnetListener(this);
            mainLoop();


        }catch(IOException e){}

        closeUpstreamSocket();

        WorkerManager.getInstance().remove(this);
    }

    public String getDescription() {
        return String.format("ServiceTelnet in %s", getSocket().getRemoteSocketAddress().toString());
    }


    private void closeUpstreamSocket(){
        try{
            upstreamSocket.close();
        }catch(IOException e){}
    }

    public void cancel(){
        super.cancel();
        closeUpstreamSocket();
    }
}
