package burp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.awt.Component;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;



public class BurpExtender implements IBurpExtender,ITab,IProxyListener, IHttpListener {
    public final static String extensionName = "Passive Scan Client";
    public final static String version ="0.1";
    public static IBurpExtenderCallbacks callbacks;
    public static IExtensionHelpers helpers;
    public static PrintWriter stdout;
    public static PrintWriter stderr;
    public static GUI gui;
    public static final List<LogEntry> log = new ArrayList<LogEntry>();
    public static BurpExtender burpExtender;
    private ExecutorService executorService;

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.burpExtender = this;
        this.callbacks = callbacks;
        this.helpers = callbacks.getHelpers();
        this.stdout = new PrintWriter(callbacks.getStdout(),true);
        this.stderr = new PrintWriter(callbacks.getStderr(),true);

        callbacks.setExtensionName(extensionName + " " + version);
        BurpExtender.this.gui = new GUI();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                BurpExtender.this.callbacks.addSuiteTab(BurpExtender.this);
                BurpExtender.this.callbacks.registerProxyListener(BurpExtender.this);
                stdout.println(Utils.getBanner());
            }
        });
        executorService = Executors.newSingleThreadExecutor();
        //必须等插件界面显示完毕，重置JTable列宽才生效
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //按照比例显示列宽
                float[] columnWidthPercentage = {5.0f, 5.0f, 55.0f, 20.0f, 15.0f};
                int tW = GUI.logTable.getWidth();
                TableColumn column;
                TableColumnModel jTableColumnModel = GUI.logTable.getColumnModel();
                int cantCols = jTableColumnModel.getColumnCount();
                for (int i = 0; i < cantCols; i++) {
                    column = jTableColumnModel.getColumn(i);
                    int pWidth = Math.round(columnWidthPercentage[i] * tW);
                    column.setPreferredWidth(pWidth);
                }
            }
        });
    }
    //
    // 实现ITab
    //

    @Override
    public String getTabCaption() {
        return extensionName;
    }

    @Override
    public Component getUiComponent() {
        return gui.getComponet();
    }

    public List<Map<String, String>> getHeaders(IHttpRequestResponse messageInfo) {
        List<Map<String, String>> headers = new ArrayList<>();
        IRequestInfo analyzeRequest = helpers.analyzeRequest(messageInfo);
        List<String> h = analyzeRequest.getHeaders();
        for (String h1: h) {
            Map<String, String> hMap = new HashMap<>();
            if (h1.startsWith("GET") || h1.startsWith("POST")) {
                continue;
            } else {
                String[] header = h1.split(":", -1);
                hMap.put(header[0], header[1]);
            }
            headers.add(hMap);
        }
        return headers;
    }

    public void processHttpMessage(int toolFlag, boolean messageIsRequest, final IHttpRequestResponse messageInfo) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                BurpExtender.this.callbacks.addSuiteTab(BurpExtender.this);
                BurpExtender.this.callbacks.registerProxyListener(BurpExtender.this);
                stdout.println(messageInfo.getRequest().toString());
            }
        });
    }

    private String getBody(IHttpRequestResponse messageInfo) {
        IRequestInfo requestInfo = helpers.analyzeRequest(messageInfo);
        int bodyOffset = requestInfo.getBodyOffset();
        byte[] byteRequest = messageInfo.getRequest();
        byte[] byteBody = Arrays.copyOfRange(byteRequest, bodyOffset, byteRequest.length);
        return new String(byteBody);
    }

    private Map<String, String> sendPost(String url, String data) {
        HttpClient client = HttpClientBuilder.create().build();
        Map<String, String> result = new HashMap<>();
        try {
            StringEntity requestEntity = new StringEntity(data, ContentType.APPLICATION_JSON);
            HttpPost post = new HttpPost(url);
            post.setEntity(requestEntity);
            HttpResponse res = client.execute(post);
            if (res.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = res.getEntity();
                Header encodingHeader = entity.getContentEncoding();
                Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8 :
                        Charsets.toCharset(encodingHeader.getValue());
                String json = EntityUtils.toString(entity, encoding);
                JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
                result.put("result", jsonObject.get("result").getAsString());
                result.put("code", jsonObject.get("code").getAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;

    }

    public void processProxyMessage(boolean messageIsRequest, final IInterceptedProxyMessage iInterceptedProxyMessage) {
        try {
            List<Map<String, String>> headers = getHeaders(iInterceptedProxyMessage.getMessageInfo());
            IRequestInfo requestInfo = helpers.analyzeRequest(iInterceptedProxyMessage.getMessageInfo());
            String url = requestInfo.getUrl().toString();
            String method = requestInfo.getMethod();
            String host = requestInfo.getUrl().getHost();
            String postData = "";
            String agentId = "";
            if (method == "POST") {
                postData = getBody(iInterceptedProxyMessage.getMessageInfo());
            }
            long t = System.currentTimeMillis();
            RequestData requestData = new RequestData(url, host, method, agentId, postData, t, headers);
            Gson gson = new Gson();
            String result = gson.toJson(requestData);
            stdout.print(result);
            Map<String, String> res = sendPost("http://localhost:8000/api", result);
            if (res.get("result") == "success") {
                stdout.print("request success!!!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!messageIsRequest && Config.IS_RUNNING) {
            IHttpRequestResponse reprsp = iInterceptedProxyMessage.getMessageInfo();
            IHttpService httpService = reprsp.getHttpService();
            stdout.print(iInterceptedProxyMessage.getMessageInfo());
//            List<String> headers = helpers.analyzeRequest(reprsp).getHeaders();
            for (String header: helpers.analyzeRequest(reprsp).getHeaders()) {
                System.out.println(header);
            }
//            String host = reprsp.getHttpService().getHost();
//            stdout.println("[+] host:" + host);
//            stdout.println(Config.DOMAIN_REGX);
//            if(!Utils.isMathch(Config.DOMAIN_REGX,host)){
//                return;
//            }

//            String url = helpers.analyzeRequest(httpService,reprsp.getRequest()).getUrl().toString();
//            url = url.indexOf("?") > 0 ? url.substring(0, url.indexOf("?")) : url;
//            if(Utils.isMathch(Config.SUFFIX_REGX,url)){
//                return;
//            }

            final IHttpRequestResponse resrsp = iInterceptedProxyMessage.getMessageInfo();

            //final LogEntry logEntry = new LogEntry(1,callbacks.saveBuffersToTempFiles(iInterceptedProxyMessage.getMessageInfo()),helpers.analyzeRequest(resrsp).getUrl());

            // create a new log entry with the message details
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    synchronized(log) {
                        int row = log.size();
                        String method = helpers.analyzeRequest(resrsp).getMethod();
                        Map<String,String> mapResult = HttpAndHttpsProxy.Proxy(resrsp);

                        log.add(new LogEntry(iInterceptedProxyMessage.getMessageReference(),
                                callbacks.saveBuffersToTempFiles(resrsp), helpers.analyzeRequest(resrsp).getUrl(),
                                method,
                                mapResult)
                        );
                        GUI.logTable.getHttpLogTableModel().fireTableRowsInserted(row, row);
                    }
                }
            });
        }
    }
}