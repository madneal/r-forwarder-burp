package forwader;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class LogEntry {
    final int id;
    final IHttpRequestResponsePersisted requestResponse;
    final String url;
    final String method;
    final String result;
//    final String proxyResponse;
    public String requestTime;
    public String toolSource;

    LogEntry(int id, IHttpRequestResponsePersisted requestResponse, String url, String toolSource,
             String method, Map<String,String> mapResult) {
        this.id = id;
        this.requestResponse = requestResponse;
        this.url = url;
        this.method = method;
        this.result = mapResult.get("result");
        this.toolSource = toolSource;
//        this.proxyResponse =
        this.requestTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
    }
}
