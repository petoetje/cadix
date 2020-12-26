/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cadix.core;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.el.ValueExpression;
import javax.faces.application.ResourceDependencies;
import javax.faces.application.ResourceDependency;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.el.MethodExpression;
import javax.faces.component.ActionSource;
import javax.faces.component.ActionSource2;
import javax.faces.el.MethodBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.faces.event.FacesEvent;
import javax.faces.event.PhaseId;
import static javax.faces.event.PhaseId.APPLY_REQUEST_VALUES;
import static javax.faces.event.PhaseId.INVOKE_APPLICATION;

import org.json.simple.JSONValue;

/**
 *
 * @author christo
 */
@ResourceDependencies({
    @ResourceDependency(library = "cadix", name = "cycle.js", target = "head"),
    @ResourceDependency(library = "cadix", name = "cadix.js", target = "head"),
    @ResourceDependency(library = "javax.faces", name = "jsf.js", target = "head") // Required for jsf.ajax.request.
})
@FacesComponent(createTag = true, tagName = "cadix")
public class Cadix extends UIInput implements ActionSource2 {

    public Cadix() {
        super();
        setRendererType("javax.faces.Button");//taken from Mojarra UICommand
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
        Map<String, Object> attributes = getPassThroughAttributes();

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

        String execute = resolveClientIds(context, getExecute());
        String render = resolveClientIds(context, getRender());
        //A Cadix comp can contain non-Cadix children, but they are not renderered
        //(see encodeChildren)
        //they are lumped together as text
        cadixCreateCall(this, context, props, reactElementType, null, execute, render);
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {

        //createElement if not yet done
        //setProperties otherwise
        //if root -> render if not yet exist
        cadixActivateCall(this, context);
        context.getResponseWriter().endElement("span");
    }

    private static void cadixCreateCall(UIComponent c, FacesContext context, String props, String reactElementType, String innerHtml, String execute, String render) throws IOException {
        //check if we are a Cadix root

        UIComponent root = getRoot(c);

        context.getResponseWriter().startElement("script", c);
        String myClientId = "\"" + c.getClientId() + "\"";
        String parentId = "\"" + getOutputParent(c).getClientId() + "\"";
        String rootClientId = root == null ? myClientId : "\"" + root.getClientId() + "\"";
        String qReactElementType = Character.isUpperCase(reactElementType.charAt(0)) ? reactElementType : "\"" + reactElementType + "\"";
        String qProps = props == null ? "null" : "\"" + JSONValue.escape(props) + "\"";
        String qInner = innerHtml == null ? "null" : "\"" + JSONValue.escape(innerHtml) + "\"";
        String qExecute = execute == null ? "null" : "\"" + execute + "\"";
        String qRender = render == null ? "null" : "\"" + render + "\"";
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

        context.getResponseWriter().startElement("script", u);
        String myClientId = "\"" + u.getClientId() + "\"";
        String parentId = "\"" + getOutputParent(u).getClientId() + "\"";
        String rootClientId = root == null ? myClientId : "\"" + root.getClientId() + "\"";
        context.getResponseWriter().write("cadixActivateComp(" + myClientId + "," + parentId + "," + rootClientId + ")");
        context.getResponseWriter().endElement("script");
    }

    //react on an actionevent. Taken from OmniFaces CommandScript
    @Override
    public void decode(FacesContext context) {
        // super.decode(context);
        String source = context.getExternalContext().getRequestParameterMap().get("javax.faces.source");
        String cadixTag = context.getExternalContext().getRequestParameterMap().get("org.cadix.tag");
        String cadixArgs = context.getExternalContext().getRequestParameterMap().get("org.cadix.args");
        if (getClientId(context).equals(source)) {
            CadixEvent event = new CadixEvent(this);
            event.setTag(cadixTag);
            event.setRawArgs(cadixArgs);
            event.setPhaseId(isImmediate() ? PhaseId.APPLY_REQUEST_VALUES : PhaseId.INVOKE_APPLICATION);
            queueEvent(event);
        }

    }

    /**
     * Properties that are tracked by state saving.
     */
    enum PropertyKeys {
        value, immediate, methodBindingActionListener, actionExpression,
    }

    // ---------------------------------------------------- ActionSource / ActionSource2 Methods
    @Override
    public MethodExpression getActionExpression() {
        return (MethodExpression) getStateHelper().get(PropertyKeys.actionExpression);
    }

    @Override
    public void setActionExpression(MethodExpression actionExpression) {
        getStateHelper().put(PropertyKeys.actionExpression, actionExpression);
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public void addActionListener(ActionListener listener) {
        addFacesListener(listener);
    }

    @Override
    public ActionListener[] getActionListeners() {
        return (ActionListener[]) getFacesListeners(ActionListener.class);
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public void removeActionListener(ActionListener listener) {
        removeFacesListener(listener);
    }

    // ----------------------------------------------------- UIComponent Methods
    /**
     * <p>
     * In addition to to the default {@link UIComponent#broadcast} processing,
     * pass the {@link ActionEvent} being broadcast to the method referenced by
     * <code>actionListener</code> (if any), and to the default
     * {@link ActionListener} registered on the
     * {@link javax.faces.application.Application}.
     * </p>
     *
     * @param event {@link FacesEvent} to be broadcast
     *
     * @throws AbortProcessingException Signal the JavaServer Faces
     * implementation that no further processing on the current event should be
     * performed
     * @throws IllegalArgumentException if the implementation class of this
     * {@link FacesEvent} is not supported by this component
     * @throws NullPointerException if <code>event</code> is <code>null</code>
     */
    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException {

        // Perform standard superclass processing (including calling our
        // ActionListeners)
        super.broadcast(event);

        if (event instanceof ActionEvent) {
            FacesContext context = event.getFacesContext();

            // Notify the specified action listener method (if any)
            notifySpecifiedActionListener(context, event);

            // Invoke the default ActionListener
            ActionListener listener = context.getApplication().getActionListener();
            if (listener != null) {
                listener.processAction((ActionEvent) event);
            }
        }
    }

    /**
     *
     * <p>
     * Intercept <code>queueEvent</code> and take the following action. If the
     * event is an <code>{@link ActionEvent}</code>, obtain the
     * <code>UIComponent</code> instance from the event. If the component is an
     * <code>{@link ActionSource}</code> obtain the value of its "immediate"
     * property. If it is true, mark the phaseId for the event to be
     * <code>PhaseId.APPLY_REQUEST_VALUES</code> otherwise, mark the phaseId to
     * be <code>PhaseId.INVOKE_APPLICATION</code>. The event must be passed on
     * to <code>super.queueEvent()</code> before returning from this method.
     * </p>
     *
     */
    @Override
    public void queueEvent(FacesEvent event) {
        UIComponent component = event.getComponent();

        if (event instanceof ActionEvent && component instanceof ActionSource) {
            if (((ActionSource) component).isImmediate()) {
                event.setPhaseId(APPLY_REQUEST_VALUES);
            } else {
                event.setPhaseId(INVOKE_APPLICATION);
            }
        }

        super.queueEvent(event);
    }

    // ---------------------------------------------------------- Deprecated code
    @Override
    public MethodBinding getAction() {
        return null; // cannot throw, because this will abort 
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated This has been replaced by
     * {@link #setActionExpression(javax.el.MethodExpression)}.
     */
    @Override
    public void setAction(MethodBinding action) {
        //do nothing. cannot throw, because this will abort 
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Use {@link #getActionListeners} instead.
     */
    @Override
    public MethodBinding getActionListener() {
        return (MethodBinding) getStateHelper().get(PropertyKeys.methodBindingActionListener);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated This has been replaced by
     * {@link #addActionListener(javax.faces.event.ActionListener)}.
     */
    @Override
    public void setActionListener(MethodBinding actionListener) {
        getStateHelper().put(PropertyKeys.methodBindingActionListener, actionListener);
    }

    private void notifySpecifiedActionListener(FacesContext context, FacesEvent event) {
        MethodBinding mb = getActionListener();
        if (mb != null) {
            mb.invoke(context, new Object[]{event});
        }
    }

    //taken from omnifaces commandscript
    /**
     * Returns a space separated string of client IDs to process on ajax
     * request.
     *
     * @return A space separated string of client IDs to process on ajax
     * request.
     */
    public String getExecute() {
        return (String) getStateHelper().eval("execute", "@this");
    }

    /**
     * Sets a space separated string of client IDs to process on ajax request.
     *
     * @param execute A space separated string of client IDs to process on ajax
     * request.
     */
    public void setExecute(String execute) {
        getStateHelper().put("execute", execute);
    }

    /**
     * Returns a space separated string of client IDs to update on ajax
     * response.
     *
     * @return A space separated string of client IDs to update on ajax
     * response.
     */
    public String getRender() {
        return (String) getStateHelper().eval("render", "@none");
    }

    /**
     * Sets a space separated string of client IDs to update on ajax response.
     *
     * @param render A space separated string of client IDs to update on ajax
     * response.
     */
    public void setRender(String render) {
        getStateHelper().put("render", render);
    }

    private static final String ERROR_UNKNOWN_CLIENTID
            = "Cadix execute/render client ID '%s' cannot be found relative to parent NamingContainer component"
            + " with client ID '%s'.";

    /**
     * Resolve the given space separated collection of relative client IDs to
     * absolute client IDs.
     *
     * @param context The faces context to work with.
     * @param relativeClientIds The space separated collection of relative
     * client IDs to be resolved.
     * @return A space separated collection of absolute client IDs, or
     * <code>null</code> if the given relative client IDs is empty.
     */
    protected String resolveClientIds(FacesContext context, String relativeClientIds) {
        if (relativeClientIds == null || relativeClientIds.isEmpty()) {
            return null;
        }

        StringBuilder absoluteClientIds = new StringBuilder();

        for (String relativeClientId : relativeClientIds.split("\\s+")) {
            if (absoluteClientIds.length() > 0) {
                absoluteClientIds.append(' ');
            }

            if (relativeClientId.charAt(0) == '@') {
                absoluteClientIds.append(relativeClientId);
            } else {
                UIComponent found = findComponent(relativeClientId);

                if (found == null) {
                    throw new IllegalArgumentException(
                            String.format(ERROR_UNKNOWN_CLIENTID, relativeClientId, getNamingContainer().getClientId(context)));
                }

                absoluteClientIds.append(found.getClientId(context));
            }
        }

        return absoluteClientIds.toString();
    }

}
