package com.gamingbar.common.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
public class RequestLogAspect {

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logRequestCost(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long cost = System.currentTimeMillis() - start;
            HttpServletRequest request = currentRequest();
            if (request == null) {
                log.info("request={} cost={}ms", joinPoint.getSignature().toShortString(), cost);
            } else {
                log.info("method={} uri={} query={} cost={}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getQueryString(),
                    cost);
            }
        }
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }
}
