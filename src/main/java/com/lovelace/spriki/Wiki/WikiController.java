package com.lovelace.spriki.Wiki;

import jakarta.validation.Valid;
import org.commonmark.Extension;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

@Controller

public class WikiController {

    /*
     * These first 2 objects are used to process markdown for the preview function.
     *
     * Unlike the original project, my class for processing pages cannot be used from the controller class.
     * Fortunately the libraries I use are simpler and the basic processing can be done on the fly with these few
     * objects, though it is a bit redundant.
     */

    List<Extension> extensions = Arrays.asList(YamlFrontMatterExtension.create(), TablesExtension.create());
    Parser parser = Parser.builder()
            .extensions(extensions)
            .build();
    HtmlRenderer renderer = HtmlRenderer.builder()
            .extensions(extensions)
            .build();

    Logger logger = LoggerFactory.getLogger(WikiController.class);
    Wiki currentWiki = new Wiki("E:\\Project\\Spring Wiki\\content");


    @GetMapping("/")
    public String home(Model model) {

        Page page = currentWiki.get("home");
        if (page != null) {
            return display("home", model);
        }
        logger.info("No home page was found.");
        return "home";
    }


    @GetMapping("/index")
    public String index(Model model) {

        Page[] pages = {};
        try {
            pages = currentWiki.index();
        } catch (Exception e) {
            logger.error("There was an error loading the index page.");
            return "error";
        }
        model.addAttribute("pages", pages);

        return "index";
    }

    @GetMapping("/page/{url}")
    public String display(@PathVariable(value = "url") String url, Model model) {
        logger.info("The url '" + url + "' was requested.");
        Page page = currentWiki.get_or_404(url);
        if (page == null) {
            logger.warn("404: page not found");
            return "404";
        }

        model.addAttribute("page", page);

        return "page";
    }


    @GetMapping("/edit/{url}")
    public String edit(@PathVariable(value = "url") String url, Model model) {

        logger.info("The page with url " + url + " is being edited.");

        Page page = currentWiki.get(url);

        // This is when creating a new page, will return null and so a new file is created
        if (page == null) {
            page = currentWiki.getBare(url);
        }

        model.addAttribute("page", page);

        return "editor";
    }

    @PostMapping("/preview")
    public ResponseEntity<?> preview(@Valid @RequestBody String data) {

        logger.info("A preview request was made");

        String newData = "";

        try {
            newData = URLDecoder.decode(data, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            logger.warn("There was an issue with decoding!");
            return (ResponseEntity<?>) ResponseEntity.status(500);
        }

        Node document = parser.parse(newData.substring(5));
        YamlFrontMatterVisitor visitor = new YamlFrontMatterVisitor();
        document.accept(visitor);

        String html = renderer.render(document);

        return ResponseEntity.ok(html);
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

        return "redirect:/page/" + page.getUrl();
    }

    @GetMapping("/create")
    public String createPage(Model model) {

        model.addAttribute("page", new Page());

        return "create";
    }

    @PostMapping("/create")
    public String createPageSubmit(@ModelAttribute("page") Page page, BindingResult bindingResult) {

        logger.info("A page is being created.");

        String url = page.getUrl();

        //  The isValid method checks for validity of the url as a part of the url and as a filename

        boolean validFlag = currentWiki.isValid(url);
        if (!validFlag) {
            bindingResult.rejectValue("url", "error.url", "URL is invalid");
            logger.warn("An invalid url was given to the Create page");
        } else if (currentWiki.exists(url)) {
            bindingResult.rejectValue("url", "error.url", "URL is already in use");
            logger.warn("An existing url was given to the Create page");
        }

        if (bindingResult.hasErrors()) {
            return "create";
        }

        return "redirect:/edit/" + page.getUrl();

    }

    //  This mapping will return a page which shows all the tags from the wiki pages in
    //      alphabetical order, the number of pages each shows up in, and link the tag page
    @GetMapping("/tags")
    public String tags(Model model) {

        model.addAttribute("tags", currentWiki.getTags());

        return "tags";
    }

    //  The tag page takes the tag in the url and links all pages that include that tag
    @GetMapping("/tag/{tag}")
    public String tag(@PathVariable(value = "tag") String tag, Model model) {

        TreeMap<String, List<Page>> tags = currentWiki.getTags();

        List pageList = tags.get(tag);

        model.addAttribute("tag", tag);
        model.addAttribute("pageList", pageList);

        return "tag";
    }


    //  Future consideration:
    //  Maybe use a formView object to pass the url rather than Path variables
    @GetMapping("/move/{url}")
    public String move(@PathVariable(value = "url") String url, Model model) {
        logger.info("The move page was requested for url " + url + ".");

        Page page = currentWiki.get_or_404(url);

        model.addAttribute("page", page);

        return "move";
    }

    @PostMapping("/move/{url}")
    public String moveSubmit(@PathVariable(value = "url") String url, @ModelAttribute("page") Page page, BindingResult bindingResult) {



        //  Here we need to get the new URL and save it into a variable
        String newUrl = page.getUrl();

        boolean validFlag = currentWiki.isValid(newUrl);
        if (!validFlag) {
            bindingResult.rejectValue("url", "error.url", "New URL is invalid");
            logger.warn("An invalid url was given to the Move page");
        } else if (currentWiki.exists(newUrl)) {
            bindingResult.rejectValue("url", "error.url", "URL is already in use");
            logger.warn("An existing url was given to the Move page");
        }

        if (bindingResult.hasErrors()) {
            return "move";
        }

        currentWiki.move(url, page.getUrl());
        logger.info("New url " + url + " was given to the page.");

        return "redirect:/page/" + newUrl;

    }

    @GetMapping("/search")
    public String search(Model model) {

        // This is a simple object encapsulating a String object so that I can populate it with a form.
        formView search = new formView();

        model.addAttribute("search", search);

        return "search";
    }

    @PostMapping("/search")
    public String searchSubmit(@ModelAttribute("search") formView search, Model model) {

        Page[] pages = currentWiki.search(search.getText(), search.isFlag());

        model.addAttribute("search", search);
        model.addAttribute("results", pages);

        return "search";
    }

    // This will delete the current page
    @GetMapping("/delete/{url}")
    public String delete(@PathVariable(value = "url") String url, Model model) {


        Page page = currentWiki.get_or_404(url);
        if (!currentWiki.delete(url)){
            return "error";
        }

        logger.info("Page " + url + " was deleted");

        // flash message to page
        String[] message = {"success", "Page " + page.getTitle() + " was deleted."};
        model.addAttribute("message", message);

        return "redirect:/page/home";
    }


}
