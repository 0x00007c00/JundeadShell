package t;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;

public class AutoInstall {

    public static void exec(String command) {
        String osName = System.getProperty("os.name").toLowerCase();
        String charSet = "GBK";
        Process p;
        try {
            if (osName.contains("windows"))
                p = Runtime.getRuntime().exec(new String[]{"cmd", "/C", command});
            else
                p = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    p.getInputStream(), Charset.forName(charSet)));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void fileCopy(String source, String dest) {
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            out.transferFrom(in, 0, in.size());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                assert in != null;
                in.close();
                assert out != null;
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        exec("mvn clean package -DskipTests install");
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis()) + " 正在拷贝文件");
        fileCopy("agent/target/agent.jar", "agent_starter/src/main/resources/agent/agent.jar");

        fileCopy("payload-tomcat-7/target/classes/org/apache/catalina/core/ApplicationFilterChain.class",
                "agent_starter/src/main/resources/payload/tomcat7.cls");
        fileCopy("payload-tomcat-8/target/classes/org/apache/catalina/core/ApplicationFilterChain.class",
                "agent_starter/src/main/resources/payload/tomcat8.cls");
        fileCopy("payload-tomcat-9/target/classes/org/apache/catalina/core/ApplicationFilterChain.class",
                "agent_starter/src/main/resources/payload/tomcat9.cls");

        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis()) + " 拷贝完成");

        exec("mvn clean package -DskipTests install -f agent_starter/pom.xml");
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis()) + " 安装完成");
    }
}
