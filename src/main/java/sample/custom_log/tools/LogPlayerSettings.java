package sample.custom_log.tools;

public class LogPlayerSettings {
    public final String domain;
    public final double rate;
    public final long startTime;
    public final long recordOrigin;

    public LogPlayerSettings(String domain, double rate, long startTime, long recordOrigin) {
        this.domain = domain;
        this.rate = rate;
        this.startTime = startTime;
        this.recordOrigin = recordOrigin;
    }
}
