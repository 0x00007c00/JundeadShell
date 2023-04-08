package util;

import cons.Constants;

import java.io.*;
import java.util.List;
import java.util.Properties;

public class FileUtil {

    public static void releaseFile(String readPath, String writeFile) {
        try {
            BufferedInputStream is = new BufferedInputStream(FileUtil.class.getResourceAsStream(readPath));
            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(Constants.LOCAL_DIR + File.separator + writeFile));
            byte[] b = new byte[1024];
            int len;
            while ((len = is.read(b)) != -1) {
                os.write(b, 0, len);
            }
            os.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void releaseLibJar() {
        try {
            String writeDir = Constants.LOCAL_DIR + File.separator + Constants.TMP_LIB;
            File folder = new File(writeDir);
            if (folder.exists() && folder.isDirectory()) {
            } else {
                folder.mkdirs();
            }
            String writePath = writeDir + File.separator + "tools.jar";
            if (!new File(writePath).exists()) {
                BufferedInputStream is = new BufferedInputStream(FileUtil.class.getResourceAsStream("/lib/" + Constants.TOOLS_JAR));
                BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(writePath));
                byte[] b = new byte[1024];
                int len;
                while ((len = is.read(b)) != -1) {
                    os.write(b, 0, len);
                }
                os.close();
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean delTmpLib() {
        String localDir = Constants.LOCAL_DIR;
        delFile(localDir, Constants.TMP_LIB + "/tools.jar");
        String path = localDir + File.separator + Constants.TMP_LIB;
        File f = new File(path);
        if (f.exists() && f.isDirectory() && f.listFiles().length == 0) {
            return f.delete();
        } else {
            return false;
        }
    }

    public static void outPutLog(String s) {
        String path = Constants.LOCAL_DIR + File.separator + "logger.out";
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(path, true));
            bw.write(s);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void generateConfig() {
        String path = Constants.TMP_DIR + "/" + Constants.AGENT_CONFIG_FILE;
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(path));
            w.write("starter_name=" + Constants.AGENT_STARTER);
            w.newLine();
            w.write("password=" + getProperty("password"));
            w.newLine();
            w.write("debug=" + Constants.DEBUG);
            w.newLine();
            w.write("secret_key=" + getProperty("secret_key"));
            w.newLine();
            w.write("dir_path=" + Constants.LOCAL_DIR);
            w.newLine();
            w.write("start_args=" + Constants.START_ARGS);
            w.newLine();
            w.write("tomcat_version=" + Constants.TOMCAT_VERSION);
            w.newLine();
            w.close();
        } catch (IOException ignored) {
            Logger.printErr("写入配置信息失败。path:" + path);
        }
    }

    public static String getAgentConfig(String key) {
        String val = null;
        String path = Constants.TMP_DIR + "/" + Constants.AGENT_CONFIG_FILE;
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            while ((line = br.readLine()) != null) {
                String tmp_key = line.split("=")[0].trim();
                if (key.equals(tmp_key)) {
                    val = line.split("=")[1].trim();
                    // Make the last cycle effective
                    // break;
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return val;
    }

    public static boolean delFile(String dirPath, String fileName) {
        String path = dirPath + "/" + fileName;
        File f = new File(path);
        if (f.exists()) {
            return f.delete();
        } else {
            return false;
        }
    }

    public static String getProperty(String key) {
        String val = null;
        InputStream in = FileUtil.class.getResourceAsStream("/config/jund.properties");
        Properties prop = new Properties();
        try {
            prop.load(in);
            val = prop.getProperty(key);
            if (val == null) {
                val = "";
            } else {
                val = val.trim();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return val;
    }

    public static String getLocalFileName() {
        String file = FileUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (file != null) {
            file = file.substring(file.lastIndexOf("/") + 1);
        }
        return file;
    }

    public static String getLocalDir() {
        String path = FileUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            path = path.substring(1);
        }
        String localDir = null;
        if (path.endsWith("/" + Constants.AGENT_STARTER)) {
            localDir = path.substring(0, path.lastIndexOf("/" + Constants.AGENT_STARTER));
        }
        return localDir;
    }

    public static void clean4windows(List<String> pids) {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            // Release Windows handle shutdown program.
            FileUtil.releaseFile("/clean/close.e", "close.exe");

            try {
                for (String pid : pids) {
                    String cmd = Constants.LOCAL_DIR + File.separator + "close.exe " + pid + " " + Constants.AGENT_STARTER;
                    Runtime.getRuntime().exec(cmd);
                }
                Thread.sleep(2000);
                String prefixCmd = "cmd /c ping localhost -n 2 > nul && del " + Constants.LOCAL_DIR.replace("/", "\\") + File.separator;
                Runtime.getRuntime().exec(prefixCmd + "agent.jar");
                Runtime.getRuntime().exec(prefixCmd + Constants.TMP_DIR.replace("/", "\\") + File.separator + "agent.properties");
                Runtime.getRuntime().exec(prefixCmd + "close.exe");
            } catch (Exception e) {
                Logger.print(e.getMessage());
            }

        }
    }
}
