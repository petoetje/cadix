/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cadix.core;

import java.io.Serializable;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.Date;

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
    
    public String getPlace() {
        return "D E M O";
    }
    
    private String txt = "hello";

    public String getTxt() {
        return txt;
    }

    public void setTxt(String txt) {
        this.txt = txt;
    }
    
    
    
    public void click(ActionEvent ev) {
        CadixEvent cev =(CadixEvent) ev;
        String a2 = (String) cev.getOutput();
        Object a3 = JSONValue.parse(a2);
        String a4 = cev.getTag();
        setName( (new Date()).toString() );
    }
    
    private Integer one=10;

    public Integer getOne() {
        return one;
    }
    
    public void inc(CadixEvent ev) {
        one++;
    }
}
