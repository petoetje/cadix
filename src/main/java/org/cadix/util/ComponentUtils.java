/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.cadix.util;

import jakarta.faces.component.UIComponent;
import jakarta.faces.component.visit.VisitContext;
import jakarta.faces.component.visit.VisitResult;
import jakarta.faces.context.FacesContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 *
 * @author christo
 */
public class ComponentUtils {

    public Predicate<UIComponent> classFilter(Class targetClass) {
        return (UIComponent t) -> t.getClass().isAssignableFrom(targetClass);
    }

    public Predicate<UIComponent> hasPropertyFilter(String targetProperty) {
        //unfortunately there is no way to tell "value is null" from "property does not exist"
        return (UIComponent t) -> t.getAttributes().get(targetProperty) != null;
    }

    public Predicate<UIComponent> hasPropertyValueFilter(String targetProperty, Object value) {
        //unfortunately there is no way to tell "value is null" from "property does not exist"
        return (UIComponent t) -> Objects.equals(t.getAttributes().get(targetProperty), value);
    }
    
   // example
    
  // Predicate<UIComponent> combined =  classFilter(UIInput.class).and(hasPropertyFilter("cadixprop"));
    

    public static List<UIComponent> findComponent(Predicate<UIComponent> filter) {
        FacesContext context = FacesContext.getCurrentInstance();
        return findComponent(context.getViewRoot(), filter);
    }

    public static List<UIComponent> findComponent(UIComponent root, Predicate<UIComponent> filter) {

        FacesContext context = FacesContext.getCurrentInstance();
        List<UIComponent> result = new ArrayList<>();
        root.visitTree(VisitContext.createVisitContext(context), (var visitContext, var component) -> {
            if (filter.test(component)) {
                result.add(component);
            }
            return VisitResult.ACCEPT;
        });
        return result;
    }

}
