package com.github.cloudgyb.jerry.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author geng
 * @since 2025/02/14 13:53:49
 */
public class DateUtil {
    private static final String DATE_RFC5322 = "EEE, dd MMM yyyy HH:mm:ss z";

    public static String getDateRFC5322(long timeMillis) {
        Date date = new Date(timeMillis);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_RFC5322);
        return simpleDateFormat.format(date);
    }

    public static void main(String[] args) {
        String dateRFC5322 = getDateRFC5322(System.currentTimeMillis());
        System.out.println(dateRFC5322);
    }
}
