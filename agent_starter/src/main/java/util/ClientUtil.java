package util;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import cons.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientUtil {
    public static Map<String, String> getArgs(String[] args) {
        Map<String, String> map = new HashMap<String, String>();
        String assignName;
        if (args.length == 1 || args.length == 2) {
            String argsName = args[0];
            if (argsName.equals("help")) {
                Logger.help();
                return null;
            }

            if (argsName.equals("list")) {
                FileUtil.releaseLibJar();
                Logger.print("List all Java processes.");
                List<VirtualMachineDescriptor> list = VirtualMachine.list();
                for (VirtualMachineDescriptor vmd : list) {
                    if (!vmd.displayName().contains(Constants.AGENT_STARTER))
                        Logger.print("pid:" + vmd.id() + ", displayName:" + vmd.displayName());
                }
                List<String> l = new ArrayList<String>();
                l.add(Constants.LOCAL_PID);
                FileUtil.clean4windows(l);
                FileUtil.delTmpLib();
                return null;
            }

            assignName = argsName;
            Logger.print("The displayName of the injection target process: " + assignName);

            String tomcat_version = "8";
            if (args.length == 2) {
                tomcat_version = args[1];
            }
            map.put("assignName", assignName);
            map.put("tomcat_version", tomcat_version);
        } else {
            Logger.help();
            return null;
        }

        return map;
    }
}
