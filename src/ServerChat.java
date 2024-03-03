import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class ServerChat extends ServerSocketWorker{
    
    private String[] tags;
    private ArrayList<ServiceChat> clients=new ArrayList<>();

    public ServerChat(int port,String...allowedTags){
        super(port,(Object[])allowedTags);
    }

    public ServerChat(int port){
        this(port,User.USER_TAG);
    }

    public String getDescription(){
        return "ServerChat on port "+getPort();
    }

    public String[] getTags(){
        return tags;
    }

    public void run(){
        super.run();
        this.tags=(String[])getArgs();

        if (getServer()!=null){
            Logger.i(String.format("Server Chat listening on port %d...", getPort()));

            while (true){
                try{
                    Socket clientSocket=getServer().accept();
                    ServiceChat client=new ServiceChat(clientSocket,this);
                    synchronized(clients){
                        clients.add(client);
                    }
                }catch(IOException e){
                    break;
                }
                
            }
        }
        killClients();
        WorkerManager.getInstance().remove(this);
    }

    public boolean remove(ServiceChat client){
        synchronized(clients){
            return clients.remove(client);
        }
    }

    public ServiceChat[] getClients(){
        synchronized(clients){
            return clients.toArray(new ServiceChat[clients.size()]);
        }
    }

    private void killClients(){
        for (ServiceChat client:getClients()){
            Logger.i("canceling %s",client.getUser().getName());
            client.cancel();
        }
    }
        
}

