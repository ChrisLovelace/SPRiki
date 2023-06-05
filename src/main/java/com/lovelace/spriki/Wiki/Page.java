package com.lovelace.spriki.Wiki;

import org.commonmark.Extension;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.File;
import java.io.FileWriter;
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
    private Path path;
    private String url;
    private boolean isNew;
    private String content;
    private String html;
    private String body;
    //  TODO: __html__ ... Not sure of the use so will add later
    private String title;
    //  TODO: Not sure, but seems tags are one string seperated by ',' character
    //private List<String> tags;
    private String tags;

    //  TODO: OrderedDict was used on the original, HashMap will be the stand in until it causes an issue
    private HashMap<String, Object> meta;

    public Page() {
    }

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

        Map<String, List> metaData = (Map) data[2];

        this.setTitle(metaData.get("title").get(0).toString());
        this.setTags(metaData.get("tags"));
        this.setBody((String) data[1]);
        this.setHTML((String) data[0]);

        System.out.println("HTML: " + this.html);

        //  use processor.process() to set _html, body, and _meta instance varibles

    }

    //  TODO: implement
    public void save(boolean update) {
        //  Check that directory from this.path exists, if not make it
        Path dir = this.path.getFileName();
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (java.io.IOException ex) {
                //  TODO: logging
                System.out.println("Page class, save method: fucked up creating a dir");
            }
        }

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
            System.out.println("\n\n\nSave method fuckup with writing file\n\n");
        }

        if (update) {
            this.load();
            this.render();
        }


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
        System.out.println("This is getHTML: " + this.html);
        return this.html;
    }

    public void setHTML(String value) {
        this.html = value;
    }

    //  TODO: __html__ getter method?

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String value) {
        this.url = value;
    }

    public String getTitle() {
        if (this.title != null) {
            return this.title;
        }
        return this.url;
    }

    public void setTitle(String value) {
        this.title = value;
    }

    public String getBody() {
        return this.body;
    }

    public void setBody(String value) {
        this.body = value;
    }

    //  return tags List, if it is not set return null
    public List<String> getTagsList() {
        String[] tagValues = this.tags.split(",");
        ArrayList<String> tagsList = new ArrayList<String>(List.of(tagValues));
        return tagsList;
    }


    public String getTags() {
        return this.tags;
    }
    /*TODO remove?
       public String getTags(){
        String tagString = "";
        for (String s : this.tags) {
            tagString = tagString + s + ",";
        }
        if (tagString.endsWith(",")){
            tagString = tagString.substring(0, tagString.length() - 1);
        }
        return tagString;
    }*/

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
        // make markdown object? for processing
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
         * This sound reasonable as an extension of the functionality, but seeing as the original project only
         * implemented one post processor and no pre-processors, I will just use a function defined in this class
         */

        //  TODO: Riki also had a default parameter url_formatter set to none...
        private String wikiLinks(String text) {
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
            System.out.print(this.input + "\n");
            this.markdown = (this.input.split("\r\n\r\n", 2))[1];
        }

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
        private Object[] process() {

            this.splitRaw();
            this.processMarkdown();
            //this.postProcess();

            return new Object[]{this.html, this.markdown, this.meta};

        }

    }
}