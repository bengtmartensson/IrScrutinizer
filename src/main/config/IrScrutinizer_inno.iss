#define MyAppName "${project.artifactId}"
#define MyAppVersion "${project.version}"
#define MyAppPublisher "Bengt Martensson"
#define MyAppURL "${project.url}"
#define MyAppJarName "${project.name}.jar"
#define JVMPath "{app}\jre-x86-windows\bin\javaw"

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
; Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{AC1B3ACE-5FFD-A379-472A-D97CE9ED3DE9}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
VersionInfoCopyright=Copyright (C) 2014-2022 Bengt Martensson.
DefaultDirName={commonpf}\{#MyAppName}
DefaultGroupName={#MyAppName}
AllowNoIcons=yes
LicenseFile=doc\LICENSE.txt
InfoBeforeFile=doc\pre_install.txt
InfoAfterFile=doc\post_install.txt
OutputBaseFilename={#MyAppName}-{#MyAppVersion}
Compression=lzma2/max
SolidCompression=yes
OutputDir=.
ChangesEnvironment=true
PrivilegesRequired=none
SetupIconFile={#MyAppName}.ico
ChangesAssociations="yes"

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Types]
Name: "with_jvm"; Description: "Full installation with Java"
Name: "without_jvm"; Description: "Installation using already present Java"

[Components]
Name: "jvm"; Description: "Java Virtual Machine"; Types: with_jvm;

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"
Name: associateGirr; Description: "Associate *.girr files with the program";
Name: modifypath; Description: &Add installation directory to path

[Files]
Source: "IrScrutinizer-jar-with-dependencies.jar"; DestName: "IrScrutinizer.jar"; DestDir: "{app}"; Flags: ignoreversion; AfterInstall: CreateWrapper
Source: "*.xml"; DestDir: "{app}"; Flags: ignoreversion
Source: "exportformats.d\*"; DestDir: "{app}\exportformats.d"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "contributed\import\*"; DestDir: "{app}\contributed\import"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "generated-documents\IrScrutinizer.html"; DestDir: "{app}\doc"; Flags: isreadme
Source: "doc\*.txt"; DestDir: "{app}\doc"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "doc\*.html"; DestDir: "{app}\doc"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "*.xsd"; DestDir: "{app}\schemas"
Source: "{#MyAppName}.ico";  DestDir: "{app}"
Source: "HexCalculator.ico"; DestDir: "{app}"
Source: "TimeFrequencyCalculator.ico"; DestDir: "{app}"
Source: "AmxBeaconListener.ico"; DestDir: "{app}"
Source: "jre-x86-windows\*"; DestDir: "{app}\jre-x86-windows"; Components: jvm; Flags:  ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppJarName}"; IconFilename: "{app}\{#MyAppName}.ico"; Components: not jvm
Name: "{group}\{#MyAppName}"; Filename: "{#JVMPath}"; IconFilename: "{app}\{#MyAppName}.ico"; Parameters: "-jar ""{app}\{#MyAppJarName}"""; Components: jvm
Name: "{group}\HexCalculator"; Filename: "{#JVMPath}"; IconFilename: "{app}\HexCalculator.ico"; Parameters: "-cp ""{app}\{#MyAppJarName}"" org.harctoolbox.guicomponents.HexCalculator"; Components: jvm
Name: "{group}\TimeFrequencyCalculator"; Filename: "{#JVMPath}"; IconFilename: "{app}\TimeFrequencyCalculator.ico"; Parameters: "-cp ""{app}\{#MyAppJarName}"" org.harctoolbox.guicomponents.TimeFrequencyCalculator"; Components: jvm
Name: "{group}\AmxBeaconListener"; Filename: "{#JVMPath}"; IconFilename: "{app}\AmxBeaconListener.ico"; Parameters: "-cp ""{app}\{#MyAppJarName}"" org.harctoolbox.guicomponents.AmxBeaconListenerPanel"; Components: jvm
Name: "{group}\HTML-Doc\Protocol Documentation"; Filename: "{app}\doc\IrpProtocols.html"
Name: "{group}\HTML-Doc\IrScrutinizer Documentation"; Filename: "{app}\doc\IrScrutinizer.html"
Name: "{group}\HTML-Doc\Release Notes"; Filename: "{app}\doc\IrScrutinizer.releasenotes.txt"
Name: "{group}\{cm:ProgramOnTheWeb,{#MyAppName}}"; Filename: "{#MyAppURL}"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"

Name: "{commondesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppJarName}"; Tasks: desktopicon; IconFilename: "{app}\{#MyAppName}.ico"; Components: not jvm
Name: "{commondesktop}\{#MyAppName}"; Filename: "{#JVMPath}";            Tasks: desktopicon; IconFilename: "{app}\{#MyAppName}.ico"; Parameters: "-jar ""{app}\{#MyAppJarName}"""; Components: jvm

[UninstallDelete]
Type: files; Name: "{app}\IrpTransmogrifier.bat"

[Run]
Filename: "{app}\{#MyAppJarName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, "&", "&&")}}"; Parameters: ; Flags: shellexec postinstall skipifsilent; Components: not jvm
Filename: "{#JVMPath}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, "&", "&&")}}"; Parameters: "-jar ""{app}\{#MyAppJarName}"""; Flags: postinstall skipifsilent; Components: jvm

[Registry]
Root: HKCR; Subkey: ".girr";                       ValueType: string; ValueName: ""; ValueData: "girrfile"; Flags: uninsdeletekey;  Tasks: associateGirr
Root: HKCR; Subkey: "girrfile";                    ValueType: string; ValueName: ""; ValueData: "Girr IR commands"; Flags: uninsdeletekey; Tasks: associateGirr
Root: HKCR; Subkey: "girrfile\DefaultIcon";        ValueType: string; ValueName: ""; ValueData: "{app}\IrScrutinizer.ico";            Tasks: associateGirr
;;; Opens a pesky window :-(, but I do not know of a better solution. For example, writing the absolute pathname of javaw is not acceptable.
Root: HKCR; Subkey: "girrfile\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\irscrutinizer.bat"" ""%1"""; Tasks: associateGirr

[Code]
function DllLibraryPath(): String;
begin
  if IsWin64 then
  begin
    Result := 'Windows-amd64';
  end
  else
  begin
    Result := 'Windows-x86';
  end
end;

[Code]
procedure CreateWrapper;
var
   wrapperFilename: String;
begin
   wrapperFilename := ExpandConstant('{app}') + '\IrpTransmogrifier.bat';
   SaveStringToFile(wrapperFilename, '@ECHO off' + #13#10, false);
   SaveStringToFile(wrapperFilename, 'set IRSCRUTINIZERHOME=' + ExpandConstant('{app}') + #13#10, true);
   if WizardSetupType(False) = 'with_jvm' then
   begin
      SaveStringToFile(wrapperFilename, 'set JAVA=%IRSCRUTINIZERHOME%\jre-x86-windows\bin\java' + #13#10, true);
   end
   else
   begin
      SaveStringToFile(wrapperFilename, 'set JAVA=java' + #13#10, true);
   end;
   SaveStringToFile(wrapperFilename, '"%JAVA%"' + ' -cp "%IRSCRUTINIZERHOME%\IrScrutinizer.jar" org.harctoolbox.irp.IrpTransmogrifier %1 %2 %3 %4 %5 %6 %7 %8 %9', true);

   wrapperFilename := ExpandConstant('{app}') + '\IrScrutinizer.bat';
   SaveStringToFile(wrapperFilename, '@ECHO off' + #13#10, false);
   SaveStringToFile(wrapperFilename, #13#10, true);
   SaveStringToFile(wrapperFileName, 'REM Note: The preferred way to install and run IrScrutinizer and friends' + #13#10, true);
   SaveStringToFile(wrapperFileName, 'REM on Windows is to use the setup program. This wrapper is provided just as' + #13#10, true)
   SaveStringToFile(wrapperFileName, 'REM a convenience for command line friends.' + #13#10, true);
   SaveStringToFile(wrapperFilename, #13#10, true);
   SaveStringToFile(wrapperFilename, 'REM Where the files are located' + #13#10, true);
   SaveStringToFile(wrapperFilename, 'set IRSCRUTINIZERHOME=' + ExpandConstant('{app}') + #13#10, true);
   if WizardSetupType(False) = 'with_jvm' then
   begin
      SaveStringToFile(wrapperFilename, 'set JAVA=%IRSCRUTINIZERHOME%\jre-x86-windows\bin\java' + #13#10, true);
   end
   else
   begin
      SaveStringToFile(wrapperFilename, 'set JAVA=java' + #13#10, true);
   end;
   SaveStringToFile(wrapperFilename, '"%JAVA%"' + ' -jar "%IRSCRUTINIZERHOME%\IrScrutinizer.jar" %1 %2 %3 %4 %5 %6 %7 %8 %9', true);
end;

const
   ModPathName = 'modifypath';
   ModPathType = 'user';

function ModPathDir(): TArrayOfString;
begin
   setArrayLength(Result, 1);
   Result[0] := ExpandConstant('{app}');
 end;

#include "..\tools\modpath.iss"
