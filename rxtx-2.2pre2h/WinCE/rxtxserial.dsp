# Microsoft Developer Studio Project File - Name="rxtxSerial" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 6.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Dynamic-Link Library" 0x0102

CFG=rxtxSerial - Win32 Debug
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "rxtxserial.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "rxtxserial.mak" CFG="rxtxSerial - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "rxtxSerial - Win32 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "rxtxSerial - Win32 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE 

# Begin Project
# PROP AllowPerConfigDependencies 0
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
MTL=midl.exe
RSC=rc.exe

!IF  "$(CFG)" == "rxtxSerial - Win32 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "W32Rel"
# PROP BASE Intermediate_Dir "W32Rel"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "W32Rel"
# PROP Intermediate_Dir "W32Rel"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "RXTXSERIAL_EXPORTS" /YX /FD /c
# ADD CPP /nologo /MT /W3 /GX /O2 /I "C:\JBuilder6\jdk1.3.1\include" /I "C:\JBuilder6\jdk1.3.1\include\win32" /D "NDEBUG" /D "_MBCS" /D "WIN32" /D "_WINDOWS" /D "_USRDLL" /D "RXTXSERIAL_EXPORTS" /YX"StdAfx.h" /FD /c
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x415 /d "NDEBUG"
# ADD RSC /l 0x415 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /machine:I386
# ADD LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /machine:I386

!ELSEIF  "$(CFG)" == "rxtxSerial - Win32 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "W32Dbg"
# PROP BASE Intermediate_Dir "W32Dbg"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "W32Dbg"
# PROP Intermediate_Dir "W32Dbg"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "RXTXSERIAL_EXPORTS" /YX /FD /GZ /c
# ADD CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /I "C:\JBuilder6\jdk1.3.1\include" /I "C:\JBuilder6\jdk1.3.1\include\win32" /D "DEBUG" /D "_DEBUG" /D "W32Dbg" /D "WIN32" /D "_WINDOWS" /D "_USRDLL" /D "RXTXSERIAL_EXPORTS" /FR /YX"StdAfx.h" /FD /GZ /c
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x415 /d "_DEBUG"
# ADD RSC /l 0x415 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /debug /machine:I386 /pdbtype:sept
# ADD LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /debug /machine:I386 /pdbtype:sept

!ENDIF 

# Begin Target

# Name "rxtxSerial - Win32 Release"
# Name "rxtxSerial - Win32 Debug"
# Begin Group "Source Files"

# PROP Default_Filter "cpp;c;cxx;rc;def;r;odl;idl;hpj;bat"
# Begin Source File

SOURCE=.\gnu_io_RXTXCommDriver.cpp

!IF  "$(CFG)" == "rxtxSerial - Win32 Release"

# ADD CPP /YX

!ELSEIF  "$(CFG)" == "rxtxSerial - Win32 Debug"

# ADD CPP /YX"StdAfx.h"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\gnu_io_RXTXPort.cpp

!IF  "$(CFG)" == "rxtxSerial - Win32 Release"

# ADD CPP /YX

!ELSEIF  "$(CFG)" == "rxtxSerial - Win32 Debug"

# ADD CPP /YX"StdAfx.h"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\rxtxHelpers.cpp

!IF  "$(CFG)" == "rxtxSerial - Win32 Release"

# ADD CPP /YX

!ELSEIF  "$(CFG)" == "rxtxSerial - Win32 Debug"

# ADD CPP /YX"StdAfx.h"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\rxtxSerial.cpp

!IF  "$(CFG)" == "rxtxSerial - Win32 Release"

# ADD CPP /YX

!ELSEIF  "$(CFG)" == "rxtxSerial - Win32 Debug"

# ADD CPP /YX"StdAfx.h"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\StdAfx.cpp

!IF  "$(CFG)" == "rxtxSerial - Win32 Release"

# ADD CPP /YX

!ELSEIF  "$(CFG)" == "rxtxSerial - Win32 Debug"

# ADD CPP /YX"StdAfx.h"

!ENDIF 

# End Source File
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h;hpp;hxx;hm;inl"
# Begin Source File

SOURCE=.\gnu_io_RXTXCommDriver.h
# End Source File
# Begin Source File

SOURCE=.\gnu_io_RXTXPort.h
# End Source File
# Begin Source File

SOURCE=.\rxtxHelpers.h
# End Source File
# Begin Source File

SOURCE=.\StdAfx.h
# End Source File
# End Group
# Begin Group "Resource Files"

# PROP Default_Filter "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe"
# End Group
# End Target
# End Project
