package sample.custom_log.core;

/**
 * LogFormat書式およびCustomLog出力結果をパースするクラス。
 * スレッドセーフではない！
 */
public class FieldSplitter {
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    /** エスケープ文字(「\\」) */
    private static final char ESCAPE_CHAR = '\\';
    /** セパレータ(半角空白) */
    private static final char SEPARATOR = ' ';
    /** 引用符(「"」) */
    private static final char QUOTE = '\"';
    /** 括弧開始(「[」) */
    private static final char DATE_OPENNER = '[';
    /** 括弧終了(「]」) */
    private static final char DATE_CLOSER = ']';
    /** thisが共用するStringBuilder */
    private StringBuilder buffer = new StringBuilder(DEFAULT_BUFFER_SIZE);
    public FieldSplitter() {
        
    }
    /**
     * Apacheログの一件分(一行)からフィールドを読み込み、順次fieldSplitterHandlerのhandleFieldValue()を呼び出す。
     * 
     * @param lineString 一行分の文字列
     * @param fieldSplitterHandler 読み取ったフィールドを処理するFieldSplitterHandler
     * @throws LogParseException 引用符が閉じていない等、フォーマットがおかしい場合の例外
     */
    public void splitLine(String lineString, FieldSplitterHandler fieldSplitterHandler)
            throws LogParseException {
        int length = lineString.length();
        Mode mode = Mode.NORMAL;
        boolean isEscaping = false;
        buffer.setLength(0);
        int pos = 0;
        int start = 0;
        for (pos = 0; pos < length; pos++) {
            char c = lineString.charAt(pos);
            if (isEscaping) {
                isEscaping = false;
                start = pos;
            } else if (c == ESCAPE_CHAR) {
                isEscaping = true;
                if (pos - start > 0) {
                    buffer.append(lineString, start, pos);
                }
                start = pos + 1;
            } else {
                if (mode == Mode.NORMAL) {
                    if (c == SEPARATOR) {
                        if (pos > start) {
                            String value = fullFieldValue(buffer, lineString, start, pos);
                            fieldSplitterHandler.handleFieldValue(value);
                        }
                        start = pos + 1;
                    } else if (c == QUOTE) {
                        mode = Mode.QUOTING;
                        start = pos + 1;
                    } else if (c == DATE_OPENNER) {
                        mode = Mode.IN_DATE_PART;
                        start = pos + 1;
                    }
                } else if (mode == Mode.QUOTING) {
                    if (c == QUOTE) {
                        mode = Mode.NORMAL;
                        String value = fullFieldValue(buffer, lineString, start, pos);
                        fieldSplitterHandler.handleFieldValue(value);
                        start = pos + 1;
                    }
                } else if (mode == Mode.IN_DATE_PART) {
                    if (c == DATE_CLOSER) {
                        mode = Mode.NORMAL;
                        String value = fullFieldValue(buffer, lineString, start, pos);
                        fieldSplitterHandler.handleFieldValue(value);
                        start = pos + 1;
                    }
                }
            }
        }
        if (mode != Mode.NORMAL) {
            throw new LogParseException("Unbalance spchar.");
        }
        if (pos > start) {
            String value = fullFieldValue(buffer, lineString, start, pos);
            fieldSplitterHandler.handleFieldValue(value);
        }
    }
    /**
     * bufferの内容およびstartからposまでの間の文字列を連結したものを戻す。
     */
    private static String fullFieldValue(
            StringBuilder buffer, String lineString, int start, int pos) {
        String value;
        if (buffer.length() > 0) {
            if (pos - start > 0) {
                buffer.append(lineString, start, pos);
            }
            value = buffer.toString();
            buffer.setLength(0);
        } else {
            value = lineString.substring(start, pos);
        }
        return value;
    }
    private enum Mode {
        /** 通常 */
        NORMAL,
        /** 引用符内 */
        QUOTING,
        /** 日付括弧([])内 */
        IN_DATE_PART
    }
    
    public interface FieldSplitterHandler {
        void handleFieldValue(String fieldValue) throws LogParseException;
    }
}
