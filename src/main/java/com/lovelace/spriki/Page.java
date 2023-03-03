package com.lovelace.spriki;

import org.commonmark.Extension;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

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
public class Page {
    private Path path;
    private String url;
    private boolean isNew;
    private String content;
    private String html;
    //  TODO: __html__ ... Not sure of the use so will add later
    private String title;
    //  TODO: Not sure, but seems tags are one string seperated by ',' character
    private List tags;

    //  TODO: OrderedDict was used on the original, HashMap will be the stand in until it causes an issue
    private HashMap<String, Object> meta;


    public Page(Path path, String url) {
        this.path = path;
        this.url = url;
        this.isNew = false;
        this.meta = new HashMap();
        this.load();
        this.render();
    }

    //  overloaded constructor will set isNew flag, really only used to match functionality with optional
    //  'new' parameter in Riki, kinda clunky...
    public Page(Path path, String url, boolean isNew) {
        this.path = path;
        this.url = url;
        this.isNew = isNew;
        if (!isNew) {
            this.load();
            this.render();
        }
    }
    //  TODO: python __repr__ equivalent?

    //  TODO: implement
    public void load() {
        //  open file from this.path, r only, with encoding???
        try {
            this.content = Files.readString(this.path);
        } catch (Exception ex) {
            System.out.println("Page class, load method, issue loading file");
        }

    }

    //  TODO: implement
    public void render() {
        //  make processor object passing content instance variable
        PageProcessor processor = new PageProcessor(this.content);
        Object[] data = processor.process();

        Map<String, List> metaData = (Map)data[2];

        this.title = metaData.get("title").get(0).toString();
        this.tags = metaData.get("tags");

        this.html = (String)data[0];

        System.out.println("HTML: " + this.html);

        //  use processor.process() to set _html, body, and _meta instance varibles

    }

    //  TODO: implement
    public void save(boolean update) {
        //  Check that directory from this.path exists, if not make it

        //  open file at this.path, w only, write line by line from this._meta.items()??, append extra

        //  if update, default true, this.load and this.render again...

    }

    public HashMap<String, Object> getMeta() {
        return this.meta;
    }

    //  TODO: update when the return object is identified
    public Object getItem(String name) {
        return this.meta.get(name);
    }

    //  TODO: update when the return object is identified
    public void setItem(String name, Object value) {
        this.meta.put(name, value);
    }

    public String getHTML() {
        System.out.println("This is getHTML");
        return this.html;
    }

    //  TODO: __html__ getter method?

    public String getTitle() {
        if (this.title != null) {
            return this.title;
        }
        return this.url;
    }

    public void setTitle(String value) {
        this.title = value;
    }

    //  return tags List, if it is not set return null
    public List getTags() {
        return this.tags;
    }

    //  set tags as String of tags seperated by ',' character
    /*public void setTags(String[] values) {
        this.tags = values;
    }*/

    private class PageProcessor {
        /**
         * The processor handles the processing of file content into metadata and markdown, as well as takes care
         *  of the rendering. It is based on the same class from the Riki project
         */
        // make markdown object? for processing
        //input is the text passed into the constructor
        private String input;
        private String markdown;
        private String meta_raw;
        private String pre;
        private String html;
        private String finalHTML;
        private Map meta;

        private Parser parser;
        private HtmlRenderer renderer;

        /**
         * The original project uses lists of functions for pre- and post-processing.
         * The java equivalent that I would use requires an interface and subclasses that overload the desired method
         * You make an array of the superclass type and create the desired objects for each entry.
         * This sound reasonable as an extension of the functionality, but seeing as the original project only
         * implemented one post processor and no pre-processors, I will just use a function defined in this class
         */

        //  TODO: Riki also had a default parameter url_formatter set to none...
        private String wikiLinks (String text) {
            return "";
        }

        public PageProcessor(String text) {

            //  TODO: initialize markdown object, this will use commonMark and will be saved into instance variables.

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
        private void processMarkdown(){
            //  use YamlFrontMatterVisitor to parse metadata
            Node document = this.parser.parse(this.input);
            YamlFrontMatterVisitor visitor = new YamlFrontMatterVisitor();
            document.accept(visitor);
            this.meta = visitor.getData();
            this.html = this.renderer.render(document);

        }

        //  split raw is unnecessary because we do not need to seperate the meta data to use it

        //  process meta is unecessary because the meta data is ordered

        //  content postprocessor
        private void postProcess() {
            //  TODO: implement; May be unnecessary
            //  Post processor in og was used for making links from markdown, may have a tool in the CommonMark library
        }

        //  runs full set of processors and returns data...
        //  OG returned multiple values that are immediately used for instance variables
        //      so maybe return with array...

        //  TODO: determine necessity of seperate process method, we have many less methods in this class than the og
        private Object[] process () {

            this.processMarkdown();
            //this.postProcess();

            return new Object[]{this.html, this.markdown, this.meta};

        }

    }
}