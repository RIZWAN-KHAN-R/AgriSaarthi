
package com.agrisaarthi.agri_web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/")
    public String dashboard() {
        return "index";
    }

    @GetMapping("/history")
    public String history() {
        return "history";
    }
}
