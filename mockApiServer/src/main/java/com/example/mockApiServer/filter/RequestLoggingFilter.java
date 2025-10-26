package com.example.mockApiServer.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

@Component
public class RequestLoggingFilter implements Filter {
    
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
        
        log.info("{} {} from {} - Content-Length: {}", 
                httpRequest.getMethod(), 
                httpRequest.getRequestURI(), 
                httpRequest.getRemoteAddr(),
                httpRequest.getContentLength());
        
        chain.doFilter(wrappedRequest, response);
    }
}