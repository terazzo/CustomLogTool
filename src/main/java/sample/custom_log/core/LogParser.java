package sample.custom_log.core;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import sample.custom_log.core.FieldSplitter.FieldSplitterHandler;

/**
 * Apacheのログ文字列を読み込んでLogRecord化するためのクラス
 */
public class LogParser {
    private static final String DEFAULT_LOG_FORMAT = "%h %l %u %t \"%r\" %>s %b";
    private String logFormat;
    /** フィールドに分割するためのもの */
    private FieldSplitter fieldSplitter = new FieldSplitter();
    /** 各フィールドの値をLogRecordにセットする為のもの */
    private FieldHandler[] handlers;
    
    /**
     * デフォルトコンストラクタ。フォーマットはDEFAULT_FORMAT(Apacheのcommon)を使用する。
     */
    public LogParser() {
        this(DEFAULT_LOG_FORMAT);
    }
    
    /**
     * フォーマット文字列を指定するコンストラクタ。
     * logFormatにはApacheのLogFormatで指定する文字列を設定する。
     * @param logFormat フォーマット文字列(非null)
     */
    public LogParser(String logFormat) {
        if (logFormat == null) {
            throw new IllegalArgumentException("format is null.");
        }
        this.logFormat = logFormat;
        try {
            initializeHandlers();
        } catch (LogParseException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Illegal format specified: " + logFormat);
        }
    }

    /**
     * 一行分のログをパースし、内容を新規に生成したLogRecordオブジェクトに設定して戻す。
     * @param logLine 一行分のログを含む文字列
     * @return ログの情報がセットされたLogRecord
     * @throws LogParseException フォーマットの不整合等、パース時の例外
     */
    public LogRecord parseLine(String logLine) throws LogParseException {
        if (logLine == null) {
            throw new LogParseException("Null input specified.");
        }
        final LogRecord logRecord = new LogRecord();
        this.fieldSplitter.splitLine(logLine, new FieldSplitterHandler() {
            private int fieldCount = 0;
            public void handleFieldValue(String fieldValue) throws LogParseException {
                if (fieldCount < handlers.length) {
                    handlers[fieldCount++].setFieldValue(logRecord, fieldValue);
                } else {
                    throw new LogParseException("Too Many fields.　Over " + fieldCount);
                }
            }
        });
        return logRecord;
    }
    
    /**
     * this.formatの内容にあわせてhandlersを初期化する。
     * フォーマット文字列のフィールド数と出力ログのフィールド数は一致する事が期待出来るため、
     * 各フィールドを処理するFieldHandlerを配列で準備する事で、ログを処理する際にフィールドの
     * 位置によって対応する処理をおこなうことができる。(例: formatの先頭が「%h」の場合、
     * handlers[0]にリモートホストを処理するFieldHandlerを格納しておく事で、配列のインデッ
     * クスによって適切なFieldHandlerを取得出来る。)
     * 途中で例外が発生した場合、handlersの内容は中途半端に初期化されていることがある。
     * @throws LogParseException フォーマット文字列の読み込み時の例外
     */
    private void initializeHandlers() throws LogParseException {
        final List<String> fieldFormats = new ArrayList<String>();
        fieldSplitter.splitLine(logFormat, new FieldSplitterHandler() {
            public void handleFieldValue(String fieldValue) {
                fieldFormats.add(fieldValue);
            }
        });
        this.handlers = new FieldHandler[fieldFormats.size()];
        for (int index = 0; index < fieldFormats.size(); index++ ) {
            String fieldFormat = fieldFormats.get(index);
            int formatLength = fieldFormat.length();
            // %からはじまらない場合は例外
            if (formatLength < 2 || !fieldFormat.startsWith("%")) {
                throw new IllegalArgumentException("Illegal format string:" + fieldFormat);
            }
            // %Xや%{hoge}XのXにあたる文字を取得
            char type = fieldFormat.charAt(formatLength - 1);
            // %{hoge}Xのhogeにあたる文字を取得
            String param = fieldFormat.substring(1, formatLength - 1);
            if (param.length() >=2 &&
                    param.startsWith("{") && param.endsWith("}")) {
                param = param.substring(1, param.length() - 1);
            }
            // そのフィールド用のFieldHandlerを生成
            FieldHandler handler = createHandler(type, param);
            if (handler == null) {
                throw new IllegalArgumentException("Unsupported format: " + fieldFormat);
            }
            this.handlers[index] = handler;
        }
    }

    /**
     * フォーマット文字列の内容にしたがってFieldHandlerを生成して戻す
     * @param type 「%{hoge}X」のXにあたる文字
     * @param param 「%xX」のx、または「%{hoge}X」のhogeにあたる文字列。%Xのようにtypeのみの場合は空文字列。
     * @return 生成したFieldHandlerオブジェクト
     * @see <a href="http://httpd.apache.org/docs/2.2/en/mod/mod_log_config.html"
     * >mod_log_config - Apache HTTP Server</a>
     */
    private FieldHandler createHandler(final char type, final String param) {
        switch (type) {
            case 'h':   // Remote host
                return new FieldHandler() {
                    public void setFieldValue(LogRecord logRecord, String value) {
                        logRecord.setRemoteHost(value);
                    }};
            case 'l':   // Remote logname (from identd, if supplied). 
                return new FieldHandler() {
                    public void setFieldValue(LogRecord logRecord, String value) {
                        logRecord.setRemoteLogname(value);
                    }};
            case 'u':   // Remote user. 
                return new FieldHandler() {
                    public void setFieldValue(LogRecord logRecord, String value) {
                        logRecord.setRemoteUser(value);
                    }};
            case 't':   // Time the request was received
                // TODO: paramが空でない場合はstrftimeフォーマットとして使用
                final DateFormat formatter = 
                    new CLFDateFormat();
                return new FieldHandler() {
                    public void setFieldValue(LogRecord logRecord, String value) throws LogParseException {
                        try {
                            logRecord.setRequestTime(formatter.parse(value));
                        } catch (ParseException e) {
                            e.printStackTrace();
                            throw new LogParseException("Failed to parse requestTime:" + value);
                        }
                    }};
            case 'r':   // First line of request
                return new FieldHandler() {
                    public void setFieldValue(LogRecord logRecord, String value) {
                        logRecord.setRequestLine(value);
                    }};
            case 's':   // Status
                // この実装では%sと%>sの区別はつけない
                return new FieldHandler() {
                    public void setFieldValue(LogRecord logRecord, String value) throws LogParseException {
                        try {
                            logRecord.setStatus(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            throw new LogParseException("Failed to parse status:" + value);
                        }
                    }};
            case 'b':   // Size of response in bytes
                return new FieldHandler() {
                    public void setFieldValue(LogRecord logRecord, String value) throws LogParseException {
                        int responseSize = 0;
                        if (!value.equals("-")) {
                            try {
                                responseSize = Integer.parseInt(value);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                                throw new LogParseException("Failed to parse status:" + value);
                            }
                        }
                        logRecord.setResponseSize(responseSize);
                    }};
            case 'i':   // The contents of Foobar: header line(s) in the request sent to the server. 
                if (param.equalsIgnoreCase("referer")) {
                    return new FieldHandler() {
                        public void setFieldValue(LogRecord logRecord, String value) {
                           logRecord.setReferer(value);
                        }};
                } else if (param.equalsIgnoreCase("user-agent")) {
                    return new FieldHandler() {
                        public void setFieldValue(LogRecord logRecord, String value) {
                           logRecord.setUserAgent(value);
                        }};
                } else
                
                return new FieldHandler() {
                    public void setFieldValue(LogRecord logRecord, String value) {
                       logRecord.setRequestHeader(param, value);
                    }};
                    // 他の項目の対応も必要ならここに付け足す
            default: // 未対応のフォーマットについては、何もしないFieldHandlerを戻す
                return new FieldHandler() {
                    public void setFieldValue(LogRecord logRecord, String value) {
                }};
        }
    }
    public interface FieldHandler {
        /**
         * ログの各フィールドの値をlogRecordの該当する項目にセットする。
         * @param logRecord LogRecordオブジェクト
         * @param value フィールド値(文字列)
         * @throws LogParseException パース時にフォーマットの不一致などの例外が発生した場合、この例外を投げる事
         */
        void setFieldValue(LogRecord logRecord, String value) throws LogParseException;
    }
}