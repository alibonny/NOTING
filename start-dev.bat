@echo off
ECHO.
ECHO =======================================
ECHO   Avvio dell'ambiente di sviluppo NOTING
ECHO =======================================
ECHO.

ECHO [1/2] Avvio del GWT CodeServer in una nuova finestra...
START "GWT CodeServer" mvn gwt:codeserver -pl noting-client -am

ECHO.
ECHO [2/2] Avvio del Server Jetty in una nuova finestra...
START "Jetty Server" mvn jetty:run-forked -pl noting-server -am -Denv=dev

ECHO.
ECHO Fatto! Controlla le due nuove finestre del terminale.
pause