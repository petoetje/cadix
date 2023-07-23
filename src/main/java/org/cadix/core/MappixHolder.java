/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.cadix.core;

import jakarta.faces.application.ResourceDependencies;
import jakarta.faces.application.ResourceDependency;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.NamingContainer;
import jakarta.faces.component.UIOutput;
import jakarta.faces.context.FacesContext;
import java.io.IOException;
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
@FacesComponent(createTag = true, tagName = "mappixHolder")
public class MappixHolder extends UIOutput implements NamingContainer {

    /**
     * Properties that are tracked by state saving.
     */
    enum PropertyKeys {
        holdertype, name
    }

    enum HolderType {
        MAP, LIST
    }

    enum ContextKeys {
        ACTIVEHOLDER
    }

    public HolderType getHolderType() {
        return (HolderType) getStateHelper().get(PropertyKeys.holdertype);
    }

    public void setHolderType(HolderType ht) {
        getStateHelper().put(PropertyKeys.holdertype, ht);
    }

    public String getName() {
        return (String) getStateHelper().get(PropertyKeys.name);
    }

    public void setName(String name) {
        getStateHelper().put(PropertyKeys.name, name);
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {

        // check if I am the first MappixHolder. If so, set a mark, to know I need to call the
        // callback at the end
        String activeId = (String) context.getAttributes().get(ContextKeys.ACTIVEHOLDER);
        if (activeId == null) {
            //use Id, because I think "this" is sometimes re-used , for multiple renders of the same component
            context.getAttributes().put(ContextKeys.ACTIVEHOLDER, getId());
            //todo : start Recoil object
        }

        //if I have Name then store it it context.  A child element might need it to call an updater
        //super.encodeBegin(context);
        //create a element with specified id, to make Mojarra happy
        //if we don't do this then partial updates don't work
        //this also removes the old script call
        context.getResponseWriter().startElement("span", this);
        context.getResponseWriter().writeAttribute("id", this.getClientId(), null);
        context.getResponseWriter().startElement("script", this);
        List<String> splitId = Arrays.asList(getClientId().split(":"));
        String holderType = "HolderType.list";
        HolderType ht = getHolderType();
        if (ht != null || ht == HolderType.MAP) {
            holderType = "HolderType.map";
        }
        context.getResponseWriter().write(String.format("addHolder(%s,%s)", JSONValue.toJSONString(splitId), holderType));
        context.getResponseWriter().endElement("script");

    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        //super.encodeEnd(context); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        context.getResponseWriter().endElement("span");
        String activeId = (String) context.getAttributes().get(ContextKeys.ACTIVEHOLDER);
        if (activeId != null && activeId.equals(getId())) {
            context.getAttributes().remove(ContextKeys.ACTIVEHOLDER);
            List<String> splitId = Arrays.asList(getClientId().split(":"));
            String idArr = JSONValue.toJSONString(splitId);
            //if we have a name, then define an atom
            //else call setter
            String myName = getName();
            if (myName != null) {
                /**
                 * const fontSizeState = atom({key: 'fontSizeState',default: 14
                 * });
                 */
                context.getResponseWriter().startElement("script", this);
                context.getResponseWriter().write(String.format("const %s = Recoil.atom({key: %s,default: JSON.stringify(getHolder(%s), mappxReplacer)});", myName, JSONValue.toJSONString(myName), idArr));
                context.getResponseWriter().endElement("script");
            } else {

            }
            //temp placeholde
            context.getResponseWriter().startElement("script", this);
            context.getResponseWriter().write("alert('END')");
            context.getResponseWriter().endElement("script");
        }
    }

    @Override
    public boolean getRendersChildren() {
        return Boolean.FALSE;
    }

}
