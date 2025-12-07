@echo off
cd /d "%~dp0"
echo Starting AnimStudio...
mvn compile -q
mvn org.openjfx:javafx-maven-plugin:0.0.8:run

