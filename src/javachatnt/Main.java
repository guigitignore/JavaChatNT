package javachatnt;

import java.security.Security;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.AbstractMap.SimpleEntry;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import client.ClientChat;
import server.ServerChat;
import user.User;
import util.Logger;
import util.RSAKeyPair;
import worker.WorkerManager;

class Main{
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
                serverMode(modeArgs);
                break;
            case "generate":
                generatorMode(modeArgs);
                break;
            case "connect":
                clientMode(modeArgs);
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
            Logger.close();
        }));
    }

    public static void serverMode(String... args){
        ArrayList<SimpleEntry<Integer,String[]>> servers=new ArrayList<>();

        for (String arg:args){
            if (arg.isEmpty()) continue;
            StringTokenizer tokens=new StringTokenizer(arg,":");

            try{
                int port=Integer.parseInt(tokens.nextToken());
                if (port<=0) throw new NumberFormatException();

                if (tokens.hasMoreTokens()){
                    String[] userTypes=tokens.nextToken("").split(",");
                    servers.add(new SimpleEntry<Integer,String[]>(port,userTypes));
                }else{
                    servers.add(new SimpleEntry<Integer,String[]>(port,new String[]{User.USER_TAG}));
                }
            }catch(NumberFormatException e){
                Logger.w("Invalid port number in argument: \"%s\"",arg);
                continue;
            } 
        }

        if (servers.size()>0){
            if (!Logger.addOutput("server")) Logger.e("Cannot write server logs in file");
            Logger.i("Starting engine...");

            for (SimpleEntry<Integer,String[]> server:servers){
                new ServerChat(server.getKey(),server.getValue());
            }
            
        }else{
            Logger.e("Nothing to do");
            System.exit(-1);
        }
    }

    public static void generatorMode(String...args){
        if (args.length==0){
            Logger.e("Expected username as argument");
            System.exit(-1);
        }

        for (String username:args){
            try{
                RSAKeyPair keyPair=new RSAKeyPair();
                Logger.i("Sucessfully generate RSA keypair");
                keyPair.exportKeyPair(username);
                Logger.i("Sucessfully export keypair");  
            }catch(Exception e){
                Logger.e("An error occured suring generation process for user %s: %s",username,e.getMessage());
            }
        }
        System.exit(0);
    }

    public static void clientMode(String...args){
        String host=null;
        int port=0;

        try{
            if (args.length==1){
                host="localhost";
                port=Integer.parseInt(args[0]);
            }else if (args.length>=2){
                host=args[0];
                port=Integer.parseInt(args[1]);
            }else{
                throw new NoSuchFieldException();
            }

            if (port<=0) throw new NumberFormatException("port must be greater than 0");

            if (Logger.addOutput("client")){
                Logger.removeSTDOUT();
            }else{
                Logger.e("Cannot write client logs in file: falling back on standard output");
            }
                
            try{
                new ClientChat(host, port);
            }catch(Exception e){
                Logger.e("client error: %s",e.getMessage());
                System.exit(-1);
            }
        }catch(NumberFormatException e){
            Logger.e("Invalid port number!");
            System.exit(-1);
        }catch(NoSuchFieldException e){
            Logger.e("Expecting at least one argument");
            System.exit(-1);
        }
            
    }
}