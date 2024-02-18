import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private final static SimpleDateFormat DATE_FORMAT=new SimpleDateFormat("dd/MM/yy - HH:mm:ss");

    private static String getCallerName(){
        return Thread.currentThread().getStackTrace()[4].getClassName();
    }

    private static String getTime(){
        return DATE_FORMAT.format(new Date());
    }

    private static void log(String level,String message,Object...args){
        System.out.println(getTime()+" ["+ level+"] (" + getCallerName()+") "+ String.format(message, args));
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
}
