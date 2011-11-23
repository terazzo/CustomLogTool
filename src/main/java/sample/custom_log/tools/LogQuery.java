package sample.custom_log.tools;

import static sample.custom_log.tools.Constants.CUSTOM_LOG_FORMAT_PROP_KEY;
import static sample.custom_log.tools.Constants.LOG_FORMAT;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.josql.Query;
import org.josql.QueryExecutionException;
import org.josql.QueryParseException;
import org.josql.QueryResults;

import sample.custom_log.core.LogParseException;
import sample.custom_log.core.LogParser;
import sample.custom_log.core.LogRecord;
import sample.custom_log.util.DateFunctionHandler;

/** JoSQLを使用してApacheのアクセスログを処理するクラス */
public class LogQuery {
    private String[] paths;
    private Query query = new Query();
    private QueryResults queryResults;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Error: Too few args.");
            System.err.println("Usage: java " + LogQuery.class +
                    " <query string> <filpath> [<filepath> ...]");
            System.exit(-1);
        }
        String query = args[0];
        String[] paths = new String[args.length - 1];
        System.arraycopy(args, 1, paths, 0, paths.length);
        
        LogQuery logQuery;
        try {
            logQuery = new LogQuery(query, paths);
            logQuery.execute();
            logQuery.printResults();
        } catch (QueryParseException e) {
            e.printStackTrace();
        } catch (QueryExecutionException e) {
            e.printStackTrace();
        }
    }
    /**
     * @param queryString クエリ文字列
     * @param paths 読み込むログのパス
     */
    public LogQuery(String queryString, String[] paths) throws QueryParseException {
        if (queryString == null) {
            throw new IllegalArgumentException("queryString is null.");
        }
        if (paths == null || paths.length == 0) {
            throw new IllegalArgumentException("paths is empty.");
        }
        this.query.addFunctionHandler(new DateFunctionHandler());

        this.query.parse(queryString);
        this.paths = paths.clone();
    }
	/** クエリを実行する */
    public void execute() throws QueryExecutionException {
        List<LogRecord> records = readLogs();
        this.queryResults = this.query.execute(records);
    }

    /**
     * pathsに設定されたパスからApacheのログファイルを読み込む。
     * @return 読み込んだログの内容を含むLogRecordのリスト
     */
    private List<LogRecord> readLogs() {
        String format = System.getProperty(CUSTOM_LOG_FORMAT_PROP_KEY, LOG_FORMAT);
        LogParser parser = new LogParser(format);
        List<LogRecord> records = new ArrayList<LogRecord>();
        for (String path: this.paths) {
            List<LogRecord> recordsForFile;
            try {
                recordsForFile = readLog(path, parser);
                records.addAll(recordsForFile);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Read Error in file:" + path + " : " + e.getMessage());
            }
        }
        return records;
    }
    /**
     * 1ファイルからログを読み込む
     * @param path ファイルパス
     * @param parser パーサー
     * @return 読み込んだログの内容を含むLogRecordのリスト
     * @throws IOException ファイル読み込み時のIO例外
     */
    private List<LogRecord> readLog(String path, LogParser parser) throws IOException {
        List<LogRecord> records = new ArrayList<LogRecord>();
        FileReader fileReader = new FileReader(path);
        try {
            BufferedReader reader = new BufferedReader(fileReader);
            int lineNumber = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    LogRecord logRecord = parser.parseLine(line);
                    records.add(logRecord);
                } catch (LogParseException e) {
                    e.printStackTrace();
                    System.err.println("Parse Error at line:" + lineNumber +
                            " in file:" + path + " : " + e.getMessage());
                }
            }
            return records;
        } finally {
            fileReader.close();
        }
    }
    /**
     * 結果出力をおこなう
     */
    public void printResults() {
        Map groupByResults = this.queryResults.getGroupByResults();
        if (groupByResults == null) {
            printQueryResults(this.queryResults.getResults());
        } else {
            printGroupByResults(this.queryResults.getResults(), groupByResults);
        }
    }
    /**
     * 通常の結果出力をおこなう
     * @param results 結果リスト
     */
    private void printQueryResults(List results) {
        for (Object o: results) {
            printSingleResult(o);
        }
    }
    /**
     * group by 使用時の結果出力をおこなう
     * @param results 結果リスト
     * @param groupByResults QueryResultsから取得したgroup byの結果
     */
    private void printGroupByResults(List results, Map groupByResults) {
        for (Object groupItem: results) {
            List eachResults = (List) groupByResults.get(groupItem);
            printSingleResult(eachResults.isEmpty() ? "" : eachResults.get(0));
        }
    }
    /**
     * 一行分のオブジェクトを出力する。
     * Listなら要素をタブ区切りで、そうでない場合はtoString()の結果を書き出す。
     * @param resultObject 出力するオブジェクト
     */
    private void printSingleResult(Object resultObject) {
        if (resultObject instanceof List) {
            Iterator it = ((List) resultObject).iterator();
            while (it.hasNext()) {
                System.out.print(it.next());
                if (it.hasNext()) {
                    System.out.print("\t");
                }
            }
            System.out.println();
        } else {
            System.out.println(resultObject.toString());
        }
    }
}