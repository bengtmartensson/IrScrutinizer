@ECHO off

REM This simple wrapper basically makes it simpler to invoke ghostscript.
REM It converts pdf or ps to text, that can be imported in IrScrutinizer.

REM Command used to invoke Ghostscript, change if desired/necessary
set GHOSTSCRIPT=C:\Program Files\gs\gs9.52\bin\gswin32c

"%GHOSTSCRIPT%" -dNOPAUSE -dBATCH -q -sOutputFile=- -sDEVICE=txtwrite -- "%1"
