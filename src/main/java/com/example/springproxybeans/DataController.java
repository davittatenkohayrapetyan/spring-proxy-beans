package com.example.springproxybeans;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/data")
public class DataController {
    
    private final DataService dataService;
    
    @Autowired
    public DataController(DataService dataService) {
        this.dataService = dataService;
    }
    
    @PostMapping
    public ResponseEntity<String> setData(@RequestBody DataRequest request) {
        dataService.setDataForCurrentRequest(request.getData());
        return ResponseEntity.ok("Data set for current request: " + request.getData());
    }
    
    @GetMapping
    public ResponseEntity<String> getData() {
        if (!dataService.hasDataInCurrentRequest()) {
            return ResponseEntity.ok("No data set for current request");
        }
        return ResponseEntity.ok(dataService.getDataFromCurrentRequest());
    }
    
    public static class DataRequest {
        private String data;
        
        public DataRequest() {}
        
        public String getData() {
            return data;
        }
        
        public void setData(String data) {
            this.data = data;
        }
    }
}