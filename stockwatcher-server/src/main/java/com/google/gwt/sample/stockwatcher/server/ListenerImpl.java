package com.google.gwt.sample.stockwatcher.server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import com.google.gson.Gson;

public class ListenerImpl implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Context initialized.");
        System.out.println("Loading data from file");
        Gson gson = new Gson();
    	Person p = gson.fromJson("{\"age\": 24, \"name\":\"Mario\"}", Person.class);
    	System.out.println("Data: " + p);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
