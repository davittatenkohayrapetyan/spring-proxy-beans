package com.example.springproxybeans;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataService {
    
    private final RequestScopedDataHolder requestScopedDataHolder;
    
    @Autowired
    public DataService(RequestScopedDataHolder requestScopedDataHolder) {
        this.requestScopedDataHolder = requestScopedDataHolder;
    }
    
    public void setDataForCurrentRequest(String data) {
        requestScopedDataHolder.setData(data);
    }
    
    public String getDataFromCurrentRequest() {
        return requestScopedDataHolder.getRequestInfo();
    }
    
    public boolean hasDataInCurrentRequest() {
        return requestScopedDataHolder.getData() != null;
    }
}