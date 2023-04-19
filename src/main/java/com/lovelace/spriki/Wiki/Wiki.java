package com.lovelace.spriki.Wiki;

import jakarta.validation.constraints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This will handle processing of pages
 */

public class Wiki {

    Logger logger = LoggerFactory.getLogger(Wiki.class);

    //root will be the directory path, used to find files and such
    private Path root;
    //@Pattern(regexp = "^[a-zA-Z0-9]+([\\-\\._][a-zA-Z0-9]+)*([\\/]?[a-zA-Z0-9]+[\\-\\._\\+~#%&=:@]?)*$")
    private static final String URL_PATTERN = "^[a-zA-Z0-9]+([\\-\\._][a-zA-Z0-9]+)*([\\/]?[a-zA-Z0-9]+[\\-\\._\\+~#%&=:@]?)*$";
    //public boolean

    public Wiki(String root) {
        this.root = Paths.get(root);
    }

    //Should return the path to the
    public Path path(String url) {
        // returns a path object pointing to fil
        return Paths.get(this.root + "\\" + url + ".md");
    }

    // This method was in the original Wiki project, it will see if the given URL already matches an existing file.
    // Here it is also used for validation, to protect against duplicates.
    public boolean exists(String url) {
        Path path = this.path(url);
        return Files.exists(path);
    }

    //  This method is used for URL validation, ensures that the given URL is valid before creating any files.
    //  Uses regex to ensure that it can be used in the url and as a filename.
    public boolean isValid(String url) {
        System.out.print("\nThis is the url: " + url + "\n It was ");
        boolean matchFlag = url.matches(URL_PATTERN);
        System.out.println(matchFlag);
        return matchFlag;
    }

    //  will return page object
    public Page get(String url) {
        Path path = this.path(url);
        logger.info("Page get(String url); This path was created: " + path.toString());
        if (this.exists(url)) {
            return new Page(path, url);
        }
        return null;
    }

    public Page get_or_404(String url) {
        Page page = this.get(url);
        if (page != null) {
            return page;
        }
        // TODO: !!!  abort(404) was used here in flask, FIX !!!
        logger.error("get_or_404(String url); 404 error!");
        return null;
    }

    //  original returned a boolean and Page object
    public Page getBare(String url) {
        Path path = this.path(url);
        if (this.exists(url)) {
            //  TODO: og returns false
            return null;
        }
        return new Page(path, url, true);
    }


    //  TODO: make sure the .md part works with this method
    public void move(String url, String newUrl) {
        Path source = Paths.get(this.root + url + ".md");
        Path target = Paths.get(this.root + newUrl + ".md");
        Path root = this.root.normalize();
        //find longest common prefix
        Path common = root.relativize(target.normalize());
        if (common.getNameCount() < root.getNameCount()) {
            // TODO: add logging
            //  og riases a runtime error
            System.out.println("Wiki class, move method: nameCount");
            return;
        }
        //  create a folder if it does not exist
        Path dir = target.getFileName();
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (java.io.IOException ex) {
                // TODO: add logging
                System.out.println("Wiki class, move method: fucked up creating a dir");
            }
        }
        try {
            Files.move(source, target);
        } catch (java.io.IOException Ex) {
            // TODO: add logging
            System.out.println("Wiki class, move method: fucked up moving file");
        }

    }

    public boolean delete(String url) {
        Path path = this.path(url);
        try {
            Files.deleteIfExists(path);
        } catch (java.io.IOException Ex) {
            // TODO: add logging, check for working
            System.out.println("Wiki class, delete method: fucked up deleting a file");
        }
        return true;
    }

}
