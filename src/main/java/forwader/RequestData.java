package forwader;

import java.util.Map;

public class RequestData {

    public RequestData(String url, String host, String method, String agentId, String postdata, long t,
                       Map<String, String> headers) {
        this.url = url;
        this.host = host;
        this.method = method;
        this.agentId = agentId;
        this.postdata = postdata;
        this.t = t;
        this.headers = headers;
    }

    private String url;
    private Map<String, String> headers;
    private String host;
    private String method;
    private String agentId;
    private String postdata;
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

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
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
        return postdata;
    }

    public void setPostData(String postdata) {
        this.postdata = postdata;
    }

    public long getT() {
        return t;
    }

    public void setT(long t) {
        this.t = t;
    }

}
