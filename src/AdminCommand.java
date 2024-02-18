import java.io.IOException;
import java.util.StringTokenizer;

public class AdminCommand {
    public AdminCommand(ServiceChat client,String command,String args) throws IOException{
        switch (command){
            case "ps":
                StringBuilder builder=new StringBuilder();
                builder.append("Processes running on the server:\n");
                int i=0;
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
                WorkerManager.getInstance().cancelAll();
                break;
            case "wall":
                for (ServiceChat user:ServerChatManager.getInstance().getConnectedUsers()){
                    if (user.getUser().getTag().equals(User.ADMIN_TAG)){
                        user.getPacketInterface().sendMessage(args);
                    }
                }
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
        }
    }
}
