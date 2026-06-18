package com.exam.common.utils;

/**
 * XSS防护工具类
 * 对用户输入的文本字段进行HTML特殊字符转义，防止存储型XSS攻击
 */
public class XssUtils {

    /**
     * 转义HTML特殊字符，防止XSS攻击
     * 将 < > " ' & 等字符转为HTML实体
     */
    public static String escapeHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#x27;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 检测字符串是否包含潜在的XSS内容
     */
    public static boolean containsXss(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        String lower = input.toLowerCase();
        return lower.contains("<script") || lower.contains("javascript:") ||
               lower.contains("onerror") || lower.contains("onload") ||
               lower.contains("<img") || lower.contains("<iframe") ||
               lower.contains("<svg") || lower.contains("onclick");
    }
}
