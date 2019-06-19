@echo off

REM Note: The preferred way to install and run IrScrutinizer and friends
REM on Windows is to use the setup program. This wrapper is provided just as
REM a convenience for command line friends.

rem The command line name to use to invoke java.exe
rem set JAVA=C:\Program Files\Java\jre1.6.0_07\bin\java
set JAVA=java

rem Where the files are located.
set APPLICATIONHOME=C:\Program Files\IrScrutinizer

"%JAVA%" -jar "%APPLICATIONHOME%"\IrScrutinizer.jar --applicationhome "%APPLICATIONHOME%" %1 %2 %3 %4 %5 %6 %7 %8 %9
