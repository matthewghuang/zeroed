package com.zeroed.app.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {
	@RequestMapping({
        "/{path:^(?!api$|error$)[^.]+}",
        "/{path:^(?!api$|error$)[^.]+}/**"
    })
    public String forward(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Do not forward API, management, or error endpoints
        if (path.startsWith("/api/")
                || path.equals("/error")) {
            return null;
        }

        // Do not forward static asset requests
        String lastSegment = path.substring(path.lastIndexOf('/') + 1);
        if (lastSegment.contains(".")) {
            return null;
        }

        return "forward:/index.html";
    }
}
