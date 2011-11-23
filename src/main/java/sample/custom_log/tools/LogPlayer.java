package sample.custom_log.tools;

import static sample.custom_log.tools.Constants.CUSTOM_LOG_FORMAT_PROP_KEY;
import static sample.custom_log.tools.Constants.LOG_FORMAT;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sample.custom_log.core.LogParseException;
import sample.custom_log.core.LogParser;
import sample.custom_log.core.LogRecord;
import sample.custom_log.util.NoncancelableTask;

/** Apacheのアクセスログを再生するクラス */
public class LogPlayer {
    private static final String DEFAULT_HOST = "localhost";
    private static final String STDIN_NAME = "-";
    private static final int THREAD_COUNT = 30;
    private final String path;
    private final String domain;
    private final double rate;
    private final String format;
    private LogPlayerSettings settings;
    private Log logger = LogFactory.getLog(LogPlayer.class);

    public LogPlayer(String path, String domain, double rate) {
        this.path = path;
        this.domain = domain;
        this.rate = rate;
        
        format = System.getProperty(CUSTOM_LOG_FORMAT_PROP_KEY, LOG_FORMAT);
    }
    // 設定されたファイル、ドメイン、速度でリクエストを再生する。
    public void play() {
        logger.debug("start playing: " + path);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        try {
            InputStream in = path.equals(STDIN_NAME) ? System.in : new FileInputStream(path);
            playWith(in, executor);
        } catch (IOException e) {
            logger.warn("Error occurs while processing file:" + path + " : " + e.getMessage(), e);
        }
        executor.shutdown();
        awaitTermination(executor);
        logger.debug("complete");
    }
    // ストリームの内容を読みだしてリクエストを生成してexecutorを使用して投げる
    private void playWith(InputStream in, ExecutorService executor) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        LogParser parser = new LogParser(format);
        String line;
        while ((line = reader.readLine()) != null) {
            try {
                final LogRecord logRecord = parser.parseLine(line);
                if (logRecord.getRequestTime() == null) {
                    continue;
                }
                if (settings == null) {
                    prepareSettings(logRecord.getRequestTime());
                }
                executor.execute(new LogRecordPlayer(logRecord, settings));
            } catch (LogParseException e) {
                logger.warn("Parse Error in file:" + path + " : " + e.getMessage(), e);
            }
        }
    }
    private synchronized void prepareSettings(Date requestTime) {
        if (this.settings != null)  {
            return;
        }
        Date startDate = new Date();
        this.settings = new LogPlayerSettings(domain, rate,
                startDate.getTime(), requestTime.getTime());
    }
    // すべてのタスクが終わるまで永久に待ち続ける
    private static void awaitTermination(final ExecutorService executor) {
        new NoncancelableTask() {
            @Override
            protected boolean endure(long millis) throws InterruptedException {
                return executor.awaitTermination(millis, TimeUnit.MILLISECONDS);
            }
        }.runWithoutLimit(300 * 1000L);
    }

    private static Option buildOption(String opt, boolean hasArg, String argName, boolean required, String description) {
        Option option = new Option(opt, hasArg, description);
        option.setArgName(argName);
        option.setRequired(required);
        return option;
    }
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(buildOption("d", true, "domain name", false, "リクエストを送信するドメイン名(省略時はlocalhost)"));
        options.addOption(buildOption("r", true, "rate", false, "速度指定。倍速なら0.5を指定する。(省略時は1)"));
        options.addOption(buildOption("f", true, "file name", true, "ログファイル指定。(\"-\"指定時は標準入力を使用)"));
        CommandLineParser parser = new BasicParser();
        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(options, args, false);
        } catch (ParseException e) {
        }
        if (commandLine == null) {
            HelpFormatter help = new HelpFormatter();
            help.setWidth(Integer.MAX_VALUE);
            help.printHelp("java " + LogPlayer.class.getName(), options, true);
            return;
        }
        String path = commandLine.getOptionValue("f");
        String domain = commandLine.getOptionValue("d", DEFAULT_HOST);
        double rate = 1;
        try {
            rate = Double.parseDouble(commandLine.getOptionValue("r", "1"));
        } catch(NumberFormatException e) {
            
        }
        LogPlayer logPlayer = new LogPlayer(path, domain, rate);
        logPlayer.play();
    }
}