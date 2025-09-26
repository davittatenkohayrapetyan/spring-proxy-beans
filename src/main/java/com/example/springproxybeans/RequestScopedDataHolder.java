package com.example.springproxybeans;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestScopedDataHolder {
    
    private String data;
    private Long timestamp;
    
    public RequestScopedDataHolder() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public String getRequestInfo() {
        return "Data: " + data + ", Created at: " + timestamp;
    }
}