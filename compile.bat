@echo off
if not exist build mkdir build
javac -encoding UTF-8 -cp lib\mysql-connector-java-5.1.45.jar -d build src\menu\server\*.java src\menu\client\*.java
