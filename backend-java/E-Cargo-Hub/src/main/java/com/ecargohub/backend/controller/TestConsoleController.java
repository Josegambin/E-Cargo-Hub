package com.ecargohub.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestConsoleController {

    @GetMapping({ "/test-console", "/test-console/" })
    public String testConsole() {
        return "forward:/test-console/index.html";
    }
}
