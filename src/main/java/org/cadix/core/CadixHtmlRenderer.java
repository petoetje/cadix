/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.cadix.core;

import com.sun.faces.renderkit.html_basic.HtmlBasicInputRenderer;
import jakarta.el.ValueExpression;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.event.PhaseId;
import jakarta.faces.render.FacesRenderer;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONValue;

@FacesRenderer(componentFamily = "org.cadix", rendererType = "CadixHTML")
public class CadixHtmlRenderer extends HtmlBasicInputRenderer {

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {

        //cadixcomp can only contain text nodes, or cadix comps
        //non-Cadix comps (including text nodes) are converted to text
        if (context == null) {
            throw new NullPointerException();
        }

        if (!component.isRendered()) {
            return;
        }

        if (component.getChildCount() > 0) {
            for (UIComponent child : component.getChildren()) {
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
                        super.encodeChildren(context, component);
                    } finally {
                        if (orgWriter != null) {
                            context.setResponseWriter(orgWriter);
                        }
                    }
                    String output = writer.toString();

                    cadixCreateCall(child, context, null, "span", output, null, null);
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
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {

        //Cadix renderer can obly render Cadix comps ...
        var cadixComp = (Cadix) component;

        super.encodeBegin(context, component);

        //create a element with specified id, to make Mojarra happy
        //if we don't do this then partial updates don't work
        //this also removes the old script call
        context.getResponseWriter().startElement("span", component);
        context.getResponseWriter().writeAttribute("id", component.getClientId(), null);

        //we still want a standard JSF input field, to be able to use POST
        //this create an html input element
        // - which has name "clientid"
        // - participates in POST
        // - has a value, as used by JSF
        //Strange : passThroughAttributes are written to DOM, by the following code
        //But we don't want them for the input element
        //So remove them first, then re-add them after input elem is written
        Map<String, Object> orgAttributes = new HashMap(component.getPassThroughAttributes());
        component.getPassThroughAttributes().clear();
        context.getResponseWriter().startElement("input", component);
        context.getResponseWriter().writeAttribute("id", component.getClientId() + "-input", null);
        context.getResponseWriter().writeAttribute("name", component.getClientId(), null);
        context.getResponseWriter().writeAttribute("value", cadixComp.getValue(), null);
        context.getResponseWriter().writeAttribute("hidden", "true", null);
        context.getResponseWriter().endElement("input");
        String props = null;
        String reactElementType = "div";
        //collect  attributes and convert to react props
        component.getPassThroughAttributes().putAll(orgAttributes);
        Map<String, Object> attributes = component.getPassThroughAttributes();
        if (attributes != null && !attributes.isEmpty()) {
            Map<String, String> reactProps = new HashMap<>();
            for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
                String attributeName = attribute.getKey();
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
                        if (attributeName.equals("p_elementtype")) {
                            reactElementType = value;
                        } else {
                            reactProps.put(attributeName, value);
                        }
                    }

                }
            }
            //convert Map to JSON , then to String
            props = JSONValue.toJSONString(reactProps);

        }

        String execute = cadixComp.resolveClientIds(context, cadixComp.getExecute());
        String render = cadixComp.resolveClientIds(context, cadixComp.getRender());
        //A Cadix comp can contain non-Cadix children, but they are not renderered
        //(see encodeChildren)
        //they are lumped together as text
        cadixCreateCall(cadixComp, context, props, reactElementType, null, execute, render);
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {

        //Cadix renderer can obly render Cadix comps ...
        var cadixComp = (Cadix) component;

        //createElement if not yet done
        //setProperties otherwise
        //if root -> render if not yet exist
        cadixActivateCall(cadixComp, context);
        context.getResponseWriter().endElement("span");
        super.encodeEnd(context, component);
    }

    private static void cadixCreateCall(UIComponent c, FacesContext context, String props, String reactElementType, String innerHtml, String execute, String render) throws IOException {
        //check if we are a Cadix root

        UIComponent root = getRoot(c);
//Strange : passThroughAttributes are written to DOM, by the following code
        //But we don't want them for the input element
        //So remove them first, then re-add them after input elem is written
        Map<String, Object> orgAttributes = new HashMap(c.getPassThroughAttributes());
        c.getPassThroughAttributes().clear();
        context.getResponseWriter().startElement("script", c);
        c.getPassThroughAttributes().putAll(orgAttributes);
        String myClientId = "\"" + c.getClientId() + "\"";
        String parentId = "\"" + getOutputParent(c).getClientId() + "\"";
        String rootClientId = root == null ? myClientId : "\"" + root.getClientId() + "\"";
        String qReactElementType = Character.isUpperCase(reactElementType.charAt(0)) ? reactElementType : "\"" + reactElementType + "\"";
        String qProps = props == null ? "null" : "\"" + JSONValue.escape(props) + "\"";
        String qInner = innerHtml == null ? "null" : "\"" + JSONValue.escape(innerHtml) + "\"";
        String qExecute = execute == null ? "null" : "\"" + execute + "\"";
        String qRender = render == null ? "null" : "\"" + render + "\"";
        // context.getPartialViewContext().getEvalScripts().add("cadixCreateComp(" + myClientId + "," + parentId + "," + rootClientId + "," + qProps + "," + qReactElementType + "," + qInner + "," + qExecute + "," + qRender + ")");
        context.getResponseWriter().write("cadixCreateComp(" + myClientId + "," + parentId + "," + rootClientId + "," + qProps + "," + qReactElementType + "," + qInner + "," + qExecute + "," + qRender + ")");
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

    //return the first parent which is not a facelets comp (like UIRepeat)
    private static UIComponent getOutputParent(UIComponent p) {

        while ((p = p.getParent()) != null && (p.getFamily().equals("facelets"))) {

        }
        return p;
    }

    private static void cadixActivateCall(UIComponent u, FacesContext context) throws IOException {
        //check if we are a Cadix root

        UIComponent root = getRoot(u);

        //Strange : passThroughAttributes are written to DOM, by the following code
        //But we don't want them for the input element
        //So remove them first, then re-add them after input elem is written
        Map<String, Object> orgAttributes = new HashMap(u.getPassThroughAttributes());
        u.getPassThroughAttributes().clear();
        context.getResponseWriter().startElement("script", u);
        u.getPassThroughAttributes().putAll(orgAttributes);
        String myClientId = "\"" + u.getClientId() + "\"";
        String parentId = "\"" + getOutputParent(u).getClientId() + "\"";
        String rootClientId = root == null ? myClientId : "\"" + root.getClientId() + "\"";
        context.getResponseWriter().write("cadixActivateComp(" + myClientId + "," + parentId + "," + rootClientId + ")");
        context.getResponseWriter().endElement("script");
        //collect  attributes and convert to react props

    }

    //react on an actionevent. Taken from OmniFaces CommandScript
    @Override
    public void decode(FacesContext context, UIComponent component) {
        //Cadix renderer can obly render Cadix comps ...
        var cadixComp = (Cadix) component;

        super.decode(context, component);
        String source = context.getExternalContext().getRequestParameterMap().get("jakarta.faces.source");
        String cadixTag = context.getExternalContext().getRequestParameterMap().get("org.cadix.tag");
        String cadixOutput = context.getExternalContext().getRequestParameterMap().get("org.cadix.output");
        if (component.getClientId(context).equals(source)) {
            CadixEvent event = new CadixEvent(component);
            event.setTag(cadixTag);
            event.setRawOutput(cadixOutput);
            event.setPhaseId(cadixComp.isImmediate() ? PhaseId.APPLY_REQUEST_VALUES : PhaseId.INVOKE_APPLICATION);
            cadixComp.queueEvent(event);
        }

    }

}
