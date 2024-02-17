public class BasicCommand {
    public BasicCommand(ServiceChat user,String command,String args){
        switch (command){
            case "hello":
                try{
                    PacketChatFactory.createMessagePacket("[server]", "hello from server").send(user.getOutputStream());
                }catch(PacketChatException e){
                    Logger.w("Packet cannot be sent to "+user.getUser().getName());
                }
                break;
        }
    }
}
