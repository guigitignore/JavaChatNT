import java.io.IOException;
import java.util.Base64;
import java.util.StringTokenizer;

public class AdminCommand {
    private ServiceChat client;

    private void psCommand() throws IOException{
        StringBuilder builder=new StringBuilder();
        builder.append("Processes running on the server:\n");
        int i=0;
        for (IWorker worker:WorkerManager.getInstance().getWorkers()){
            builder.append(String.format("%d - %s - %b\n", i,worker.getDescription(),worker.getStatus()));
            i++;
        }
        client.getPacketInterface().sendMessage(builder.toString());
    }

    private void killCommand(String args) throws IOException{
        try{
            int task=Integer.parseInt(args);
            WorkerManager.getInstance().cancel(task);
        }catch(NumberFormatException e){}
    }

    private void shutdownCommand() throws IOException{
        ServerChatManager.getInstance().sendAdminMessage("%s shutdown the server...",client.getUser().getName());
        WorkerManager.getInstance().cancelAll();
    }

    private void wallCommand(String args) throws IOException{
        ServerChatManager.getInstance().sendAdminMessage(args);
    }

    private void kickCommand(String args) throws IOException{
        ServiceChat user=ServerChatManager.getInstance().getConnectedUser(args);
        if (user==null){
            client.getPacketInterface().sendFormattedMessage("user \"%s\" does not seem to be connected",args);
        }else{
            user.cancel();
            client.getPacketInterface().sendFormattedMessage("user \"%s\" has been kicked",args);
        }
    }

    private void lsCommand() throws IOException{
        StringBuilder builder=new StringBuilder();
        builder.append("Currently connected users:\n");
        int i=1;
        for (ServiceChat u:ServerChatManager.getInstance().getUsers()){
            builder.append(String.format("%d - %s - %s - %s\n", i,u.getUser().getName(),u.getUser().getTypeName(),u.getUser().getTag()));
            i++;
        }
        client.getPacketInterface().sendMessage(builder.toString());
    }

    private void addpwduserCommand(String args) throws IOException{
        StringTokenizer tokens=new StringTokenizer(args," ");
        if (tokens.countTokens()!=2){
            client.getPacketInterface().sendMessage("Expected 2 arguments");
        }else{
            String username=tokens.nextToken();
            String password=tokens.nextToken();

            if (ServerChatManager.getInstance().getDataBase().addUser(new PasswordUser(username, password))){
                client.getPacketInterface().sendFormattedMessage("Successfully add user %s",username);
            }else{
                client.getPacketInterface().sendFormattedMessage("Failed to add user %s",username);
            }
        }
    }

    private void addrsauserCommand(String args) throws IOException{
        User user;
        StringTokenizer tokens=new StringTokenizer(args," ");
        if (tokens.countTokens()!=2){
            client.getPacketInterface().sendMessage("Expected 2 arguments");
        }else{
            String username=tokens.nextToken();
            String pubKey=tokens.nextToken();

            try{
                user=new RSAUser(username, Base64.getDecoder().decode(pubKey));
                if (!ServerChatManager.getInstance().getDataBase().addUser(user)){
                    throw new Exception();
                }
                client.getPacketInterface().sendFormattedMessage("Successfully add user %s",username);
            }catch(Exception e){
                client.getPacketInterface().sendFormattedMessage("Failed to add user %s",username);
            }
        }
    }

    private void helpCommand() throws IOException{
        StringBuilder builder=new StringBuilder();
        builder.append("list of available commands:\n");
        builder.append("/ps - list running tasks on the server\n");
        builder.append("/kill - kill a task by its index\n");
        builder.append("/shutdown - shutdown the server\n");
        builder.append("/wall - send message to admin\n");
        builder.append("/kick - kick a user\n");
        builder.append("/ls - advanced listing of currently connected users\n");
        builder.append("/addpwduser - add a password user\n");
        builder.append("/help - print help menu\n");

        client.getPacketInterface().sendMessage(builder.toString());
    }


    public AdminCommand(ServiceChat client,String command,String args) throws IOException{
        this.client=client;

        switch (command){
            case "ps":
                psCommand();
                break;
            case "kill":
                killCommand(args);
                break;
            case "shutdown":
                shutdownCommand();
                break;
            case "wall":
                wallCommand(args);
                break;
            case "kick":
                kickCommand(args);
                break;
            case "ls":
                lsCommand();
                break;
            case "addpwduser":
                addpwduserCommand(args);
                break;
            case "addrsauser":
                addrsauserCommand(args);
                break;
            case "help":
                helpCommand();
                break;
            default:
                client.getPacketInterface().sendFormattedMessage("The command \"%s\" does not exist",command);
                break;
        }
    }
}
