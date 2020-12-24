/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cadix.core;

import java.io.IOException;
import java.io.StringWriter;
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
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
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

        //cadixcomp can only contain text nodes, or cadix comps
        //non-Cadix comps (including text nodes) are converted to text
        if (context == null) {
            throw new NullPointerException();
        }

        if (!isRendered()) {
            return;
        }

        if (getChildCount() > 0) {
            for (UIComponent child : getChildren()) {
                //UIInstructions are converted to text by me and send this way to JavaScript
                //facelets family is stuff like ui:repeat
                if (child instanceof Cadix || child.getFamily().equals("facelets")) {
                    child.encodeAll(context);
                } else {
                    //capture output of render of children
                    ResponseWriter orgWriter = context.getResponseWriter();

                    StringWriter writer = new StringWriter();

                    try {
                        context.setResponseWriter(context.getRenderKit().createResponseWriter(writer, "text/html", "UTF-8"));
                        super.encodeChildren(context);
                    } finally {
                        if (orgWriter != null) {
                            context.setResponseWriter(orgWriter);
                        }
                    }
                    String output = writer.toString();

                    //act as if a special Cadix comp "_noncadix"  was inserted
                    Map<String, String> reactProps = new HashMap<>();
                    reactProps.put("text", output);
                    String props = JSONValue.toJSONString(reactProps);
                    cadixCreateCall(child, context, props, "_noncadix");
                    cadixActivateCall(child, context);
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
        //A Cadix comp can contain non-Cadix children, but they are not renderered
        //(see encodeChildren)
        //they are lumped together as text

        cadixCreateCall(this, context, props, reactElementType);
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {

        //createElement if not yet done
        //setProperties otherwise
        //if root -> render if not yet exist
        cadixActivateCall(this, context);
        context.getResponseWriter().endElement("span");
    }

    private static void cadixCreateCall(UIComponent c, FacesContext context, String props, String reactElementType) throws IOException {
        //check if we are a Cadix root

        UIComponent root = getRoot(c);

        context.getResponseWriter().startElement("script", c);
        String myClientId = "\"" + c.getClientId() + "\"";
        String parentId = "\"" + getOutputParent(c).getClientId() + "\"";
        String rootClientId = root == null ? myClientId : "\"" + root.getClientId() + "\"";
        String qReactElementType = Character.isUpperCase(reactElementType.charAt(0)) ? reactElementType : "\"" + reactElementType + "\"";
        String qProps = props == null ? "null" : "\"" + JSONValue.escape(props) + "\"";

        context.getResponseWriter().write("cadixCreateComp(" + myClientId + "," + parentId + "," + rootClientId + "," + qProps + "," + qReactElementType + ")");
        context.getResponseWriter().endElement("script");
    }

    private static UIComponent getRoot(UIComponent p) {
        //check if we are a Cadix root

        UIComponent root = null;
        UIComponent parent = getOutputParent(p);
        if (!(parent instanceof Cadix)) {
            root = p;
        }
        while ((p = getOutputParent(p)) != null && root == null) {
            parent = getOutputParent(p);
            if (!(parent instanceof Cadix)) {
                //I have a non Cadix parent , thus I am root
                root = p;
            }
        }
        return root;
    }

    //return the first parent which is an UIOutput
    private static UIComponent getOutputParent(UIComponent p) {

        while ((p = p.getParent()) != null && (p.getFamily().equals("facelets"))) {

        }
        return p;
    }

    private static void cadixActivateCall(UIComponent u, FacesContext context) throws IOException {
        //check if we are a Cadix root

        UIComponent root = getRoot(u);

        context.getResponseWriter().startElement("script", u);
        String myClientId = "\"" + u.getClientId() + "\"";
        String parentId = "\"" + getOutputParent(u).getClientId() + "\"";
        String rootClientId = root == null ? myClientId : "\"" + root.getClientId() + "\"";
        context.getResponseWriter().write("cadixActivateComp(" + myClientId + "," + parentId + "," + rootClientId + ")");
        context.getResponseWriter().endElement("script");
    }

}
