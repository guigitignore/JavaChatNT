public class UserCommand {
    public UserCommand(ServiceChat user,String command,String args) throws PacketChatException{
        StringBuilder builder;
        switch (command){
            case "hello":
                user.getOutput().sendMessage("hello from server");
                break;
            case "help":
                builder=new StringBuilder();
                builder.append("list of available commands:\n");
                builder.append("/hello - send hello message\n");
                builder.append("/help - print help menu\n");
                user.getOutput().sendMessage(builder.toString());
                break;
            default:
                user.getOutput().sendFormattedMessage("The command \"%s\" does not exist",command);
                break;
        }
    }
}
