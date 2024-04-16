package com.cfde.playbook_ctd.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StackTracePrinter {

    public static String printStackTrace(Exception e){
        StringWriter stack = new StringWriter();
        e.printStackTrace(new PrintWriter(stack));
        return stack.toString();
    }

}
