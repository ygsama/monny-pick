package com.example.demo.controller;

import com.example.demo.service.PromptGenerationService;
import com.example.demo.service.StockAnalysisPrompt;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
public class HelloController {

    @Resource
    PromptGenerationService promptGenerationService;

    // 根据日期，生成prompt
    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE) // 映射根路径
    public String home(
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "510500") String code,
            @RequestParam(required = false, defaultValue = "101") Integer kt) throws ParseException {
        String stockCode = code == null ? "510500" : code;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date targetDate = date == null ? new Date() : sdf.parse(date);

        StockAnalysisPrompt prompt =
                promptGenerationService.generateSinglePrompt(stockCode, targetDate, kt);

        return prompt.getPrompt();
    }
}
