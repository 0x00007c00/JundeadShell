package com.jun.apt.util;

import com.jun.apt.AgentEntry;

import java.io.*;
import java.util.Arrays;
import java.util.Properties;

public class Utils {

    public static byte[] mergeByteArray(byte[]... byteArray) {
        int totalLength = 0;
        for (byte[] bytes : byteArray) {
            if (bytes == null) {
                continue;
            }
            totalLength += bytes.length;
        }
        byte[] result = new byte[totalLength];
        int cur = 0;
        for (byte[] bytes : byteArray) {
            if (bytes == null) {
                continue;
            }
            System.arraycopy(bytes, 0, result, cur, bytes.length);
            cur += bytes.length;
        }
        return result;
    }

    public static byte[] getBytesFromFile(String fileName) {
        try {
            byte[] result = new byte[]{};
            InputStream is = new FileInputStream(fileName);
            byte[] bytes = new byte[1024];
            int num;
            while ((num = is.read(bytes)) != -1) {
                result = mergeByteArray(result, Arrays.copyOfRange(bytes, 0, num));
            }
            is.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getStringFromFile(String fileName) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void readFile(String dirPath, String fileName) throws Exception {
        File f = new File(dirPath + File.separator + fileName);
        InputStream is = new BufferedInputStream(new FileInputStream(f));
        byte[] result = new byte[]{};
        byte[] bytes = new byte[1024];
        int num;
        while ((num = is.read(bytes)) != -1) {
            result = mergeByteArray(result, Arrays.copyOfRange(bytes, 0, num));
        }
        is.close();
        if (fileName.equals("agent.jar")) {
            AgentEntry.AGENT_JAR = result;
        } else if (fileName.equals(AgentEntry.AGENT_STARTER)) {
            AgentEntry.AGENT_STARTER_JAR = result;
        }
    }

    public static boolean delFile(String dirPath, String fileName) {
        String path = dirPath + File.separator + fileName;
        File f = new File(path);
        if (f.exists()) {
            return f.delete();
        } else {
            return false;
        }
    }

    public static void writeFile(String dirPath, String fileName) {
        String path = dirPath + File.separator + fileName;
        File f = new File(path);
        if (f.exists()) {
            Debugger.log("file already exist: " + path);
        } else {
            try {
                OutputStream fos = new BufferedOutputStream(new FileOutputStream(path));
                if (fileName.equals(AgentEntry.AGENT)) {
                    fos.write(AgentEntry.AGENT_JAR);
                } else if (fileName.equals(AgentEntry.AGENT_STARTER)) {
                    fos.write(AgentEntry.AGENT_STARTER_JAR);
                }
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static String getProperty(String key) {
        String path = AgentEntry.TMP_DIR + "/" + AgentEntry.AGENT_CONFIG_FILE;
        String val = null;
        InputStream in;
        try {
            in = new FileInputStream(path);
            Properties prop = new Properties();
            prop.load(in);
            val = prop.getProperty(key);
            if (val == null) {
                val = "";
            } else {
                val = val.trim();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return val;
    }

    public static void setProperty(String key, String val) {
        String path = AgentEntry.TMP_DIR + "/" + AgentEntry.AGENT_CONFIG_FILE;
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(path, true));
            w.write(key + "=" + val);
            w.newLine();
            w.close();
        } catch (FileNotFoundException ignored) {
        } catch (IOException ignored) {
        }
    }

}
