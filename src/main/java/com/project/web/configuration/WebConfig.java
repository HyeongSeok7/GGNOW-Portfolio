package com.project.web.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	// CSS, JavaScript, 이미지 같은 정적 리소스를 직접 매핑
	// application.properties에서 기본 정적 리소스 매핑을 비활성화했기 때문에 필요한 설정!
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/assets/**").addResourceLocations("classpath:/static/assets/");
		registry.addResourceHandler("/images/**").addResourceLocations("classpath:/static/images/");
	}
}