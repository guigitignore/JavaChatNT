package util;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

public class Logger {
    private final static SimpleDateFormat DATE_FORMAT=new SimpleDateFormat("dd/MM/yy - HH:mm:ss");
    private final static SimpleDateFormat LOG_FORMAT=new SimpleDateFormat("dd-MM-yy_HH-mm-ss");
    private static HashSet<PrintStream> outputs;

    static{
        outputs=new HashSet<>();
        //add stdout by default
        outputs.add(System.out);    
    }

    private static String getCallerName(){
        return Thread.currentThread().getStackTrace()[4].getClassName();
    }

    private static String getTime(){
        return DATE_FORMAT.format(new Date());
    }

    private static void log(String level,String message,Object...args){
        String logLine=getTime()+" ["+ level+"] (" + getCallerName()+") "+ String.format(message, args);

        for (PrintStream output:outputs){
            output.println(logLine);
        } 
    }

    public static void i(String format,Object...args){
        log("INFO", format,args);
    }

    public static void w(String format,Object...args){
        log("WARNING", format,args);
    }

    public static void e(String format,Object...args){
        log("ERROR", format,args);
    }

    public static void close(){
        for (PrintStream output:outputs){
            output.close();
        }
    }

    public static void addOutput(PrintStream output){
        outputs.add(output);
    }

    public static boolean addOutput(String name){
        boolean result;

        File file = new File(String.format("logs/%s_%s.log",name,LOG_FORMAT.format(new Date())));
        try{
            file.getParentFile().mkdirs(); 
            addOutput(new PrintStream(new FileOutputStream(file,false)));
            result=true;
        }catch(IOException e){
            result=false;
        }
        return result;
    }

    public static void removeOutput(PrintStream output){
        outputs.remove(output);
    }

    public static void removeSTDOUT(){
        removeOutput(System.out);
    }
}
