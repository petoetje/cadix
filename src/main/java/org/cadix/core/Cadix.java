/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cadix.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.el.ValueExpression;
import javax.faces.application.ResourceDependencies;
import javax.faces.application.ResourceDependency;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.json.simple.JSONValue;

/**
 *
 * @author christo
 */
@ResourceDependencies({
    @ResourceDependency(library = "cadix", name = "cadix.js", target = "head")})
@FacesComponent(createTag = true, tagName = "cadix")
public class Cadix extends UIInput {

    @Override
    public boolean getRendersChildren() {
        return Boolean.FALSE;
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
        super.encodeBegin(context); //To change body of generated methods, choose Tools | Templates.

        //create a element with specified id, to make Mojarra happy
        //if we don't do this then partial updates don't work
        //this also removes the old script call
        context.getResponseWriter().startElement("span", this);
        context.getResponseWriter().writeAttribute("id", getClientId(), null);

        String props = null;
        //collect  attributes and convert to react props
        Map<String, Object> attributes = getAttributes();
        Set<String> passthroughAttributes = getPassThroughAttributes(true).keySet();
        if (attributes != null && !attributes.isEmpty()) {
            Map<String, String> reactProps = new HashMap<>();
            for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
                String attributeName = attribute.getKey();
                String propName = passthroughAttributes.contains(attributeName) ? attributeName : "jsf:" + attributeName; 
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
                        reactProps.put(propName, value);
                    }
                }
            }
            //convert Map to JSON , then to String
            props = JSONValue.toJSONString(reactProps);

        }
        cadixCreateCall(context, props);

    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        //createElement if not yet done
        //setProperties otherwise
        //if root -> render if not yet exist
        cadixActivateCall(context);
        context.getResponseWriter().endElement("span");
    }

    private void cadixCreateCall(FacesContext context, String props) throws IOException {
        //check if we are a Cadix root
        UIComponent p = this;
        UIComponent root = null;
        while ((p = p.getParent()) != null) {
            if (p instanceof Cadix && root == null) {
                //I have a Cadix parent , thus I can not be Cadix root
                root = p;
            }
        }
        context.getResponseWriter().startElement("script", this);
        String myClientId = "\"" + getClientId() + "\"";
        String parentId = "\"" + getParent().getClientId() + "\"";
        String rootClientId = root == null ? myClientId : "\"" + root.getClientId() + "\"";
        String qProps = props == null ? "null" : "\"" + JSONValue.escape(props) + "\"";
        context.getResponseWriter().write("cadixCreateComp(" + myClientId + "," + parentId + "," + rootClientId + "," + qProps + ")");
        context.getResponseWriter().endElement("script");
    }

    private void cadixActivateCall(FacesContext context) throws IOException {
        //check if we are a Cadix root
        UIComponent p = this;
        UIComponent root = null;
        while ((p = p.getParent()) != null) {
            if (p instanceof Cadix && root == null) {
                //I have a Cadix parent , thus I can not be Cadix root
                root = p;
            }
        }

        context.getResponseWriter().startElement("script", this);
        String myClientId = "\"" + getClientId() + "\"";
        String parentId = "\"" + getParent().getClientId() + "\"";
        String rootClientId = root == null ? myClientId : "\"" + root.getClientId() + "\"";
        context.getResponseWriter().write("cadixActivateComp(" + myClientId + "," + parentId + "," + rootClientId + ")");
        context.getResponseWriter().endElement("script");
    }

}
