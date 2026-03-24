package com.travelate.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    // 处理根路径请求
    @GetMapping("/")
    public String home() {
        return "Travelate Backend API Server";
    }

    // 处理favicon.ico请求
    @GetMapping("/favicon.ico")
    public void favicon() {
        // 空方法，返回204 No Content
    }
}
