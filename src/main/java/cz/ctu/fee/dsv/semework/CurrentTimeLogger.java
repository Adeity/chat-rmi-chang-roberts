package cz.ctu.fee.dsv.semework;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CurrentTimeLogger {
    public static void printTimeWithMessage(String message) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        System.out.println(dtf.format(now) + " " + message);
    }
}
