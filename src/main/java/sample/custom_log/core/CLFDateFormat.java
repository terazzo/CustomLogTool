package sample.custom_log.core;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.NotImplementedException;

/** 
 * CLFのパースに特化したDateFormat。
 * スレッドセーフではない！
 */
public class CLFDateFormat extends DateFormat {
    private static final String DEFAULT_DATE_FORMAT = "dd/MMM/yyyy:HH:mm:ss Z";
    private static final long serialVersionUID = 4124093846593771052L;
    private Map<String, Long>dayTimes = new TreeMap<String, Long>();
    final DateFormat formatter = new SimpleDateFormat(DEFAULT_DATE_FORMAT, Locale.ENGLISH);
    public CLFDateFormat() {
        
    }
    @Override
    public StringBuffer format(Date date, StringBuffer stringbuffer,
            FieldPosition fieldposition) {
        throw new NotImplementedException();
    }

    @Override
    public Date parse(String s, ParsePosition parseposition) {
        int pos = parseposition.getIndex();
        String dayString = s.substring(pos, pos + 12);
        String zoneString = s.substring(pos + 21, pos + 26);
        long dayTime;
        try {
            dayTime = getDayTime(dayString, zoneString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        // dd/MMM/yyyy:HH:mm:ss Z
        // 19/Dec/2008:09:03:24 +0900
        char hourH = s.charAt(pos + 12);
        char hourL = s.charAt(pos + 13);
        char minH = s.charAt(pos + 15);
        char minL = s.charAt(pos + 16);
        char secH = s.charAt(pos + 18);
        char secL = s.charAt(pos + 19);
        dayTime = dayTime + 
            (((((hourH - '0') * 10 + (hourL - '0')) * 60)
                + ((minH - '0') * 10 + (minL - '0'))) * 60
                    + ((secH - '0') * 10 + (secL - '0'))) * 1000;
        parseposition.setIndex(pos + 26);
        Date d = new Date(dayTime);
        return d;
    }
    private long getDayTime(String dayString, String zoneString) throws ParseException {
        Long daytime = dayTimes.get(dayString);
        if (daytime == null) {
            String dateString = dayString + "00:00:00 " + zoneString;
            daytime = formatter.parse(dateString).getTime();
            dayTimes.put(dayString, daytime);
        }
        return daytime.longValue();
    }
}