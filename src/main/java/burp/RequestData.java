package burp;

import java.util.List;
import java.util.Map;

public class RequestData {

    public RequestData(String url, String host, String method, String agentId, String postData, long t,
                       List<Map<String, String>> headers) {
        this.url = url;
        this.host = host;
        this.method = method;
        this.agentId = agentId;
        this.postData = postData;
        this.t = t;
        this.headers = headers;
    }

    private String url;
    private List<Map<String, String>> headers;
    private String host;
    private String method;
    private String agentId;
    private String postData;
    private long t;


    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<Map<String, String>> getHeaders() {
        return headers;
    }

    public void setHeaders(List<Map<String, String>> headers) {
        this.headers = headers;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getPostData() {
        return postData;
    }

    public void setPostData(String postData) {
        this.postData = postData;
    }

    public long getT() {
        return t;
    }

    public void setT(long t) {
        this.t = t;
    }

}
