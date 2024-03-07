import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

class Main{
    public static int DEFAULT_SERVER_PORT=2000;
    public static void main(String[] args) {
        if (args.length<1){
            System.err.println("You must specify the mode in argument: server,connect,generate");
            System.exit(-1);
        }

        String mode=args[0];
        String[] modeArgs = new String[args.length - 1];
        System.arraycopy(args, 1, modeArgs, 0, args.length - 1);

        Security.addProvider(new BouncyCastleProvider());
        
        //switch mode
        switch(mode){
            case "serve":
                server(modeArgs);
                break;
            case "generate":
                generator(modeArgs);
                break;
            case "connect":
                client(modeArgs);
                break;
            default:
                System.err.printf("Unknown mode \"%s\"\n",mode);
                System.exit(-1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            Logger.w("Trigger engine interruption...");
            WorkerManager.getInstance().cancelAll();
            //wait 100ms
            try{
                Thread.sleep(100); //let time to logger to send messages to output
            }catch(InterruptedException e){}
            
        }));
    }

    public static void server(String... args){
        Logger.i("Starting engine...");
        new ServerChat(DEFAULT_SERVER_PORT,User.USER_TAG);
        new ServerChat(DEFAULT_SERVER_PORT+1,User.ADMIN_TAG);

    }

    public static void generator(String...args){
        if (args.length==0){
            System.err.println("Expected username as argument");
            System.exit(-1);
        }

        for (String username:args){
            try{
                new KeyGenerator(username);
            }catch(Exception e){
                Logger.e("Cannot generate keypair for user %s",username);
            }
        }
    }

    public static void client(String...args){
        String host;
        int port;

        try{
            if (args.length<2){
                host="localhost";
                port=(args.length<1)?DEFAULT_SERVER_PORT:Integer.parseInt(args[0]);
            }else{
                host=args[0];
                port=Integer.parseInt(args[1]);
            }
            if (port<=0) throw new NumberFormatException("port must be greater than 0");

            try{
                new ClientChat(host, port);
            }catch(Exception e){
                Logger.e("client error: %s",e.getMessage());
                System.exit(-1);
            }
        }catch(NumberFormatException e){
            System.err.println("Invalid port number!");
            System.exit(-1);
        }
            
    }
}