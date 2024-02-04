import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.StringTokenizer;

public class ClientManager extends SocketWorker{
    private PrintStream localPrintStream;
    private PrintStream sharedPrintStream;

    public ClientManager(Socket socket){
        this.socket=socket;
        WorkerManager.getInstance().registerAndStart(this);
    }

    public PrintStream getLocalPrintStream(){
        return localPrintStream;
    }

    public PrintStream getSharedPrintStream(){
        return sharedPrintStream;
    }

    public void run(){
        OutputStream output=null;

        try{
            output=socket.getOutputStream();
            MultipleOutputStream.getInstance().add(output);
            sharedPrintStream=new PrintStream(MultipleOutputStream.getInstance());
            localPrintStream=new PrintStream(output);
            BufferedReader input=new BufferedReader(new InputStreamReader(socket.getInputStream()));

            localPrintStream.println("Welcome to admin prompt (type /help to see available commands):");

            String line;
            while ((line=input.readLine())!=null){
                if (line.startsWith("/")){
                    StringTokenizer tokens=new StringTokenizer(line," ");

                    String command=tokens.nextToken().substring(1).toLowerCase();
                    String args=tokens.hasMoreTokens()?tokens.nextToken("").strip():"";

                    new AdminCommand(this, command, args);
                }
            }
        }catch(IOException e){}
        
        MultipleOutputStream.getInstance().remove(output);
        WorkerManager.getInstance().remove(this);
    }

    public String getDescription() {
        return String.format("manager client in %s", socket.getRemoteSocketAddress().toString());
    }
    
}
