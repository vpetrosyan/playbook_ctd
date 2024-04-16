package com.cfde.playbook_ctd.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ShellCommandFormatter {

    public static String formatted(String template, Map<String, String> substs) {
        String result = template;
        for (Map.Entry<String, String> subst : substs.entrySet()) {
            String pattern = subst.getKey();
            result = result.replace(pattern, subst.getValue());
        }
        return result;
    }

    /* Example use:
    List<String> split = ShellCommandFormatter.split(formattedCommand);
    String[] call = new String[split.size()];
    call = split.toArray(call);*/
    public static List<String> split(CharSequence string) {
        List<String> tokens = new ArrayList<String>();
        boolean escaping = false;
        char quoteChar = ' ';
        boolean quoting = false;
        int lastCloseQuoteIndex = Integer.MIN_VALUE;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (escaping) {
                current.append(c);
                escaping = false;
            } else if (c == '\\' && !(quoting && quoteChar == '\'')) {
                escaping = true;
            } else if (quoting && c == quoteChar) {
                quoting = false;
                lastCloseQuoteIndex = i;
            } else if (!quoting && (c == '\'' || c == '"')) {
                quoting = true;
                quoteChar = c;
            } else if (!quoting && Character.isWhitespace(c)) {
                if (current.length() > 0 || lastCloseQuoteIndex == (i - 1)) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0 || lastCloseQuoteIndex == (string.length() - 1)) {
            tokens.add(current.toString());
        }

        return tokens;
    }
}
