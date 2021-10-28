package com.jun.apt.util;

public class Debugger {
    public static void log(Object x) {
        if ("true".equals(Utils.getProperty("debug")))
            System.out.println(x);
    }
}
