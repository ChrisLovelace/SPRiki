package com.lovelace.spriki;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    Wiki currentWiki = new Wiki("E:\\Project\\Spring Wiki\\content");

    @GetMapping("/")
    public String home(Model theModel) {
        Page page = currentWiki.get("home");

        theModel.addAttribute("page", page);

        System.out.println("This is page.html: " + page.getHTML());

        if (page != null) {
            //  TODO: OK, og uses display here and render template underneath, not sure what the equiv would be
            System.out.println("Page not null lel");
            return "page";
        }
        return "home";
    }

    @GetMapping("/index")
    public String index() {
        return "index";
    }

}
