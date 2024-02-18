import java.io.IOException;

public class ClientCommand {
    public ClientCommand(ServiceTelnet client,String command,String args) throws IOException{
        switch (command){
            case "exit":
                client.cancel();
                break;
            default:
                //server side command
                client.getPacketInterface().sendMessage("/"+command+" "+args);
        }
    }
}
