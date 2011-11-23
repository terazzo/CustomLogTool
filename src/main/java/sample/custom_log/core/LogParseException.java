package sample.custom_log.core;

public class LogParseException extends Exception {
    /** シリアルバージョン番号 */
    private static final long serialVersionUID = -3318103984912270740L;

    public LogParseException(String message) {
        super(message);
    }
}