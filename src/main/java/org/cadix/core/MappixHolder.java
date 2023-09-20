/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.cadix.core;

import jakarta.faces.application.ResourceDependencies;
import jakarta.faces.application.ResourceDependency;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.NamingContainer;
import jakarta.faces.component.UIComponent;
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
    @ResourceDependency(library = "mappix", name = "observer.js", target = "head"),
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
        ACTIVEHOLDER, NAME
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
            //use Id, because I think "this" Java pointer is sometimes re-used , for multiple renders of the same component
            context.getAttributes().put(ContextKeys.ACTIVEHOLDER, getId());
            //todo : start Recoil object
        }
        //if I have Name then store it it context.  A child element might need it to call an updater
        String myName = getName();
        if (myName != null && !myName.isBlank()) {
            context.getAttributes().put(ContextKeys.NAME, myName);
        }

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
        //if "activeId" === my Id then I was the first to start rendering
        //so I am the last to end rendering
        //this means I have to define the Recoil atom (if it does not exist yet), or call the setter
        if (activeId != null && activeId.equals(getId())) {
            context.getAttributes().remove(ContextKeys.ACTIVEHOLDER);
            List<String> splitId = Arrays.asList(getClientId().split(":"));
            String idArr = JSONValue.toJSONString(splitId);
            //if we have a name, then define an atom, if it does not exit yet (atom could have survived complete re-render of entire component)
            //else call setter
            String myName = getName();

            if (myName == null) {
                //I am the last one to render, but I have no name ---> we are partially re-rendering
                //I do need the name of the current atom. Search the JSF tree for a parent with a name
                //Look for the nearest MappixHolder with a name
                UIComponent p;
                while ((p = this.getParent()) != null && myName == null) {
                    if (p instanceof MappixHolder) {
                        myName = ((MappixHolder) p).getName();
                        break;
                    }
                }
            }
            if (myName != null) {
                //setter should have been defined (in a previous render)
                context.getResponseWriter().startElement("script",  this);
                context.getResponseWriter().write("if (typeof window.mappixVals === 'undefined' ) { window.mappixVals = {} } ");
                context.getResponseWriter().write(String.format("mappixSet('%s',JSON.parse(JSON.stringify(getHolder(%s), mappxReplacer)));",myName,idArr));
                context.getResponseWriter().write(String.format("wobs.publish('%s');", myName));
                context.getResponseWriter().endElement("script");
            } else {
                //raise error ?
            }

        } else {
            // no work to do
        }
    }

    @Override
    public boolean getRendersChildren() {
        return Boolean.FALSE;
    }

}
