package com.example.springproxybeans;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataController.class)
public class DataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataService dataService;

    @Test
    public void testSetData() throws Exception {
        mockMvc.perform(post("/api/data")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"data\":\"test-value\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Data set for current request: test-value"));
    }

    @Test
    public void testGetDataWhenNoDataSet() throws Exception {
        when(dataService.hasDataInCurrentRequest()).thenReturn(false);

        mockMvc.perform(get("/api/data"))
                .andExpect(status().isOk())
                .andExpect(content().string("No data set for current request"));
    }

    @Test
    public void testGetDataWhenDataExists() throws Exception {
        when(dataService.hasDataInCurrentRequest()).thenReturn(true);
        when(dataService.getDataFromCurrentRequest()).thenReturn("Data: test-value, Created at: 1234567890");

        mockMvc.perform(get("/api/data"))
                .andExpect(status().isOk())
                .andExpect(content().string("Data: test-value, Created at: 1234567890"));
    }
}