import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.stream.IntStream;

public class PacketChatTelnetInterface implements IPacketChatInterface {
    private final static int USERNAME_STATE=0;
    private final static int CHALLENGE_STATE=1;
    private final static int CONNECTED_STATE=2;

    private final static String USERNAME_PROMPT="username: ";
    private final static String PASSWORD_PROMPT="password: ";
    private final static String SENDER="user";


    private BufferedReader input;
    private PrintStream output;
    private int state=USERNAME_STATE;

    public PacketChatTelnetInterface(BufferedReader input,PrintStream output){
        this.input=input;
        this.output=output;
        output.print(USERNAME_PROMPT);
    }
    
    public PacketChatTelnetInterface(Socket socket) throws IOException{
        this(new BufferedReader(new InputStreamReader(socket.getInputStream())),new PrintStream(socket.getOutputStream()));
    }

    public PacketChatTelnetInterface() throws IOException{
        this(new BufferedReader(new InputStreamReader(System.in)),System.out);
    }

    public void putPacketChat(PacketChat packet) throws PacketChatException {
        switch (packet.getCommand()){
            case PacketChat.AUTH:
                if (packet.getStatus()==PacketChat.STATUS_SUCCESS){
                    state=CONNECTED_STATE;
                    output.println("Connection sucess");
                    output.println("Welcome to the server");
                }else{
                    state=USERNAME_STATE;
                    if (packet.getFieldsNumber()==1){
                        output.println(new String(packet.getField(0)));
                    }else{
                        output.println("Connection failure.");
                    }
                    output.print(USERNAME_PROMPT);
                }
                break;
            case PacketChat.CHALLENGE:
                state=CHALLENGE_STATE;
                output.print(PASSWORD_PROMPT);
                break;

            case PacketChat.SEND_MSG:
                String sender=new String(packet.getField(0));
                String message=new String(packet.getField(1));
                int fieldsNumber=packet.getFieldsNumber();

                if (fieldsNumber>2){
                    String dest=String.join(",",IntStream.range(2, fieldsNumber).mapToObj(index -> {
                        return new String(packet.getField(index));
                    }).toList());
                    output.printf("%s->%s %s\n",sender,dest,message);
                }else{
                    output.printf("%s %s\n",sender,message);
                }
                break;
            
            case PacketChat.LIST_USERS:
                output.println("List of connected users:");
                for (byte[] user:packet.getFields()){
                    output.println("-"+new String(user));
                }
                break;

            default:
                Logger.w("Unhandled packet type: %d",packet.getCommand());
                break;
        }
    }

    public PacketChat getPacketChat() throws PacketChatException {
        PacketChat packet=null;
        String line;

        do{
            try{
                line=input.readLine();
                if (line==null) throw new IOException("line is null");

                if (line.isEmpty()) continue;
            }catch(IOException e){
                throw new PacketChatException(e.getMessage());
            }
            switch(state){
                case USERNAME_STATE:
                    packet=PacketChatFactory.createLoginPacket(line);
                    break;
                case CHALLENGE_STATE:
                    packet=PacketChatFactory.createChallengePacket(line.getBytes());
                    break;
                case CONNECTED_STATE:
                    if (line.startsWith("/")){
                        StringTokenizer tokens=new StringTokenizer(line," ");
                        String command=tokens.nextToken().substring(1).toLowerCase();
                        String args=tokens.hasMoreTokens()?tokens.nextToken("").strip():"";
                        packet=handleClientCommand(command, args);
                    }else{
                        packet=PacketChatFactory.createMessagePacket(SENDER, line);
                    }
                    break;
                default:
                    Logger.w("interface has entered in an uncontrolled state");
                    throw new PacketChatException("cannot handle this state");
            }
        }while(packet==null);
        

        return packet;
    }

    private PacketChat handleClientCommand(String command,String args) throws PacketChatException{
        PacketChat packet=null;
        StringTokenizer tokens;

        switch (command){
            case "exit":
                throw new PacketChatException("exit requested");
            case "sendmsgto":
                tokens=new StringTokenizer(args," ");

                if (tokens.countTokens()<2){
                    output.println("Syntax: /sendMsgTo <dest> <message>");
                }else{
                    String dest=tokens.nextToken();
                    String message=tokens.nextToken("");

                    packet=PacketChatFactory.createMessagePacket(SENDER,message,dest);
                }
                break;
            case "sendmsgall":
                if (args.isEmpty()){
                    output.println("Syntax: /sendMsgAll <message>");
                }else{
                    packet=PacketChatFactory.createMessagePacket(SENDER,args);
                }
                break;
            case "listusers":
                packet=PacketChatFactory.createListUserPacket();
                break;
            case "help":
                output.println("list of available client commands:");
                output.println("/exit - exit client");
                output.println("/sendmsgto - send message to a specific user");
                output.println("/sendmsgall - send message to all users");
                output.println("/help - print help menu");
                output.println();
                //do not break to send help command to server side
            default:
                //server side command
                packet=PacketChatFactory.createMessagePacket(SENDER,"/"+command+" "+args);
        }
        
        return packet;
    }
}
