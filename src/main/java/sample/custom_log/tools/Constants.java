package sample.custom_log.tools;

public class Constants {
    public static final String CUSTOM_LOG_FORMAT_PROP_KEY = "custom_log.format";
    public static final String LOG_FORMAT =
        "%h %l %u %t \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\"";
    public static final String REQUEST_ENCODING_PROP_KEY = "custom_log.encoding";
    public static final String DEFAULT_ENCODING = "UTF-8";
    
}
