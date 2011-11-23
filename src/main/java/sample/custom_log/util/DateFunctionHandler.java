package sample.custom_log.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.josql.functions.AbstractFunctionHandler;

/** 日付変換を行う為のFunctionHandler。JoSQLで使用する。  */
public class DateFunctionHandler extends AbstractFunctionHandler {
    public String to_char(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }
}
