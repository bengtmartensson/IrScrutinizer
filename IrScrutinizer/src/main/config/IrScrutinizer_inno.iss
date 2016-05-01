#define MyAppName "${project.artifactId}"
#define MyAppVersion "${project.version}"
#define MyAppPublisher "Bengt Martensson"
#define MyAppURL "${project.url}"
#define MyAppExeName "${project.name}.jar"

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
VersionInfoCopyright=Copyright (C) 2014-2016 Bengt Martensson.
DefaultDirName={pf}\{#MyAppName}
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

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked
Name: modifypath; Description: &Add installation directory to path

[Files]
Source: "IrScrutinizer-jar-with-dependencies.jar"; DestName: "IrScrutinizer.jar"; DestDir: "{app}"; Flags: ignoreversion; AfterInstall: CreateWrapper
Source: "..\..\native\Windows-x86\*"; DestDir: "{app}\Windows-x86"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\..\native\Windows-amd64\*"; DestDir: "{app}\Windows-amd64"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "*.ini"; DestDir: "{app}"; Flags: ignoreversion
Source: "irscrutinizer.bat"; DestDir: "{app}"; Flags: ignoreversion
Source: "exportformats.xml"; DestDir: "{app}"; Flags: ignoreversion
Source: "contributed\*"; DestDir: "{app}\contributed"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "generated-documents\IrScrutinizer.html"; DestDir: "{app}\doc"; Flags: isreadme
Source: "doc\*.txt"; DestDir: "{app}\doc"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\..\schemas\*.xsd"; DestDir: "{app}\schemas"
Source: "{#MyAppName}.ico";  DestDir: "{app}"
Source: "..\..\IrpMaster\target\generated-documents\IrpMaster.html"; DestDir: "{app}\doc"

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\{#MyAppName}.ico";
Name: "{group}\HTML-Doc\IrpMaster Documentation"; Filename: "{app}\doc\IrpMaster.html"
Name: "{group}\HTML-Doc\IrScrutinizer Documentation"; Filename: "{app}\doc\IrScrutinizer.html"
Name: "{group}\HTML-Doc\Release Notes"; Filename: "{app}\doc\IrScrutinizer.releasenotes.txt"
Name: "{group}\{cm:ProgramOnTheWeb,{#MyAppName}}"; Filename: "{#MyAppURL}"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{commondesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon; IconFilename: "{app}\{#MyAppName}.ico";

[UninstallDelete]
Type: files; Name: "{app}\irpmaster.bat"

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, "&", "&&")}}"; Parameters: ; Flags: shellexec postinstall skipifsilent

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
   wrapperFilename := ExpandConstant('{app}') + '\IrpMaster.bat';
   SaveStringToFile(wrapperFilename, '@ECHO off' + #13#10, false);
   SaveStringToFile(wrapperFilename, 'set IRSCRUTINIZERHOME=' + ExpandConstant('{app}') + #13#10, true);
   SaveStringToFile(wrapperFilename, 'set JAVA=java' + #13#10, true);
   SaveStringToFile(wrapperFilename, '"%JAVA%" -splash: "-Djava.library.path=%IRSCRUTINIZERHOME%\' + DllLibraryPath() + '" -jar "%IRSCRUTINIZERHOME%\IrScrutinizer.jar" --irpmaster -c "%IRSCRUTINIZERHOME%\IrpProtocols.ini" %*', true);
end;

const
   ModPathName = 'modifypath';
   ModPathType = 'user';

function ModPathDir(): TArrayOfString;
begin
   setArrayLength(Result, 1);
   Result[0] := ExpandConstant('{app}');
 end;

#include "..\..\tools\modpath.iss"
