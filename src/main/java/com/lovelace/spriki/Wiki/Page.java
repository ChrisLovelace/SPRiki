package com.lovelace.spriki.Wiki;

import org.commonmark.Extension;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The Page object is created with the url for some .md file
 * It holds important information about the specific page and is passed to the template to render
 * <p>
 * It is created by a Wiki object when a specific page is requested
 * Opens a specified .md file and uses a processor object to process into usable data
 */
public class Page implements Comparable<Page> {

    Logger logger = LoggerFactory.getLogger(Page.class);

    private Path path;
    private String url;
    private boolean isNew;
    private String content;
    private String html;
    private String body;
    private String title;
    private String tags;    // tags are stored as a string of characters seperated by only commas.

    public Page() {
    }

    public Page(Path path, String url) {
        this.path = path;
        this.url = url;
        this.isNew = false;
        this.load();
        this.render();
    }

    //  overloaded constructor will set isNew flag, really only used to match functionality with optional
    //  'new' parameter in Riki, kinda clunky...
    /*  I could remove the if statement in this method, but I will keep it for readability and in case I ever need to be
        explicit with the state of a page.
    *   I don't think isNew is ever set to false, but pages are loaded every time so it shouldn't be necessary.
    * */
    public Page(Path path, String url, boolean isNew) {
        this.path = path;
        this.url = url;
        this.isNew = isNew;
        if (!isNew) {
            this.load();
            this.render();
        }
    }

    public void load() {
        //  open file from this.path
        try {
            this.content = Files.readString(this.path);
        } catch (Exception ex) {
            logger.error("There was an issue loading a file");
        }
    }

    public void render() {
        //  create processor object passing content instance variable
        PageProcessor processor = new PageProcessor(this.content);
        Object[] data = processor.process();

        Map<String, List> metaData = (Map) data[2];

        this.setTitle(metaData.get("title").get(0).toString());
        this.setTags(metaData.get("tags"));
        this.setBody((String) data[1]);
        this.setHTML((String) data[0]);

    }

    public void save(boolean update) {

        //  initialize string that will hold markdown data
        String saveData = "---\r\n";
        //  add title info
        saveData = saveData + "title: " + this.getTitle() + "\r\n";
        //  add tags
        saveData = saveData + "tags:\r\n";
        for (String s : this.getTagsList()) {
            saveData = saveData + "    - " + s + "\r\n";
        }
        saveData = saveData + "---\r\n\r\n";
        saveData += this.body;

        try {
            Files.write(this.path, Collections.singleton(saveData));
        } catch (java.io.IOException ex) {
            logger.error("There was an issue writing to a file.");
        }

        if (update) {
            this.load();
            this.render();
        }
    }

    public String getHTML() {
        return this.html;
    }

    public void setHTML(String html) {
        this.html = html;
    }


    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        if (this.title != null) {
            return this.title;
        }
        return this.url;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return this.body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    //  return tags List, if it is not set: return null
    public List<String> getTagsList() {
        String[] tagValues = this.tags.split(",");
        ArrayList<String> tagsList = new ArrayList<String>(List.of(tagValues));
        return tagsList;
    }

    public String getTags() {
        return this.tags;
    }

    //  set tags as String of tags seperated by ',' character
    public void setTags(String values) {
        this.tags = values;
    }

    public void setTags(List<String> values) {
        String tagString = "";
        for (String s : values) {
            tagString = tagString + s + ",";
        }
        if (tagString.endsWith(",")) {
            tagString = tagString.substring(0, tagString.length() - 1);
        }
        this.tags = tagString;
    }

    public String toString() {
        return this.getUrl();
    }

    @Override
    public int compareTo(Page a) {
        return this.getTitle().compareToIgnoreCase(a.getTitle());
    }

    private class PageProcessor {
        /**
         * The processor handles the processing of file content into metadata and markdown, as well as takes care
         * of the rendering. It is based on the same class from the Riki project
         */

        //input is the text passed into the constructor
        private final String input;
        private String markdown;
        private String meta_raw;
        private String pre;
        private String html;
        private String finalHTML;
        private Map meta;

        private final Parser parser;
        private final HtmlRenderer renderer;

        /**
         * The original project uses lists of functions for pre- and post-processing.
         * The java equivalent that I would use requires an interface and subclasses that overload the desired method
         * You make an array of the superclass type and create the desired objects for each entry.
         * This sounds reasonable as an extension of the functionality, but seeing as the original project only
         * implemented one post processor and no pre-processors, I will just use a function defined in this class
         */

        //  Riki also had a default parameter url_formatter set to none...
        private String wikiLinks(String text) {
            return "";
        }

        public PageProcessor(String text) {

            // These will be used to process the text
            List<Extension> extensions = Arrays.asList(YamlFrontMatterExtension.create(), TablesExtension.create());
            this.parser = Parser.builder()
                    .extensions(extensions)
                    .build();
            this.renderer = HtmlRenderer.builder()
                    .extensions(extensions)
                    .build();

            this.input = text;
        }

        // preProcessor function is for future consideration
        //  void preProcess(){}

        //convert to html
        private void processMarkdown() {
            //  use YamlFrontMatterVisitor to parse metadata
            Node document = this.parser.parse(this.input);
            YamlFrontMatterVisitor visitor = new YamlFrontMatterVisitor();
            document.accept(visitor);
            this.meta = visitor.getData();
            this.html = this.renderer.render(document);

        }

        //  split raw will get the body of the markup from the .md file
        public void splitRaw() {
            this.markdown = (this.input.split("\r\n\r\n", 2))[1];
        }

        //  The process meta method is unnecessary because the metadata is ordered

        //  content postprocessor is unnecessary for now, the link creation function is already handled in the commonmark library

        //private void postProcess() {}

        //  runs full set of processors and returns data.

        // TODO: consider necessity of separate process method, we have many less methods in this class than the original
        private Object[] process() {

            this.splitRaw();
            this.processMarkdown();
            //this.postProcess();

            return new Object[]{this.html, this.markdown, this.meta};

        }


    }
}