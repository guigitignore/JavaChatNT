import java.io.IOException;
import java.util.StringTokenizer;

public class AdminCommand {
    private void sendTaggedMessage(String tag,String format,Object...args) throws IOException{
        String message=String.format(format,args);

        for (ServiceChat user:ServerChatManager.getInstance().getConnectedUsers()){
            if (user.getUser().getTag().equals(tag)){
                user.getPacketInterface().sendMessage(message);
            }
        }
    }

    private void sendAdminMessage(String message,Object...args) throws IOException{
        sendTaggedMessage(User.ADMIN_TAG, message, args);
    }

    public AdminCommand(ServiceChat client,String command,String args) throws IOException{
        StringBuilder builder;
        int i;

        switch (command){
            case "ps":
                builder=new StringBuilder();
                builder.append("Processes running on the server:\n");
                i=0;
                for (IWorker worker:WorkerManager.getInstance().getWorkers()){
                    builder.append(String.format("%d - %s - %b\n", i,worker.getDescription(),worker.getStatus()));
                    i++;
                }
                client.getPacketInterface().sendMessage(builder.toString());
                break;
            case "kill":
                try{
                    int task=Integer.parseInt(args);
                    WorkerManager.getInstance().cancel(task);
                }catch(NumberFormatException e){}
                break;
            case "shutdown":
                sendAdminMessage("%s shutdown the server...",client.getUser().getName());
                WorkerManager.getInstance().cancelAll();
                break;
            case "wall":
                sendAdminMessage(args);
                break;
            case "kick":
                ServiceChat user=ServerChatManager.getInstance().getConnectedUser(args);
                if (user==null){
                    client.getPacketInterface().sendFormattedMessage("user \"%s\" does not seem to be connected",args);
                }else{
                    user.cancel();
                    client.getPacketInterface().sendFormattedMessage("user \"%s\" has been kicked",args);
                }
                break;
            case "ls":
                builder=new StringBuilder();
                builder.append("Currently connected users:\n");
                i=1;
                for (ServiceChat u:ServerChatManager.getInstance().getConnectedUsers()){
                    builder.append(String.format("%d - %s - %s - %s\n", i,u.getUser().getName(),u.getUser().getTypeName(),u.getUser().getTag()));
                    i++;
                }
                client.getPacketInterface().sendMessage(builder.toString());
                break;
            case "adduser":
                StringTokenizer tokens=new StringTokenizer(args," ");
                if (tokens.countTokens()!=2){
                    client.getPacketInterface().sendMessage("Expected 2 arguments");
                }
                String username=tokens.nextToken();
                String password=tokens.nextToken();

                ServerChatManager.getInstance().getDataBase().addUser(new PasswordUser(username, password));
                client.getPacketInterface().sendMessage("The command has been executed");
                break;
            case "help":
                builder=new StringBuilder();
                builder.append("list of available commands:\n");
                builder.append("/ps - list running tasks on the server\n");
                builder.append("/kill - kill a task by its index\n");
                builder.append("/shutdown - shutdown the server\n");
                builder.append("/wall - send message to admin\n");
                builder.append("/kick - kick a user\n");
                builder.append("/ls - advanced listing of currently connected users\n");
                builder.append("/adduser - add a user\n");
                builder.append("/help - print help menu\n");

                client.getPacketInterface().sendMessage(builder.toString());
                break;
            default:
                client.getPacketInterface().sendFormattedMessage("The command \"%s\" does not exist",command);
                break;
        }
    }
}
