package com.kingeid.cjh.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpUtils {
    /**
     * 判断端口号是否合法
     *
     * @param portStr
     * @return
     */
    public static boolean isPort(String portStr) {
        if (portStr == null) {
            return false;
        }
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(portStr);
        if (isNum.matches() && portStr.length() < 6 && Integer.valueOf(portStr) >= 1
                && Integer.valueOf(portStr) <= 65535) {
            return true;
        }
        return false;
    }

}
