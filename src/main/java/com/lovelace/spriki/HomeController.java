package com.lovelace.spriki;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    Wiki currentWiki = new Wiki("E:\\Project\\Spring Wiki\\content");

    @GetMapping("/")
    public String home(Model model) {
        Page page = currentWiki.get("home");

        model.addAttribute("page", page);

        if (page != null) {
            return "page";
        }
        return "home";
    }

    @GetMapping("/index")
    public String index() {
        return "index";
    }

    @GetMapping("/edit/{url}")
    public String edit(@PathVariable(value="url") String url, Model model, RedirectAttributes redirAttrs) {
        Page page = currentWiki.get(url);

        if(page == null) {
            page = currentWiki.getBare(url);
        }

        //redirAttrs.addFlashAttribute("message", "THIS IS A TEST MESSAGE");

        model.addAttribute("page", page);

        return "editor";
    }

    @PostMapping("/savePage")
    public String savePage(@ModelAttribute Page page, Model model){
        //  page will be a new object with only the html, tags, and title, with old url

        Page newPage = currentWiki.get(page.getUrl());

        if(newPage == null) {
            newPage = currentWiki.getBare(page.getUrl());
        }



        newPage.setTitle(page.getTitle());
        newPage.setBody(page.getBody());
        newPage.setTags(page.getTags());

        newPage.save(true);

        String[] message = {"success", newPage.getTitle() + " was saved."};
        model.addAttribute("message", message);

        model.addAttribute("page", newPage);

        return "page";
    }

    @GetMapping("/create")
    public String createPage(Model model) {

        Page page = new Page();
        //page.setUrl("url");
        model.addAttribute("page", page);

        //  This is where validation comes in, need a custom one to validate url isnt taken.

        //  post mapping will need to create a new page object and then save it.

        return "create";
    }

}
