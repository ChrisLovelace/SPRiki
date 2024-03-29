package com.lovelace.spriki.Wiki;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This will handle processing of pages
 */

public class Wiki {

    Logger logger = LoggerFactory.getLogger(Wiki.class);

    //root will be the directory path, used to find files and such
    private final Path root;
    private static final String URL_PATTERN = "^[a-zA-Z0-9]+([\\-\\._][a-zA-Z0-9]+)*([\\/]?[a-zA-Z0-9]+[\\-\\._\\+~#%&=:@]?)*$";

    public Wiki(String root) {
        this.root = Paths.get(root);
    }

    //Should return the path to the file associated with the given URL.
    public Path path(String url) {
        return Paths.get(this.root + "\\" + url + ".md");
    }

    // This method was in the original Wiki project, it will see if the given URL already matches an existing file.
    public boolean exists(String url) {
        Path path = this.path(url);
        return Files.exists(path);
    }

    //  This method is used for URL validation, ensures that the given URL is valid before creating any files.
    //  Uses regex to ensure that it can be used in the url and as a filename.
    public boolean isValid(String url) {

        boolean matchFlag = url.matches(URL_PATTERN);

        String isValid = matchFlag ? "valid" : "invalid";
        logger.info("URL \"" + url + "\" is " + isValid);

        return matchFlag;
    }

    //  will return page object
    public Page get(String url) {
        Path path = this.path(url);
        if (this.exists(url)) {
            logger.info("Returning path \"" + path.toString() + "\"");
            return new Page(path, url);
        }
        logger.warn("Requested path \"" + path.toString() + "\" does not exist");
        return null;
    }

    public Page get_or_404(String url) {
        Page page = this.get(url);
        if (page != null) {
            return page;
        }

        logger.error("404 error!");

        return null;
    }

    public Page getBare(String url) {
        Path path = this.path(url);
        if (this.exists(url)) {
            return null;
        }
        return new Page(path, url, true);
    }


    public void move(String url, String newUrl) {
        Path source = Paths.get(this.root + "\\" + url + ".md");
        Path target = Paths.get(this.root + "\\" + newUrl + ".md");
        Path root = this.root.normalize();


        //find longest common prefix
        Path common = root.resolve(target.normalize());


        if (common.getNameCount() < root.getNameCount()) {
            //  og raises a runtime error

            logger.error("Runtime error when finding common prefix");

            logger.info("common name count: " + common.getNameCount());
            logger.info("root name count: " + root.getNameCount());
            return;
        }
        //  create a folder if it does not exist
        Path dir = target.getFileName();
        logger.info("Dir Path = " + dir.toString());


        if (!Files.exists(dir)) {
            logger.info("file does not exist, creating");
            try {
                Files.createDirectories(dir);
            } catch (java.io.IOException ex) {
                logger.error("There was an issue when creating a directory");
            }
        }
        try {
            Files.move(source, target);
            logger.info("Attempting to move source file to target");
        } catch (java.io.IOException Ex) {
            logger.error("There was an issue when moving a file");
        }

    }

    public boolean delete(String url) {
        Path path = this.path(url);
        try {
            Files.deleteIfExists(path);
        } catch (java.io.IOException Ex) {
            logger.error("There was an issue when deleting a file");
            return false;
        }
        return true;
    }

    public Page[] index() throws IOException {
        List<Page> pages = new ArrayList<Page>();
        List<Path> pagesList;

        Path absRoot = root.toAbsolutePath();

        try (Stream<Path> walk = Files.walk(absRoot)) {
            pagesList = walk.filter(Files::isRegularFile)
                    .collect(Collectors.toList());

        }

        String absRootString = absRoot.toString();

        for (Path filePath : pagesList) {
            String filePathString = filePath.toString();
            String url = filePathString.substring(31);
            url = url.replace(".md", "");
            Page p = new Page(filePath, url);
            pages.add(p);
        }

        Collections.sort(pages);

        Page[] pageArray = new Page[pages.size()];
        pageArray = pages.toArray(pageArray);

        return pageArray;
    }

    public TreeMap<String, List<Page>> getTags() {
        Page[] pages = {};
        try {
            pages = this.index();
        } catch (IOException ex) {
            logger.error("Exception in getTags: " + ex);
        }

        /*
         * The original used a python dictionary for this section
         * They would use the .get function to check if the tag is already in the dictionary, and append pages
         * to the list in the key value pair to make a list of all pages that have the tag.
         * I used a TreeMap to ensure the results are displayed in alphabetical order.
         */
        TreeMap<String, List<Page>> tags = new TreeMap();

        for (Page page : pages) {
            String[] pageTags = page.getTags().split(",");
            for (String tag : pageTags) {
                tag = tag.strip();
                if (tag.equals("")) {
                    continue;
                } else if (tags.containsKey(tag)) {
                    tags.get(tag).add(page);
                } else {
                    List<Page> p = new ArrayList<>();
                    p.add(page);
                    tags.put(tag, p);
                }

            }
        }

        return tags;
    }

    public Page[] search(String term, boolean ignoreCase) {

        Page[] pages = new Page[0];
        try {
            pages = this.index();
        } catch (IOException ex) {
            logger.error("Unhandled IOException");
            return null;
        }

        List<Page> matches = new ArrayList<>();

        Pattern pattern = ignoreCase ? Pattern.compile(term, Pattern.CASE_INSENSITIVE) : Pattern.compile(term);

        for (Page page : pages) {
            String content = page.getTitle() + "|" + page.getTags() + "|" + page.getBody();
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                matches.add(page);
            }
        }

        // Creating an array of pages from the ArrayList

        Collections.sort(matches);

        Page[] pageArray = new Page[matches.size()];
        pageArray = matches.toArray(pageArray);

        return pageArray;

    }

}
