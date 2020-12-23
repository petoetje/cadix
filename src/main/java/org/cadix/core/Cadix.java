/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cadix.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.el.ValueExpression;
import javax.faces.application.ResourceDependencies;
import javax.faces.application.ResourceDependency;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.render.Renderer;

import org.json.simple.JSONValue;

/**
 *
 * @author christo
 */
@ResourceDependencies({
    @ResourceDependency(library = "cadix", name = "cadix.js", target = "head")})
@FacesComponent(createTag = true, tagName = "cadix")
public class Cadix extends UIInput {

    public Cadix() {
        setRendererType(null);
    }

    @Override
    public boolean getRendersChildren() {
        //return TRUE, because we want to filter out text nodes
        return Boolean.TRUE;

    }

    @Override
    public void encodeChildren(FacesContext context) throws IOException {

        if (context == null) {
            throw new NullPointerException();
        }

        if (!isRendered()) {
            return;
        }

        if (getRendererType() != null) {
            Renderer renderer = getRenderer(context);
            if (renderer != null) {
                renderer.encodeChildren(context, this);
            }
            // We've already logged for this component
        } else if (getChildCount() > 0) {
            for (UIComponent child : getChildren()) {
                //UIInstructions are converted to text by me and send this way to JavaScript
                if (!child.getClass().getSimpleName().equals("UIInstructions")) {
                    child.encodeAll(context);
                }
            }
        }
    }

    // 1. Check if I am a Cadix root. If not, find my root.  If not found, then become a root
    //  If I am a root, then check if the datastructure "cadixmap" is below my data node. Create if needed
    // root -> create dom node if needed
    //  ~~Remove child props from cadix map (will be re-created)~~ TOCH, verwijder gewoon alles (JSF re-render)
    //  bvb : wat als in nieuwe render minder children ?
    // 2. Lookup myself in root map. If not found, insert myself via CreateComponent, below parent, at right child place.
    //    If found, could still need to move ...
    //     Use CreateComponent or store class + props ? Finally, recursive create component
    //      We need to be able to call setProps 
    // 3. Update properties
    // 4. after render children : React render if fresh created Dom node
    //
    // Root must render at encodeEnd
    @Override
    public void encodeBegin(FacesContext context) throws IOException {

        //create a element with specified id, to make Mojarra happy
        //if we don't do this then partial updates don't work
        //this also removes the old script call
        context.getResponseWriter().startElement("span", this);
        context.getResponseWriter().writeAttribute("id", getClientId(), null);

        //we still want a standard JSF input field, to be able to use POST
        //this create an html input element
        // - which has name "clientid"
        // - participates in POST
        // - has a value, as used by JSF
        context.getResponseWriter().startElement("input", this);
        context.getResponseWriter().writeAttribute("id", getClientId() + "-input", null);
        context.getResponseWriter().writeAttribute("name", getClientId(), null);
        context.getResponseWriter().writeAttribute("value", getValue(), null);
        context.getResponseWriter().writeAttribute("hidden", "true", null);
        context.getResponseWriter().endElement("input");
        String props = null;
        String reactElementType = "div";
        //collect  attributes and convert to react props
        Map<String, Object> attributes = getAttributes();

        if (attributes != null && !attributes.isEmpty()) {
            Map<String, String> reactProps = new HashMap<>();
            for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
                String attributeName = attribute.getKey();
                if (attributeName.startsWith("react_")) {
                    String propName = attributeName.substring(6);
                    Object attributeValue = attribute.getValue();
                    if (attributeValue != null) {
                        String value = null;
                        if (attributeValue instanceof ValueExpression) {
                            Object expressionValue = ((ValueExpression) attributeValue).getValue(context.getELContext());
                            if (expressionValue != null) {
                                value = expressionValue.toString();
                            }
                        } else {
                            value = attributeValue.toString();
                        }
                        if (value != null) {
                            //special attribute for type of react component
                            if (attributeName.equals("react_elementtype")) {
                                reactElementType = value;
                            } else {
                                reactProps.put(propName, value);
                            }
                        }
                    }
                }
            }
            //convert Map to JSON , then to String
            props = JSONValue.toJSONString(reactProps);

        }
        //keep children in order
        Map<String, String> children = new LinkedHashMap<>();
        for (UIComponent child : getChildren()) {
            if (child.getClass().getSimpleName().equals("UIInstructions")) {
                children.put(child.getClientId(), child.toString());
            } else {
                children.put(child.getClientId(), null);
            }

        }

        cadixCreateCall(context, props, reactElementType, JSONValue.toJSONString(children));
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {

        //createElement if not yet done
        //setProperties otherwise
        //if root -> render if not yet exist
        cadixActivateCall(context);
        context.getResponseWriter().endElement("span");
    }

    private void cadixCreateCall(FacesContext context, String props, String reactElementType, String children) throws IOException {
        //check if we are a Cadix root

        UIComponent root = getRoot();

        context.getResponseWriter().startElement("script", this);
        String myClientId = "\"" + getClientId() + "\"";
        String parentId = "\"" + getParent().getClientId() + "\"";
        String rootClientId = root == null ? myClientId : "\"" + root.getClientId() + "\"";
        String qReactElementType = Character.isUpperCase(reactElementType.charAt(0)) ? reactElementType : "\"" + reactElementType + "\"";
        String qProps = props == null ? "null" : "\"" + JSONValue.escape(props) + "\"";
        String qChildren = children == null ? "null" : "\"" + JSONValue.escape(children) + "\"";
        context.getResponseWriter().write("cadixCreateComp(" + myClientId + "," + parentId + "," + rootClientId + "," + qProps + "," + qReactElementType + "," + qChildren + ")");
        context.getResponseWriter().endElement("script");
    }

    private UIComponent getRoot() {
        //check if we are a Cadix root
        UIComponent p = this;
        UIComponent root = null;
        if (!(p.getParent() instanceof Cadix)) {
            root = p;
        }
        while ((p = p.getParent()) != null && root == null) {
            if (!(p.getParent() instanceof Cadix)) {
                //I have a non Cadix parent , thus I am root
                root = p;
            }
        }
        return root;
    }

    private void cadixActivateCall(FacesContext context) throws IOException {
        //check if we are a Cadix root

        UIComponent root = getRoot();

        context.getResponseWriter().startElement("script", this);
        String myClientId = "\"" + getClientId() + "\"";
        String parentId = "\"" + getParent().getClientId() + "\"";
        String rootClientId = root == null ? myClientId : "\"" + root.getClientId() + "\"";
        context.getResponseWriter().write("cadixActivateComp(" + myClientId + "," + parentId + "," + rootClientId + ")");
        context.getResponseWriter().endElement("script");
    }

}
