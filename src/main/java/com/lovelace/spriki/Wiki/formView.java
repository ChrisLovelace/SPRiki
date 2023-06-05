package com.lovelace.spriki.Wiki;

/**
 * The formView object will be used to transfer data from the view to the controller that is not already encapsulated in
 *  an object. Usually this is a simple string, and using this object adds reusability for any case where I need a String
 *  from a form.
 */

public class formView {

    private String text = null;

    private boolean flag = true;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
}
