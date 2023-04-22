# gwt-stockwatch-maven
[![Java CI with Maven](https://github.com/jjocram/gwt-stockwatch-maven/actions/workflows/maven.yml/badge.svg)](https://github.com/jjocram/gwt-stockwatch-maven/actions/workflows/maven.yml)

The GWT stockwatch demo application, maven flavor and JUnit testing.

To test it, run:

`mvn -U -e gwt:codeserver -pl *-client -am`

to execute the codeserver (just keep that running),
then you can use

`mvn -U jetty:run-forked -pl *-server -am -Denv=dev`

to run the application in developer mode (the URL is `http://localhost:8080/`). 
