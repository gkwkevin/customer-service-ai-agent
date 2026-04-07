package com.example.ticket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/ticket/list")
    public String list() {
        return "ticket/list";
    }
}
