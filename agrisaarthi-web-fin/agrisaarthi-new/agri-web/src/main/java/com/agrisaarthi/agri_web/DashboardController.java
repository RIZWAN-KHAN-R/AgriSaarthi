
package com.agrisaarthi.agri_web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/")
    public String dashboard() {
        return "index";
    }

    @GetMapping("/assistant")
    public String assistant() {
        return "assistant";
    }

    @GetMapping("/crops")
    public String crops() {
        return "crops";
    }

    @GetMapping("/diseases")
    public String diseases() {
        return "diseases";
    }

    @GetMapping("/weather")
    public String weather() {
        return "weather";
    }

    @GetMapping("/history")
    public String history() {
        return "history";
    }
}
