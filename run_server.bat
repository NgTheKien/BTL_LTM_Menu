@echo off
chcp 65001 >nul
java -cp build;lib\mysql-connector-java-5.1.45.jar menu.server.UDPServer
pause
