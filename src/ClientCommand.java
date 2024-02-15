public class ClientCommand {
    public ClientCommand(ServiceTelnet client,String command,String args){
        switch (command){
            case "exit":
                client.cancel();
                break;
            default:
                //server side command
                client.broadcastMessage(args);
        }
    }
}
