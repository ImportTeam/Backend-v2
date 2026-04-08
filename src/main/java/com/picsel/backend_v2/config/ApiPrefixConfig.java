package com.picsel.backend_v2.config;

import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

/**
 * 모든 우리 컨트롤러에 /api 접두사를 자동으로 붙입니다.
 * springdoc(Swagger) 컨트롤러는 제외하여 /swagger 경로를 그대로 유지합니다.
 */
@Configuration
public class ApiPrefixConfig implements WebMvcRegistrations {

    private static final String API_PREFIX = "/api";

    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new RequestMappingHandlerMapping() {
            @Override
            protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
                RequestMappingInfo info = super.getMappingForMethod(method, handlerType);
                if (info == null) return null;

                // springdoc 컨트롤러는 접두사 제외 (Swagger UI 경로 보존)
                String pkg = handlerType.getPackageName();
                if (pkg.startsWith("org.springdoc")) return info;

                // 우리 컨트롤러에만 /api 접두사 추가
                RequestMappingInfo prefix = RequestMappingInfo
                        .paths(API_PREFIX)
                        .options(getBuilderConfiguration())
                        .build();
                return prefix.combine(info);
            }
        };
    }
}
