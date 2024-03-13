import java.util.Base64;
import java.util.StringTokenizer;

public class AdminCommand {
    private ServiceChat client;

    private void psCommand() throws PacketChatException{
        StringBuilder builder=new StringBuilder();
        builder.append("Processes running on the server:\n");
        int i=0;
        for (IWorker worker:WorkerManager.getInstance().getWorkers()){
            builder.append(String.format("%d - %s - %b\n", i,worker.getDescription(),worker.getStatus()));
            i++;
        }
        client.getOutput().sendMessage(ServiceChat.SERVER_NAME,builder.toString());
    }

    private void killCommand(String args) throws PacketChatException{
        try{
            int task=Integer.parseInt(args);
            WorkerManager.getInstance().cancel(task);
        }catch(NumberFormatException e){}
    }

    private void shutdownCommand() throws PacketChatException{
        String message=String.format("\"%s\" shutdown the server...",client.getUser().getName());
        
        Logger.w(message);
        ServerChatManager.getInstance().getClientsByTag(User.ADMIN_TAG).getOutput().sendMessage(ServiceChat.SERVER_NAME,message);
        WorkerManager.getInstance().cancelAll();
    }

    private void wallCommand(String args) throws PacketChatException{
        ServerChatManager.getInstance().getClientsByTag(User.ADMIN_TAG).getOutput().sendFormattedMessage(ServiceChat.SERVER_NAME,args);
    }

    private void kickCommand(String args) throws PacketChatException{
        ServiceChat user=ServerChatManager.getInstance().getConnectedUser(args);
        if (user==null){
            client.getOutput().sendFormattedMessage("user \"%s\" does not seem to be connected",args);
        }else{
            user.cancel();
            client.getOutput().sendFormattedMessage("user \"%s\" has been kicked",args);
        }
    }

    private void lsCommand() throws PacketChatException{
        StringBuilder builder=new StringBuilder();
        builder.append("Currently connected users:\n");
        ServerChatManager.getInstance().getClients().stream().map(ServiceChat::getUser).forEach(user->{
            builder.append(String.format("- %s / %s / %s\n",user.getName(),user.getTypeName(),user.getTag()));
        });
        client.getOutput().sendMessage(ServiceChat.SERVER_NAME,builder.toString());
    }

    private void addpwduserCommand(String args) throws PacketChatException{
        StringTokenizer tokens=new StringTokenizer(args," ");
        if (tokens.countTokens()!=2){
            client.getOutput().sendMessage(ServiceChat.SERVER_NAME,"Expected 2 arguments");
        }else{
            String username=tokens.nextToken();
            String password=tokens.nextToken();

            if (ServerChatManager.getInstance().getDataBase().addUser(new PasswordUser(username, password))){
                client.getOutput().sendFormattedMessage("Successfully add user %s",username);
            }else{
                client.getOutput().sendFormattedMessage("Failed to add user %s",username);
            }
        }
    }

    private void addrsauserCommand(String args) throws PacketChatException{
        User user;
        StringTokenizer tokens=new StringTokenizer(args," ");
        if (tokens.countTokens()!=2){
            client.getOutput().sendMessage(ServiceChat.SERVER_NAME,"Expected 2 arguments");
        }else{
            String username=tokens.nextToken();
            String pubKey=tokens.nextToken();

            try{
                user=new RSAUser(username, Base64.getDecoder().decode(pubKey));
                if (!ServerChatManager.getInstance().getDataBase().addUser(user)){
                    throw new Exception();
                }
                client.getOutput().sendFormattedMessage("Successfully add user %s",username);
            }catch(Exception e){
                client.getOutput().sendFormattedMessage("Failed to add user %s",username);
            }
        }
    }

    private void asCommand(String args) throws PacketChatException{
        StringTokenizer tokens=new StringTokenizer(args," ");

        if (tokens.countTokens()<2){
            client.getOutput().sendMessage(ServiceChat.SERVER_NAME,"This command expected 2 arguments");
        }else{
            String username=tokens.nextToken();
            //get the remaining args
            String message=tokens.nextToken("").substring(1);
            Logger.i("admin \"%s\" has sent a message as user \"%s\": \"%s\"",client.getUser().getName(),username,message);

            ServiceChat target=ServerChatManager.getInstance().getClient(username);
            if (target==null){
                client.getOutput().sendFormattedMessage("The user \"%s\" does not seem to be connected",username);
            }else{
                target.getInput().sendMessage(ServiceChat.SERVER_NAME,message);
            }
        }
    }

    private void serverCommand(String args) throws PacketChatException{
        if (args.isEmpty()) return;
        ServerChatManager.getInstance().getClients().getOutput().sendMessage(ServiceChat.SERVER_NAME,args);
    }

    private void helpCommand() throws PacketChatException{
        StringBuilder builder=new StringBuilder();
        builder.append("list of available commands:\n");
        builder.append("/ps - list running tasks on the server\n");
        builder.append("/kill - kill a task by its index\n");
        builder.append("/shutdown - shutdown the server\n");
        builder.append("/wall - send message to admin\n");
        builder.append("/kick - kick a user\n");
        builder.append("/ls - advanced listing of currently connected users\n");
        builder.append("/addpwduser - add a password user\n");
        builder.append("/addrsauser - add a rsa user\n");
        builder.append("/as - spoof user identity\n");
        builder.append("/server - send message as server\n");
        builder.append("/help - print help menu\n");

        client.getOutput().sendMessage(ServiceChat.SERVER_NAME,builder.toString());
    }


    public AdminCommand(ServiceChat client,String command,String args) throws PacketChatException{
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
            case "as":
                asCommand(args);
                break;
            case "server":
                serverCommand(args);
                break;
            case "help":
                helpCommand();
                break;
            default:
                client.getOutput().sendFormattedMessage("The command \"%s\" does not exist",command);
                break;
        }
    }
}
