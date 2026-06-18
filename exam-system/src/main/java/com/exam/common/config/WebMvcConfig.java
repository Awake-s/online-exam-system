package com.exam.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${upload.path:./uploads/}")
    private String uploadPath;

    /**
     * CORS 允许的 Origin 列表（配置驱动，支持多域名精确白名单）
     * <p>
     * 开发环境（application.yml 未设）：默认 "*"（通配，便于本机联调）<br>
     * 生产环境（application-prod.yml / secrets.env）：配置精确域名，禁止通配
     * <p>
     * 示例：<pre>
     *   app:
     *     cors:
     *       allowed-origins: http://124.222.21.219,https://examplatform.online
     * </pre>
     */
    @Value("${app.cors.allowed-origins:*}")
    private String[] allowedOrigins;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absPath = new java.io.File(uploadPath).getAbsolutePath().replace('\\', '/');
        if (!absPath.endsWith("/")) absPath += "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absPath);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
