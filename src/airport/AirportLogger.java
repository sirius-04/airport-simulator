package airport;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AirportLogger {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    public static synchronized void log(String component, String message) {
        String timestamp = sdf.format(new Date());
        System.out.println("[" + timestamp + "] [" + component + "] " + message);
    }
}
