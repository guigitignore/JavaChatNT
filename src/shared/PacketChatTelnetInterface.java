package shared;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import packetchat.IPacketChatInterface;
import packetchat.PacketChat;
import packetchat.PacketChatException;
import packetchat.PacketChatFactory;
import util.Logger;

public class PacketChatTelnetInterface implements IPacketChatInterface {
    private final static int USERNAME_STATE=0;
    private final static int CHALLENGE_STATE=1;
    private final static int CONNECTED_STATE=2;
    private final static String SENDER="CLIENT";

    private final static String USERNAME_PROMPT="username: ";
    private final static String PASSWORD_PROMPT="password: ";

    private BufferedReader input;
    private PrintStream output;
    private int state=USERNAME_STATE;

    public PacketChatTelnetInterface(InputStream input,PrintStream output){
        this.input=new BufferedReader(new InputStreamReader(input));
        this.output=output;
        output.print(USERNAME_PROMPT);
    }

    public synchronized void putPacketChat(PacketChat packet) throws PacketChatException {
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
                    }).collect(Collectors.toList()));
                    output.printf("%s->%s %s\n",sender,dest,message);
                }else{
                    output.printf("%s %s\n",sender,message);
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


    public PacketChat getPacketChat() throws PacketChatException {
        String line;
        PacketChat packet;

        do{
            line=readLine();    
        }while(line.isEmpty()); 
            
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
                    String args=tokens.hasMoreTokens()?tokens.nextToken("").trim():"";
                    packet=handleClientCommand(command, args);
                }else{
                    packet=PacketChatFactory.createMessagePacket(SENDER, line);
                }
                break;
            default:
                Logger.w("interface has entered in an uncontrolled state");
                throw new PacketChatException("cannot handle this state");
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
