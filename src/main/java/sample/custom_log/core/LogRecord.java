package sample.custom_log.core;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.builder.ToStringBuilder;

/** 
 * Apacheのカスタムログの1リクエスト分の情報を保持するクラス。
 * cf: http://httpd.apache.org/docs/2.2/en/mod/mod_log_config.html
 */
public class LogRecord {
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    private String remoteHost;
    private String remoteLogname;
    private String remoteUser;
    private Date requestTime;
    private String requestLine;
    private int status;
    private int responseSize;
    private String referer;
    private String userAgent;
    /** リクエストヘッダの内容を保持するMap。キー=ヘッダフィールド名、値=ヘッダ値 */
    private Map<String, String> requestHeaders = Collections.emptyMap();
    /** Method/Request-URI/Protocol-VersonおよびURIのパラメータを保持するオブジェクト */
    private RequestLine requestLineObject = null;
    /** デフォルトコンストラクタ */
    public LogRecord() {
    }
    /* 検索/情報取得用の便利メソッド */
    /** @return リクエストメソッドを戻す */
    public String getMethod() {
        return (this.requestLineObject != null) 
                ? this.requestLineObject.getMethod() : null;
    }
    /** @return リクエストURIを戻す */
    public String getRequestUri() {
        return (this.requestLineObject != null) 
                ? this.requestLineObject.getRequestUri() : null;
    }
    /** @return プロトコルバージョンを戻す */
    public String getProtocolVersion() {
        return (this.requestLineObject != null) 
                ? this.requestLineObject.getProtocolVersion() : null;
    }
    /** @return リクエストパスを戻す */
    public String getRequestPath() {
        return (this.requestLineObject != null) 
                ? this.requestLineObject.getRequestPath() :null;
    }
    /** @return リクエストパラメータのMapを戻す */
    public Map<String, String> getParam() {
        return (this.requestLineObject != null) 
                ? this.requestLineObject.getParamMap()
                        :Collections.<String, String>emptyMap();
    }
    /** @return リクエストヘッダのMapを戻す */
    public Map<String, String> getHeader() {
        return this.requestHeaders;
    }

    /* getters */
    public String getRemoteHost() {
        return remoteHost;
    }
    public String getRemoteLogname() {
        return remoteLogname;
    }
    public String getRemoteUser() {
        return remoteUser;
    }
    public Date getRequestTime() {
        return requestTime;
    }
    public String getRequestLine() {
        return requestLine;
    }
    public int getStatus() {
        return status;
    }
    public int getResponseSize() {
        return responseSize;
    }
    public String getReferer() {
        return referer;
    }
    public String getUserAgent() {
        return userAgent;
    }
    /* setters */
    protected void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }
    protected void setRemoteLogname(String remoteLogname) {
        this.remoteLogname = remoteLogname;
    }
    protected void setRemoteUser(String remoteUser) {
        this.remoteUser = remoteUser;
    }
    protected void setRequestTime(Date requestTime) {
        this.requestTime = requestTime;
    }
    /**
     * リクエスト行をセットする。同時にrequestLineObjectを更新する
     * @param requestLine リクエスト行(例:「GET / HTTP/1.0」)
     */
    public void setRequestLine(String requestLine) {
        this.requestLine = requestLine;
        this.requestLineObject = new RequestLine(requestLine);
    }
    protected void setStatus(int status) {
        this.status = status;
    }
    protected void setReferer(String referer) {
        this.referer = referer;
    }
    protected void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    protected void setResponseSize(int responseSize) {
        this.responseSize = responseSize;
    }
    /**
     * リクエストヘッダをセットする
     * @param name ヘッダフィールド名
     * @param value ヘッダフィールド値
     */
    public void setRequestHeader(String name, String value) {
        if (requestHeaders == Collections.<String,String>emptyMap()) {
            requestHeaders = new TreeMap<String, String>();
        }
        requestHeaders.put(name, value);
    }
    /**
     * リクエスト行から情報を取り出し保持する為のクラス
     * リクエスト行の例: 「GET /a.cgi?category=aaa HTTP/1.1」
     */
    private static class RequestLine {
        /** HTTPメソッド。例: 「GET」 */
        private String method;
        /** リクエストURI。例: 「/a.cgi?category=aaa」 */
        private String requestUri;
        /** プロトコルとバージョン。例: 「HTTP/1.1」 */
        private String protocolVersion;

        /** リクエストURI。例: 「/a.cgi」 */
        private String requstPath;
        /** パラメータのMap。例: キー=「category」、値=「aaa」 (%エンコーディングはデコードしない)*/
        private Map<String, String> paramMap =  Collections.emptyMap();

        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
        public RequestLine(String requstLine) {
            int length = requstLine.length();
            int cur, pos = 0;
            pos = requstLine.indexOf(' ');
            if (pos == -1) {
                method = requstLine;
                return;
            }
            method = requstLine.substring(0, pos);
            cur = pos + 1;
            pos = requstLine.indexOf(' ', cur);
            if (pos != -1) {
                requestUri = requstLine.substring(cur, pos);
                updatePathAndParam();
            } else {
                requestUri = requstLine.substring(cur, length);
                updatePathAndParam();
                return;
            }
            protocolVersion = requstLine.substring(pos + 1);
        }
        /** パスとパラメータを更新する 
         * @param max 
         * @param cur 
         * @param requstLine */
        private void updatePathAndParam() {
            int length = requestUri.length();
            int pos = requestUri.indexOf('?');
            if (pos == -1) {
                requstPath = requestUri;
                return;
            }
            this.paramMap = new TreeMap<String, String>();
            requstPath = requestUri.substring(0, pos);
            int cur = pos + 1;
            while (pos != -1) {
                String key = null;
                String value;
                char c = '\0';
                for (pos = cur; pos < length && (c = requestUri.charAt(pos)) != '=' && c  != '&';  pos++);
                if (c == '=') {
                    key = requestUri.substring(cur, pos);
                    cur = pos + 1;
                }
                pos = requestUri.indexOf('&', pos + 1);
                if (pos == -1) {
                    value = requestUri.substring(cur);
                } else {
                    value = requestUri.substring(cur, pos);
                    cur = pos + 1;
                }
                if (key == null) {
                    paramMap.put(value, null);
                } else {
                    paramMap.put(key, value);
                }
                
            }
        }

        public String getMethod() {
            return method;
        }
        public String getRequestUri() {
            return requestUri;
        }
        public String getProtocolVersion() {
            return protocolVersion;
        }
        public String getRequestPath() {
            return requstPath;
        }
        public Map<String, String> getParamMap() {
            return paramMap;
        }
    }
}