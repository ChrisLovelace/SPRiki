package com.lovelace.spriki.Wiki;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;

@Controller
public class WikiController {

    Logger logger = LoggerFactory.getLogger(WikiController.class);
    Wiki currentWiki = new Wiki("E:\\Project\\Spring Wiki\\content");

    @GetMapping("/")
    public String home(Model model) {

        logger.trace("A TRACE Message");
        logger.debug("A DEBUG Message");
        logger.info("An INFO Message");
        logger.warn("A WARN Message");
        logger.error("An ERROR Message");

        logger.info("This is the '/' mapping");

        Page page = currentWiki.get("home");
        if (page != null) {
            return display("home", model);
        }
        return "home";
    }

    @GetMapping("/index")
    public String index(Model model) {

        Page[] pages = {};
        try {
            pages = currentWiki.index();
        }catch(Exception e ){
            logger.error("THERE WAS AN IO ERROR");
        }

        model.addAttribute("pages", pages);


        return "index";
    }

    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        logger.warn("favicon.ico was requested.");
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/{url}")
    public String display(@PathVariable(value = "url") String url, Model model) {
        logger.info("The url '" + url + "' was requested.");
        Page page = currentWiki.get_or_404(url);
        if (page == null) {
            logger.warn("A 404 was triggered");
        }

        model.addAttribute("page", page);

        return "page";
    }

    @GetMapping("/edit/{url}")
    public String edit(@PathVariable(value = "url") String url, Model model, RedirectAttributes redirAttrs) {
        logger.info("The url '" + url + "' was passed to the edit page.");

        Page page = currentWiki.get(url);

        // This is when creating a new page, will return null and so a new file is created
        if (page == null) {
            page = currentWiki.getBare(url);
        }

        //redirAttrs.addFlashAttribute("message", "THIS IS A TEST MESSAGE");

        model.addAttribute("page", page);

        return "editor";
    }

    @PostMapping("/savePage")
    public String savePage(@ModelAttribute Page page, Model model) {
        logger.info("A page was saved.");

        //  page will be a new object with only the html, tags, and title, with old url

        Page newPage = currentWiki.get(page.getUrl());

        if (newPage == null) {
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
        logger.info("The createPage GetMapping was requested.");

        //page.setUrl("url");
        model.addAttribute("page", new Page());

        //  This is where validation comes in, need a custom one to validate url isnt taken.

        //  post mapping will need to create a new page object and then save it.

        return "create";
    }

    @PostMapping("/create")
    public String createPageSubmit(@ModelAttribute("page") Page page, BindingResult bindingResult) {

        logger.info("The createPage PostMapping was requested.");

        String url = page.getUrl();

        //  The isValid method checks for validity of the url as a part of the url and as a filename
        //    I did the existence check first originally and it took me an embarrassingly long time to realize
        //    the invalid filename was causing errors

        boolean validFlag = currentWiki.isValid(url);
        if (!validFlag) {
            bindingResult.rejectValue("url", "error.url", "URL is invalid");
            logger.warn("An invalid url was given to the Create page");
        } else if (currentWiki.exists(url)) {
            bindingResult.rejectValue("url", "error.url", "URL is already in use");
            logger.warn("An existing url was given to the Create page");
        }

        //urlValidator.validate(page.getUrl(), bindingResult);

        if (bindingResult.hasErrors()) {
            return "create";
        }

        return "redirect:/edit/" + page.getUrl();

    }

    @GetMapping("/tags")
    public String tags(Model model){

        //Page[] tagged = currentWiki.index_by_tag();
        // This was for a diff page, I fucked up


        model.addAttribute("tags", currentWiki.getTags());


        return "tags.html";
    }

}
