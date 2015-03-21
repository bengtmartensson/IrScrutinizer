@echo off

REM Note: The preferred way to install and run IrScrutinizer and friends
REM on Windows is to use the setup program. This wrapper is provided just as
REM a convenience for command line friends.

rem The command line name to use to invoke java.exe
rem set JAVA=C:\Program Files\Java\jre1.6.0_07\bin\java
set JAVA=java

rem Where the files are located.
set APPLICATIONHOME=C:\Program Files\IrScrutinizer

rem Uncomment exactly one, depending on your "bitness"
set DLL=Windows-x86
rem set DLL=Windows-amd64

"%JAVA%" -Djava.library.path=%DLL% -jar IrScrutinizer.jar --applicationhome "%APPLICATIONHOME%"
