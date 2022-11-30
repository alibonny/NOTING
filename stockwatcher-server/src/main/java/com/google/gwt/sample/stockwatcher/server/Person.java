package com.google.gwt.sample.stockwatcher.server;

public class Person {
	private String name;
	private int age;
	
	public String toString() {
		return String.format("%s has %s", name, age);
	}

}
