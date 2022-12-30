/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cadix.core;

import jakarta.faces.application.ResourceDependencies;
import jakarta.faces.application.ResourceDependency;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.context.FacesContext;
import jakarta.el.MethodExpression;
import jakarta.faces.component.ActionSource;
import jakarta.faces.component.ActionSource2;

import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.event.ActionListener;
import jakarta.faces.event.FacesEvent;
import static jakarta.faces.event.PhaseId.APPLY_REQUEST_VALUES;
import static jakarta.faces.event.PhaseId.INVOKE_APPLICATION;


/**
 *
 * @author christo
 */
@ResourceDependencies({
    @ResourceDependency(library = "cadix", name = "cycle.js", target = "head"),
    @ResourceDependency(library = "cadix", name = "cadix.js", target = "head"),
    @ResourceDependency(library = "jakarta.faces", name = "faces.js", target = "head") // Required for faces.ajax.request.
})
@FacesComponent(createTag = true, tagName = "cadix")
public class Cadix extends UIInput implements ActionSource2 {

    public Cadix() {
        super();
        setRendererType("CadixHTML");//see CadixHtmlRenderer.java
        
    }

    @Override
    public String getFamily() {
       return "org.cadix";
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
     * {@link jakarta.faces.application.Application}.
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
