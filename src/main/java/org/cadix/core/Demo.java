/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cadix.core;

import java.io.Serializable;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.json.simple.JSONValue;


@Named
@ViewScoped
public class Demo implements Serializable{
    
    private String name = "straf";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    
    
    
    public void click(ActionEvent ev) {
        CadixEvent cev =(CadixEvent) ev;
        String a2 = (String) cev.getOutput();
        Object a3 = JSONValue.parse(a2);
        String a4 = cev.getTag();
    }
}
