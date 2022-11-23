package com.google.gwt.sample.stockwatcher.server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ListenerImpl implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Context initialized.");
        System.out.println("Loading data from file");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
