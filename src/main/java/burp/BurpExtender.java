package burp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.awt.Component;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;



public class BurpExtender implements IBurpExtender, ITab, IHttpListener {
    public final static String extensionName = "R-forwarder";
    public final static String version ="0.0.2";
    public static IBurpExtenderCallbacks callbacks;
    public static IExtensionHelpers helpers;
    public static PrintWriter stdout;
    public static PrintWriter stderr;
    public static GUI gui;
    public static final List<LogEntry> log = new ArrayList<>();
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
        SwingUtilities.invokeLater(() -> {
            BurpExtender.this.callbacks.addSuiteTab(BurpExtender.this);
//                BurpExtender.this.callbacks.registerProxyListener(BurpExtender.this);
            BurpExtender.this.callbacks.registerHttpListener(BurpExtender.this);
//                stdout.println(Utils.getBanner());
        });
        executorService = Executors.newSingleThreadExecutor();
        //必须等插件界面显示完毕，重置JTable列宽才生效
        SwingUtilities.invokeLater(() -> {
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
        });
    }


    @Override
    public String getTabCaption() {
        return extensionName;
    }

    @Override
    public Component getUiComponent() {
        return gui.getComponet();
    }

    public Map<String, String> getHeaders(IHttpRequestResponse messageInfo) {
        Map<String, String> headers = new HashMap<>();
        IRequestInfo analyzeRequest = helpers.analyzeRequest(messageInfo);
        List<String> h = analyzeRequest.getHeaders();
        for (String h1: h) {
            if (h1.startsWith("GET") || h1.startsWith("POST")) {
                continue;
            } else {
                String[] header = h1.split(":", 2);
                headers.put(header[0], header[1].trim());
            }
        }
        return headers;
    }

    private String getToolSource(int toolFlag) {
        switch (toolFlag) {
            case 1:
                return "SUITE";
            case 2:
                return "TARGET";
            case 4:
                return "PROXY";
            case 8:
                return "SPIDER";
            case 16:
                return "SCANNER";
            case 32:
                return "INTRUDER";
            case 64:
                return "REPEATER";
            case 128:
                return "SEQUENCER";
            case 256:
                return "DECODER";
            case 512:
                return "COMPARER";
            case 1024:
                return "EXTENDER";
            default:
                return "";
        }
    }

    public void processHttpMessage(int toolFlag, boolean messageIsRequest, final IHttpRequestResponse messageInfo) {
        try {
            if (!messageIsRequest) {
                return;
            }
            if (!Config.IS_RUNNING) {
                return;
            }
            if (!(toolFlag == 4 || toolFlag == 64 || toolFlag == 32)) {
                return;
            }
            IRequestInfo request = helpers.analyzeRequest(messageInfo);
            if (!Utils.isMathch(Config.DOMAIN_REGX, request.getUrl().getHost())) {
                return;
            }
            if (Utils.isMathch(Config.SUFFIX_REGX, request.getUrl().toString())) {
                return;
            }
            String toolSource = getToolSource(toolFlag);
            RequestData requestData = getRequestData(messageInfo);
            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            String result = gson.toJson(requestData);
            Map<String, String> res = sendPost(Config.SERVICE, result);

            Config.RequestId++;
            int row = log.size();
            log.add(new LogEntry(Config.RequestId,
                    callbacks.saveBuffersToTempFiles(messageInfo), requestData.getUrl(), toolSource,
                    request.getMethod(), res)
            );
            GUI.logTable.getHttpLogTableModel().fireTableRowsInserted(row, row);
            if (res.get("code").equals("0")) {
                Utils.updateSuccessCount();
            } else {
                Utils.updateFailCount();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getBody(IHttpRequestResponse messageInfo) {
        IRequestInfo requestInfo = helpers.analyzeRequest(messageInfo);
        int bodyOffset = requestInfo.getBodyOffset();
        byte[] byteRequest = messageInfo.getRequest();
        byte[] byteBody = Arrays.copyOfRange(byteRequest, bodyOffset, byteRequest.length);
        return new String(byteBody);
    }

    private Map<String, String> sendPost(String url, String data) {
        int timeout = 5;
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000).build();
        HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
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
                String resultPro = "";
                switch (jsonObject.get("code").getAsInt()) {
                    case 0:
                        resultPro = "上传成功";
                        break;
                    case 1:
                        resultPro = "不支持的请求方法";
                        break;
                    case 2:
                        resultPro = "处理请求失败";
                        break;
                    case 3:
                        resultPro = "存入 mq 失败";
                        break;
                }
                result.put("result", resultPro);
                result.put("code", jsonObject.get("code").getAsString());
            }
        } catch (SocketTimeoutException s) {
            stderr.print("Request to " + url + " timeout!");
            result.put("result", "请求超时");
            result.put("code", "-1");
        } catch (ConnectTimeoutException e1) {
            stderr.print("Request to " + url + " timeout!");
            result.put("result", "请求超时");
            result.put("code", "-1");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return result;

    }

    // obtain request data as JSON string
    private RequestData getRequestData(IHttpRequestResponse messageInfo) {
        RequestData requestData = null;
        try {
            Map<String, String> headers = getHeaders(messageInfo);
            IRequestInfo requestInfo = helpers.analyzeRequest(messageInfo);
            String url = requestInfo.getUrl().toString();
            if (!Config.IS_DUPLICATE) {
                url = url + "$$";
            }
            String method = requestInfo.getMethod();
            String host = requestInfo.getUrl().getHost();
            String postdata = "";
            if (method == "POST") {
                postdata = Base64.getEncoder().encodeToString(getBody(messageInfo).getBytes("UTF-8"));
            }
            long t = System.currentTimeMillis();
            requestData = new RequestData(url, host, method, Config.AGENT_ID, postdata, t, headers);
        } catch (Exception e) {
            e.printStackTrace();
            stderr.print(e);
        }
        return requestData;
    }
}