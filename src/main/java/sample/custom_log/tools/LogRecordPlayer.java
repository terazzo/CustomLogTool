package sample.custom_log.tools;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParamBean;

import sample.custom_log.core.LogRecord;
import sample.custom_log.util.NoncancelableTask;

public class LogRecordPlayer implements Runnable {
    private final LogRecord logRecord;
    private final LogPlayerSettings settings;
    private Log logger = LogFactory.getLog(LogRecordPlayer.class);

    public LogRecordPlayer(LogRecord logRecord, LogPlayerSettings settings) {
        this.logRecord = logRecord;
        this.settings = settings;
    }

    @Override
    public void run() {
        try {
            playRecord(logRecord);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // HEADとGETのみ対応
    private static final List<String> SUPPORTED_METHODS = Arrays.asList(HttpGet.METHOD_NAME, HttpHead.METHOD_NAME);
    private void playRecord(LogRecord logRecord) throws URISyntaxException, IOException {
        if (! SUPPORTED_METHODS.contains(logRecord.getMethod().toUpperCase())) {
            return;
        }
        
        long requestTime = logRecord.getRequestTime().getTime(); 
        long timeToWait = (long)((requestTime - settings.recordOrigin) * settings.rate) -
                            (System.currentTimeMillis() - settings.startTime);
        if (timeToWait > 0) {
            logger.debug(String.format("waiting (%d msec)", timeToWait));
            new NoncancelableTask(){
                @Override
                protected boolean endure(long millis) throws InterruptedException {
                    Thread.sleep(millis);
                    return false;
                }
            }.runWithLimit(timeToWait);
        } else {
            logger.warn(String.format("delaying (%d msec)", -timeToWait));
        }
        
        HttpClient httpClient = new DefaultHttpClient();
        HttpUriRequest request = prepareRequest(logRecord);
        HttpResponse response = httpClient.execute(request);
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[%s] %s : %s",
                logRecord.getRequestTime(), logRecord.getRequestUri(), response.getStatusLine()));
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
                logger.debug("  to:" + response.getLastHeader("Location"));
            }
        }
    }
    // logRecordからHttpUriRequestを生成する
    private HttpUriRequest prepareRequest(LogRecord logRecord) throws URISyntaxException {
        URI uri = URIUtils.createURI("http", settings.domain, -1, logRecord.getRequestUri(),
                null, null);

        HttpUriRequest request;
        if (logRecord.getMethod().equalsIgnoreCase(HttpHead.METHOD_NAME)) {
            request = new HttpHead(uri);
        } else {
            request = new HttpGet(uri);
        }
        for (Map.Entry<String, String> headerEntry : logRecord.getHeader().entrySet()) {
            request.addHeader(headerEntry.getKey(), headerEntry.getValue());
        }
        String referer = logRecord.getReferer();
        if (!StringUtils.isEmpty(referer) && !referer.equals("-")) {
            request.addHeader("Referer", logRecord.getReferer());
        }
        // パラメータ
        HttpParams params = new BasicHttpParams();
        HttpProtocolParamBean paramsBean = new HttpProtocolParamBean(params);
        paramsBean.setVersion(HttpVersion.HTTP_1_1);
        paramsBean.setUserAgent(logRecord.getUserAgent());
        params.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
        request.setParams(params);
        return request;
    }

}
