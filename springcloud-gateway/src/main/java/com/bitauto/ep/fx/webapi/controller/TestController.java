package com.bitauto.ep.fx.webapi.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * test Fegin
 */
@RestController
public class TestController {

    @RequestMapping("/fallback")
    public String fallback() {
        System.out.println("1");
        return "fallback";
    }
}
