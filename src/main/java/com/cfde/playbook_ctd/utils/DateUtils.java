package com.cfde.playbook_ctd.utils;

import com.cfde.playbook_ctd.utils.constants.Constants;
import org.apache.log4j.Logger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static Logger logger = Logger.getLogger(DateUtils.class);

    public static Date stringToDateParser(String dateStr){
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.ENGLISH);
            Date date = df.parse(dateStr);
            return date;
        }catch(Exception e){
            logger.error(StackTracePrinter.printStackTrace(e));
            return null;
        }
    }

    public static String dateToStringParser(Date date, String timePrintPattern){
        try {
            DateFormat df = new SimpleDateFormat(timePrintPattern);
            String strDate = df.format(date);
            return strDate;
        }catch(Exception e){
            logger.error(StackTracePrinter.printStackTrace(e));
            return null;
        }
    }

    public static String localDateToStringParser(LocalDateTime localDate){
        try {
            LocalDateTime localDateRef = LocalDateTime.now();//For reference
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss", Locale.ENGLISH);
            return localDateRef.format(formatter);
        }catch(Exception e){
            logger.error(e);
            return null;
        }
    }

    public static LocalDateTime stringToLocalDateParserParser(String dateStr){
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            return LocalDateTime.parse(dateStr, formatter);
        }catch(Exception e){
            logger.error(e);
            return null;
        }
    }

    public static Date addDates(String startDateStr, String period){
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.ENGLISH);
            Calendar c = Calendar.getInstance();
            c.setTime(df.parse(startDateStr));
            if(period.equals(Constants.LICENCE_TYPE_10_days)){
                c.add(Calendar.DATE, 10);
            }else if(period.equals(Constants.LICENCE_TYPE_1_MONTH)){
                c.add(Calendar.MONTH, 1);
            }else if(period.equals(Constants.LICENCE_TYPE_3_MONTH)){
                c.add(Calendar.MONTH, 3);
            }else if(period.equals(Constants.LICENCE_TYPE_6_MONTH)){
                c.add(Calendar.MONTH, 6);
            }else if(period.equals(Constants.LICENCE_TYPE_YEAR)){
                c.add(Calendar.YEAR, 1);
            }else{
                logger.error("Unknown Licence type: "+startDateStr);
            }

            return c.getTime();
        }catch(Exception e){
            logger.error(StackTracePrinter.printStackTrace(e));
            return null;
        }
    }

    public static Date addDates(String startDateStr, int periodType, int period){
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.ENGLISH);
            Calendar c = Calendar.getInstance();
            c.setTime(df.parse(startDateStr));
            c.add(periodType, period);
            return c.getTime();
        }catch(Exception e){
            logger.error(StackTracePrinter.printStackTrace(e));
            return null;
        }
    }

    public static String getTheTimeFromMillis(long timeInMilliSeconds){
        long seconds = timeInMilliSeconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        String timespanRemaining = "";
        if(days >= 1){
            if(days > 1){
                timespanRemaining = timespanRemaining + days + " days ";
            }else{
                timespanRemaining = timespanRemaining + days + " day ";
            }
        }
        if(hours >= 1){
            if(hours > 1){
                timespanRemaining = timespanRemaining + (hours % 24) + " hours ";
            }else{
                timespanRemaining = timespanRemaining + (hours % 24) + " hour ";
            }
        }
        if(minutes >= 1){
            if(minutes > 1){
                timespanRemaining = timespanRemaining + (minutes % 60) + " minutes ";
            }else{
                timespanRemaining = timespanRemaining + (minutes % 60) + " minute ";
            }
        }
        return timespanRemaining;
    }
}
