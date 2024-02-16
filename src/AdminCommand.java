import java.util.StringTokenizer;

public class AdminCommand {
    public AdminCommand(ClientManager manager,String command,String args){
        switch (command){
            case "ps":
                int i=0;
                for (IWorker worker:WorkerManager.getInstance().getWorkers()){
                    manager.getLocalPrintStream().printf("%d - %s - %b\n", i,worker.getDescription(),worker.getStatus());
                    i++;
                }
                break;
            case "kill":
                try{
                    int task=Integer.parseInt(args);
                    WorkerManager.getInstance().cancel(task);
                }catch(NumberFormatException e){}
                break;
            case "exit":
                WorkerManager.getInstance().cancelAll();
                break;
            case "wall":
                manager.getSharedPrintStream().println(args);
                break;
            case "adduser":
                StringTokenizer tokens=new StringTokenizer(args," ");
                if (tokens.countTokens()!=2){
                    manager.getLocalPrintStream().println("Expected 2 arguments");
                }
                String username=tokens.nextToken();
                String password=tokens.nextToken();

                ServerChatManager.getInstance().getDataBase().addUser(new PasswordUser(username, password));
                manager.getLocalPrintStream().println("The command has been executed");
                break;
        }
    }
}
