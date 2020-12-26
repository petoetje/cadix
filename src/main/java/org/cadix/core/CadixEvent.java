/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cadix.core;

import javax.faces.component.UIComponent;
import javax.faces.event.ActionEvent;
import org.json.simple.JSONValue;

/**
 *
 * @author christo
 */
public class CadixEvent extends ActionEvent {
    
    public CadixEvent(UIComponent component) {
        super(component);
    }
    
    // tag : provided by JavaScript caller. Useful to find out who called
    private String tag;
    // JSON repr of all arguments of the called JavaScript function
    private String rawArgs;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getRawArgs() {
        return rawArgs;
    }
    
    public Object getArgs() {
        return JSONValue.parse(getRawArgs());
    }

    public void setRawArgs(String args) {
        this.rawArgs = args;
    }
    
        
}
