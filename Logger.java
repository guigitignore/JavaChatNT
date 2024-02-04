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

    private static void log(String level,String message){
        System.out.println(getTime()+" ["+ level+"] (" + getCallerName()+") "+ message);
    }

    public static void i(String info){
        log("INFO", info);
    }

    public static void w(String warning){
        log("WARNING", warning);
    }

    public static void e(String error){
        log("ERROR", error);
    }
}
