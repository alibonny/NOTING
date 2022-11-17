# gwt-stockwatch-maven
The GWT stockwatch demo application, maven flavor.

To test it, run:

`mvn -U -e gwt:codeserver -pl *-client -am`

to execute the codeserver (just keep that running),
then you can use

`mvn -U jetty:run -pl *-server -am -Denv=dev`

to run the application in developer mode (the URL is `http://localhost:8080/`). 
