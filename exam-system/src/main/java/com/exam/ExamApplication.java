package com.exam;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;

@SpringBootApplication
@MapperScan("com.exam.mapper")
@EnableScheduling
public class ExamApplication {
    public static void main(String[] args) {
        Environment env = SpringApplication.run(ExamApplication.class, args).getEnvironment();
        String port = env.getProperty("server.port", "8081");
        String path = env.getProperty("server.servlet.context-path", "");
        String ip = "localhost";
        try { ip = InetAddress.getLocalHost().getHostAddress(); } catch (Exception ignored) {}

        String url  = "http://localhost:" + port + path;
        String net  = "http://" + ip + ":" + port + path;
        String ui   = "http://localhost:3000";
        String api  = url + "/api/**";
        // 找出最长行来决定框宽
        int max = Math.max(Math.max(url.length(), net.length()), api.length()) + 20;
        String line = "+" + "-".repeat(max) + "+";
        String blank = "|" + " ".repeat(max) + "|";
        System.out.println("\n" + line + "\n" +
            row("(♥◠‿◠)ﾉﾞ  在线考试系统启动成功！  ლ(´ڡ`ლ)ﾞ", max) + "\n" +
            blank + "\n" +
            row("后端本地地址: " + url, max) + "\n" +
            row("网络访问地址: " + net, max) + "\n" +
            row("前端访问地址: " + ui, max) + "\n" +
            row("后端api接口前缀: " + api, max) + "\n" +
            line);
    }

    private static String row(String text, int width) {
        return "|  " + text + " ".repeat(Math.max(0, width - text.length() - 2)) + "|";
    }
}
