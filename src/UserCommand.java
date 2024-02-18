import java.io.IOException;

public class UserCommand {
    public UserCommand(ServiceChat user,String command,String args) throws IOException{
        switch (command){
            case "hello":
                user.getPacketInterface().sendMessage("hello from server");
                break;
        }
    }
}
