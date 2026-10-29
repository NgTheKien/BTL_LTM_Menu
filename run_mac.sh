#!/bin/sh
# Dung tren macOS/Linux: sh run_mac.sh compile | server | client
cd "$(dirname "$0")"
JAR=lib/mysql-connector-java-5.1.45.jar
case "$1" in
  compile) mkdir -p build && javac -encoding UTF-8 -cp "$JAR" -d build src/menu/server/*.java src/menu/client/*.java ;;
  server)  java -cp "build:$JAR" menu.server.UDPServer ;;
  client)  java -cp build menu.client.UDPClient ;;
  *)       echo "Cach dung: sh run_mac.sh compile | server | client" ;;
esac
