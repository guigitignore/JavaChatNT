import java.io.IOException;
import java.util.StringTokenizer;

public class ClientCommand {
    public ClientCommand(ServiceTelnet client,String command,String args) throws IOException{
        StringTokenizer tokens;

        switch (command){
            case "exit":
                client.cancel();
                break;
            case "sendmsgto":
                tokens=new StringTokenizer(args," ");

                if (tokens.countTokens()<2){
                    client.getOutput().println("Syntax: /sendMsgTo <dest> <message>");
                }else{
                    String dest=tokens.nextToken();
                    String message=tokens.nextToken("");

                    client.getPacketInterface().sendMessage(message,dest);
                }
                break;
            case "sendmsgall":
                if (args.isEmpty()){
                    client.getOutput().println("Syntax: /sendMsgAll <message>");
                }else{
                    client.getPacketInterface().sendMessage(args);
                }
                break;
            case "listusers":
                client.getPacketInterface().sendListUserRequest();
                break;
            case "help":
                client.getOutput().println("list of available client commands:");
                client.getOutput().println("/exit - exit client");
                client.getOutput().println("/sendmsgto - send message to a specific user");
                client.getOutput().println("/sendmsgall - send message to all users");
                client.getOutput().println("/help - print help menu");
                //do not break to send help command to server side
            default:
                //server side command
                client.getPacketInterface().sendMessage("/"+command+" "+args);
        }
    }
}
