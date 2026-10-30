package com.zeroed.app.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {
	@GetMapping({
        "/{path:^(?!api$|assets$|actuator$|error$)[^.]+}",
        "/{path:^(?!api$|assets$|actuator$|error$)[^.]+}/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
