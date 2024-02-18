import java.io.IOException;

public class UserCommand {
    public UserCommand(ServiceChat user,String command,String args) throws IOException{
        StringBuilder builder;
        switch (command){
            case "hello":
                user.getPacketInterface().sendMessage("hello from server");
                break;
            case "help":
                builder=new StringBuilder();
                builder.append("list of available commands:\n");
                builder.append("/hello - send hello message\n");
                builder.append("/help - print help menu\n");
                user.getPacketInterface().sendMessage(builder.toString());
                break;
            default:
                user.getPacketInterface().sendFormattedMessage("The command \"%s\" does not exist",command);
                break;
        }
    }
}
