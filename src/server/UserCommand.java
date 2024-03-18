package server;
import packetchat.PacketChatException;

public class UserCommand {
    public UserCommand(ServiceChat user,String command,String args) throws PacketChatException{
        StringBuilder builder;
        switch (command){
            case "hello":
                user.getOutput().sendMessage("hello from server");
                break;
            case "listusers":
                builder=new StringBuilder();
                builder.append("List of connected users:\n");
                for (ServiceChat client:ServerChatManager.getInstance().getClients()){
                    builder.append(String.format("- %s - %s \n",client.getUser().getName(),client.getClientType().name()));
                }
                user.getOutput().sendMessage(builder.toString());
                break;
            case "whoami":
                user.getOutput().sendFormattedMessage("username=\"%s\" account_type=%s client_type=%s",user.getUser().getName(),user.getUser().getTypeName(),user.getClientType().name());
                break;
            case "help":
                builder=new StringBuilder();
                builder.append("list of available commands:\n");
                builder.append("/hello - send hello message\n");
                builder.append("/listusers - list connected users\n");
                builder.append("/help - print help menu\n");
                user.getOutput().sendMessage(builder.toString());
                break;
            default:
                user.getOutput().sendFormattedMessage("The command \"%s\" does not exist",command);
                break;
        }
    }
}
