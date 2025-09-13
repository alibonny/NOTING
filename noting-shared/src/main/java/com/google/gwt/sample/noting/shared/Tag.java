package com.google.gwt.sample.noting.shared;

import java.io.Serializable;

public class Tag implements Serializable {

        private String name;
    
    // Costruttore vuoto necessario per la serializzazione GWT
    public Tag() {}
    
    public Tag(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Tag tag = (Tag) obj;
        return name != null ? name.equals(tag.name) : tag.name == null;
    }
    
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }   
}
