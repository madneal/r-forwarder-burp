package burp;

public class Config {
    public static boolean IS_RUNNING = false;
    public static String SERVICE = "http://127.0.0.1:8000/api";
    public static String AGENT_ID = "";
    public static String DOMAIN_REGX = "";
    public static String SUFFIX_REGX = "js|css|jpeg|gif|jpg|png|pdf|rar|zip|docx|doc";

    public static Integer REQUEST_TOTAL = 0;
    public static Integer SUCCESS_TOTAL = 0;
    public static Integer FAIL_TOTAL = 0;

    public static int RequestId = 0;
}
