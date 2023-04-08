package cons;

import util.FileUtil;

import java.lang.management.ManagementFactory;

public class Constants {
    public static final String AGENT = "agent.jar";
    public static final String AGENT_STARTER = FileUtil.getLocalFileName();
    public static final String LOCAL_DIR = FileUtil.getLocalDir();
    public static final String AGENT_CONFIG_FILE = "agent.properties";
    public static final String TMP_DIR = System.getProperty("os.name").toLowerCase().contains("windows") ? "C:/Users/Public/Documents" : "/tmp";
    public static final String TMP_LIB = "tmp_lib";
    public static final String TOOLS_JAR = System.getProperty("os.name").toLowerCase().contains("windows") ? "tools_windows.jar" : "tools_linux.jar";
    public static final String LOCAL_PID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    public static final String DEBUG = FileUtil.getProperty("debug");
    public static final String TOMCAT_CLASS_DIR = "/payload/";
    public static String START_ARGS;
    public static String TOMCAT_VERSION;
    public static String AGENT_JAR_PATH;
}
