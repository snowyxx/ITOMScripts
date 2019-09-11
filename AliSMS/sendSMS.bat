@echo off
cd ..\AliSMS
..\jre\bin\java -cp .;bin;lib\* AliSMS %*
cd ..\bin