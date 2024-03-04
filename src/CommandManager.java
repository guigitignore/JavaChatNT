import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiFunction;

public class CommandManager {
    
    private static CommandManager instance=new CommandManager();

    public static CommandManager getInstance(){
        return instance;
    }

    private HashMap<String,ArrayList<String>> descriptions;
    private HashMap<String,ArrayList<BiFunction<ServiceChat,String,String>>> commands;

    private CommandManager(){
        descriptions=new HashMap<>();
        commands=new HashMap<>();
    }

    public void addCommandAction(String command,BiFunction<ServiceChat,String,String> action){
        commands.putIfAbsent(command, new ArrayList<>());
        commands.get(command).add(action);
    }

    public void addCommandDescription(String command,String description){
        descriptions.putIfAbsent(command, new ArrayList<>());
        descriptions.get(command).add(description);
    }

    public String getCommandDescription(String command){
        String result=null;
        ArrayList<String> descriptionsList=descriptions.get(command);

        if (descriptionsList!=null){
            result=String.join("\n", descriptionsList);
        }
        
        return result;
    }
}
