package utils;
import burp.api.montoya.MontoyaApi;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;

public class ConfigLoader {
    private final MontoyaApi api;
    private final Yaml yaml;
    private final String configFilePath;

    public ConfigLoader(MontoyaApi api) {
        this.api = api;
        DumperOptions dop = new DumperOptions();
        dop.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Representer representer = new Representer(dop);
        this.yaml = new Yaml(representer, dop);

        String configPath = getConfigPath();
        this.configFilePath = String.format("%s/%s", configPath, "config.yaml");

        File configFile = new File(configFilePath);

    }

    private String getConfigPath() {
        String userHome = String.format("%s/r-forwarder-burp/config", System.getProperty("user.home"));
        if (isValidPath(userHome)) {
            return userHome;
        }

        String jarPath = api.extension().filename();
        String jarDir = new File(jarPath).getParent();
        String jarConfigPath = String.format("%s/r-forwarder-burp/config", jarDir);
        if (isValidPath(jarConfigPath)) {
            return jarConfigPath;
        }
        return userHome;
    }

    private boolean isValidPath(String path) {
        File configPath = new File(path);
        return configPath.exists() && configPath.isDirectory();
    }
}
