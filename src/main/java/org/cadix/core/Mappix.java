/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.cadix.core;

import jakarta.faces.application.ResourceDependencies;
import jakarta.faces.application.ResourceDependency;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIOutput;
import jakarta.faces.context.FacesContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.simple.JSONValue;

/**
 *
 * @author christo
 */
@ResourceDependencies({
    @ResourceDependency(library = "mappix", name = "mappix.js", target = "head"),
    @ResourceDependency(library = "jakarta.faces", name = "faces.js", target = "head") // Required for faces.ajax.request.
})
@FacesComponent(createTag = true, tagName = "mappix")
public class Mappix extends UIOutput {

    /**
     * Properties that are tracked by state saving.
     */
    enum PropertyKeys {
        key
    }

    /**
     * Returns an alternative key, to store in Map, instead of clientId
     *
     * @return String
     */
    public Object getKey() {
        return getStateHelper().get(PropertyKeys.key);
    }

    /**
     * Sets an alternative key, to store in Map, instead of clientId
     *
     * @param key String
     */
    public void setKey(String key) {
        getStateHelper().put(PropertyKeys.key, key);
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        //super.encodeBegin(context);
        //create a element with specified id, to make Mojarra happy
        //if we don't do this then partial updates don't work
        //this also removes the old script call
        context.getResponseWriter().startElement("span", this);
        context.getResponseWriter().writeAttribute("id", this.getClientId(), null);
        context.getResponseWriter().startElement("script", this);
        List<String> splitId = Arrays.asList(getClientId().split(":"));
        if (getKey() != null) {
            splitId.set(splitId.size() - 1, JSONValue.toJSONString(getKey()));
        }

        context.getResponseWriter().write(String.format("addValue(%s,%s)", JSONValue.toJSONString(splitId), JSONValue.toJSONString(getValue())));
        context.getResponseWriter().endElement("script");

    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        //super.encodeEnd(context); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        context.getResponseWriter().endElement("span");
    }

}
