package com.heima.wemedia.service;

import com.heima.wemedia.WemediaApplication;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = WemediaApplication.class)
@RunWith(SpringRunner.class)
class WmNewsAutoScanServiceTest {

    @Autowired
    WmNewsAutoScanService wmNewsAutoScanService;

    @Test
    void autoScanWmNews() throws InvocationTargetException, IllegalAccessException {
        wmNewsAutoScanService.autoScanWmNews(6234);
    }
}