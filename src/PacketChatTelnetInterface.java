import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.stream.IntStream;

import javax.crypto.Cipher;

public class PacketChatTelnetInterface implements IPacketChatInterface,IUserConnection {
    private final static int USERNAME_STATE=0;
    private final static int CHALLENGE_STATE=1;
    private final static int CONNECTED_STATE=2;

    private final static String USERNAME_PROMPT="username: ";
    private final static String PASSWORD_PROMPT="password: ";

    private RSAKeyPair userRsaKeyPair=null;
    private byte[] challenge=null;
    private User user=null;

    private BufferedReader input;
    private PrintStream output;
    private int state=USERNAME_STATE;

    public PacketChatTelnetInterface(InputStream input,PrintStream output){
        this.input=new BufferedReader(new InputStreamReader(input));
        this.output=output;
        output.print(USERNAME_PROMPT);
    }
    
    public PacketChatTelnetInterface(Socket socket) throws IOException{
        this(socket.getInputStream(),new PrintStream(socket.getOutputStream()));
    }

    public PacketChatTelnetInterface() throws IOException{
        this(new InterruptibleInputStream(),System.out);
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
                    user=null;

                    output.print(USERNAME_PROMPT);
                }
                break;
            case PacketChat.CHALLENGE:
                state=CHALLENGE_STATE;
                if (packet.getFieldsNumber()>0){
                    challenge=packet.getField(0);
                }else{
                    output.print(PASSWORD_PROMPT);
                }
                synchronized(this){
                    notify();
                }
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

    private String readLine() throws PacketChatException{
        String line;
        try{
            line=input.readLine();
            if (line==null) throw new PacketChatException("null line");
        }catch(IOException e){
            throw new PacketChatException(e.getMessage());
        }
        return line;
    }

    private PacketChat autoCompleteChallenge() throws PacketChatException{
        PacketChat result=null;

        if (user!=null && userRsaKeyPair!=null){
            try{
                synchronized(this){
                    wait();
                }
            }catch(InterruptedException e){
                throw new PacketChatException("interruption");
            }
            if (challenge!=null){
                try{
                    Cipher cipher=Cipher.getInstance( "RSA/NONE/NoPadding", "BC" );
                    cipher.init(Cipher.DECRYPT_MODE, userRsaKeyPair.getPrivate());
                    result=PacketChatFactory.createChallengePacket(cipher.doFinal(challenge));
                }catch(Exception e){
                    Logger.w("cannot solve challenge");
                }
            }
            userRsaKeyPair=null;
        }
        return result;
    }

    public PacketChat getPacketChat() throws PacketChatException {
        String line;

        PacketChat packet=autoCompleteChallenge();

        while (packet==null){
            line=readLine();    
            if (line.isEmpty()) continue;
            
            switch(state){
                case USERNAME_STATE:
                    packet=createAuthPacket(line);
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
                        packet=PacketChatFactory.createMessagePacket(getUser().getName(), line);
                    }
                    break;
                default:
                    Logger.w("interface has entered in an uncontrolled state");
                    throw new PacketChatException("cannot handle this state");
            }
        }
        
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

                    packet=PacketChatFactory.createMessagePacket(getUser().getName(),message,dest);
                }
                break;
            case "sendmsgall":
                if (args.isEmpty()){
                    output.println("Syntax: /sendMsgAll <message>");
                }else{
                    packet=PacketChatFactory.createMessagePacket(getUser().getName(),args);
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
                packet=PacketChatFactory.createMessagePacket(getUser().getName(),"/"+command+" "+args);
        }
        
        return packet;
    }

    private PacketChat createAuthPacket(String username){
        PacketChat packet;

        try{
            userRsaKeyPair=RSAKeyPair.importKeyPair(username);
            user=new RSAUser(username, RSAEncoder.getInstance().encode(userRsaKeyPair.getPublic()));
            packet=PacketChatFactory.createLoginPacket(user.getName(), user.getKey());
        }catch(Exception e){
            Logger.w("cannot load user RSA key: %s. Falling back on password authentification",e.getMessage());
            user=new PasswordUser(username,"");
            packet=PacketChatFactory.createLoginPacket(user.getName());
        }

        return packet;
    }

    public User getUser() {
        User result;
        if (state==CONNECTED_STATE) result=this.user;
        else result=null;
        return result;
    }
}
