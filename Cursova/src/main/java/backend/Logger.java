package backend;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void info(String message) {
        System.out.println(getTimestamp() + " [INFO] " + message);
    }

    public static void error(String message, Exception e) {
        System.err.println(getTimestamp() + " [ERROR] " + message);
        if (e != null) {
            e.printStackTrace();
        }
    }

    private static String getTimestamp() {
        return formatter.format(new Date());
    }
}
