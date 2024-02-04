import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class LocalClientManager extends SocketWorker{
    private BufferedReader input=null;

    public String getDescription() {
        return "local server manager";
    }

    public void run() {
        try{
            socket=new Socket("localhost",ServerManager.SERVER_MANAGER_PORT);
            PrintStream secondaryOutput=new PrintStream(socket.getOutputStream());
            BufferedReader secondaryInput=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            input=new BufferedReader(new InputStreamReader(System.in));

            new Thread(()->{
                try{
                    String line;
                    while ((line=secondaryInput.readLine())!=null){
                        System.out.println(line);
                    }
                }catch(IOException e){
                    System.out.println("OutputStream is closed!");
                }
            }).start();

            String line;
            while (!socket.isClosed() && (line=input.readLine())!=null){
                secondaryOutput.println(line);
                //special exit to avoid System.in lock
                if (line.strip().equals("/exit")) break;
            }

        }catch(IOException e){
            System.err.println("Cannot initialize local manager");
        }
    }

}
