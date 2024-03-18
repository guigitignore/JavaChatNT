package user;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import util.Logger;

public class UserDatabase{

    private HashMap<String,User> users=new HashMap<>();
    private String filename;

    public UserDatabase(String filename){
        this.filename=filename;
        
        try{
            BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            Decoder decoder=Base64.getDecoder();
            String line;
            int successCounter=0;
            int failureCounter=0;
            
            while ((line=reader.readLine())!=null){
                StringTokenizer tokens=new StringTokenizer(line,":");
                int tokenNumber=tokens.countTokens();
                if (tokenNumber!=4){
                    failureCounter++;
                    continue;
                }

                String user=new String(decoder.decode(tokens.nextToken()));
                String type=tokens.nextToken().toUpperCase();
                byte[] key=decoder.decode(tokens.nextToken());
                String tag=tokens.nextToken().toUpperCase();

                try{
                    switch(type){
                        case "PASSWORD":
                            users.put(user,new PasswordUser(user, key,tag));
                            break;
                        case "RSA":
                            users.put(user, new RSAUser(user,key,tag));
                            break;
                    }
                    successCounter++;
                }catch(Exception e){
                    failureCounter++;
                }
                
            }
            Logger.i("Successfully import %d users (failure %d)",successCounter,failureCounter);
            reader.close();
        }catch(IOException e){
            Logger.i("Initializing empty database");
        }

    }

    public User getUser(String name){
        return users.get(name);
    }

    public boolean addUser(User user){
        boolean status;
        status=users.putIfAbsent(user.getName(), user)==null;
        if (status) export();
        return status;
    }

    public boolean removeUser(User user){
        boolean status;
        status=users.remove(user.getName(), user);
        if (status) export();
        return status;
    }

    public void export(){
        try{
            Encoder encoder=Base64.getEncoder();
            PrintStream writer=new PrintStream(new FileOutputStream(filename));

            for (User user:users.values()){
                StringBuilder builder=new StringBuilder();
                byte[] name=user.getName().getBytes();
                builder.append(new String(encoder.encode(name)));
                builder.append(":");
                builder.append(user.getTypeName());
                builder.append(":");
                byte[] key=user.getKey();
                builder.append(new String(encoder.encode(key)));
                builder.append(":");
                builder.append(user.getTag());
                writer.println(builder.toString());
            }

            writer.close();
        }catch(IOException e){
            Logger.e("Cannot save database");
        }  
    }
}