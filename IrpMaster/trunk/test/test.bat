rem  Please check/adjust the next few lines before using this script!
set DATAFILE=IrpProtocols.ini
set JARFILE=IrpMaster.jar
set LOGFILE=protocols.log
set JAVA=java


set PROG=%JAVA% -jar %JARFILE% -c %DATAFILE% --decodeir --logfile +%LOGFILE%

time /T > %LOGFILE%
%JAVA% -jar %JARFILE% -c %DATAFILE% --decodeir --version >> %LOGFILE%

%PROG% adnotam "*" "*"			
%PROG% aiwa 0,85,170,255 "*" "*"		
%PROG% akai "*" "*"			
%PROG% archer "*"				
%PROG% amino-37 "*" "*"			
%PROG% amino-56 "*" "*"			
%PROG% apple PairID="*" "*"		
%PROG% blaupunkt "*" "*"			
%PROG% bose "*"				
%PROG% canalsat 0,85,127 "*" "*"		
%PROG% denon "*" "*"
%PROG% denon-k 0   0  "*"			
%PROG% denon-k "0,1:15<<1,15" "0,1:15<<1,15" 0:4095#100
%PROG% dgtec "*" "*"			
%PROG% directv "*" "*"			
%PROG% dish_network "*" "*" "*"		
%PROG% dishplayer "*" "*" "*"		
%PROG% emerson "*" "*"			
%PROG% f12 "*" "*" "*"			
%PROG% fujitsu E="*" 0 0 0		
%PROG% fujitsu E=0 0 0 "*"		
%PROG% fujitsu E=0 0 "*" 0		
%PROG% fujitsu E=0 "*" 0 0		
%PROG% fujitsu-56 X="*" E="*" 0 0 0	
%PROG% fujitsu-56 X=0 E=0 0 0 "*"		
%PROG% fujitsu-56 X=0 E=0 0 "*" 0		
%PROG% fujitsu-56 X=0 E=0 "*" 0 0		
%PROG% g.i.4dtv "*" "*"			
%PROG% g.i.cable "*" "*"			
%PROG% gxb "*" "*"				
%PROG% grundig16-30 "*" -1 "*" "*" 		
%PROG% grundig16 "*" -1 "*" "*" 		


%PROG% jvc-48 2:255++16 "*" "*"		

%PROG% jvc-56 X=0 0 0 "*"			
%PROG% jvc-56 X=0 2 0 "*"			
%PROG% jvc-56 X=0 2 "*" 0			
%PROG% jvc-56 X="*" 2 0 0			
%PROG% jvc "*" "*" 			
%PROG% jerrold "*"			
%PROG% kaseikyo-???-??? E=0 M=0 N=0 0 0 "*" 
%PROG% kaseikyo-???-??? E=1 M=0 N=0 0 0 "*" 
%PROG% kaseikyo-???-??? E=2 M=0 N=0 0 0 "*" 
%PROG% kaseikyo-???-??? E=4 M=12 N=34 0 0 "*" 
%PROG% kaseikyo-???-??? E=8 M=56 N=78 0 0 "*" 
%PROG% kaseikyo-???-??? E=15 M=90 N=0 14 85 "*" 
%PROG% kaseikyo-???-??? E=15 M=33 N=99 45 53 "*" 
%PROG% kaseikyo-???-??? E=15 M=0 N=0 3 66 "*" 
%PROG% kaseikyo56-???-??? E=0 X=0 M=0 N=0 0 0 "*" 
%PROG% kaseikyo56-???-??? E=1 X=85 M=0 N=0 0 0 "*" 
%PROG% kaseikyo56-???-??? E=2 X=170 M=0 N=0 0 0 "*" 
%PROG% kaseikyo56-???-??? E=4 X=0 M=12 N=34 0 0 "*" 
%PROG% kaseikyo56-???-??? E=8 X=0 M=56 N=78 0 0 "*" 
%PROG% kaseikyo56-???-??? E=15 X=0 M=90 N=0 14 85 "*" 
%PROG% kaseikyo56-???-??? E=15 X=0 M=33 N=99 45 53 "*" 
%PROG% kaseikyo56-???-??? E=15 X=0 M=0 N=0 3 66 "*" 
%PROG% kathrein "*" "*" 			
%PROG% konka "*" "*"			
%PROG% lumagen "*" "*"			
%PROG% matsui "*" "*"			
%PROG% mce "*" 0,85,170,255 "*" "*"		
%PROG% metz19 "*" "*"			
%PROG% mitsubishi-k 0,85,170,255 0,85,170,255 0 "*" 
%PROG% mitsubishi "*" "*"			
%PROG% nec1 "*" "*"			
%PROG% nec1 "*" 0,85,170,255 "*"		
%PROG% nec2 "*" "*"			
%PROG% nec2 "*" 0,85,170,255 "*"		
%PROG% necx1 0 0 "*"			
%PROG% necx1 0,85,170,255 0,85,170,255 "*" 
%PROG% necx2 0 0 "*"			
%PROG% necx2 0,85,170,255 0,85,170,255 "*" 
%PROG% nokia12 "*" "*"			
%PROG% nokia32 X=0,85,170,255 0,85,170,255 "*" "*"		
%PROG% nokia 0,85,170,255 "*" "*"		
%PROG% nrc17 "*" "*"			
%PROG% ortekMCE "*" "*"			
%PROG% pctv "*" "*"			
%PROG% pacemss "*" -1 "*" "*" 		

%PROG% panasonic2 X=0 0 "*" 0		
%PROG% panasonic2 X=0 0 0 "*"		
%PROG% panasonic2 X="*" 0 0 0		
%PROG% panasonic2 X="*" 0:255++16 0 0	
%PROG% panasonic 0 0 "*"			

%PROG% panasonic 0:255++16 "*" 0		
%PROG% panasonic 0:255++16 0 "*"		
%PROG% panasonic_old "*" "*"	  	
%PROG% pid-0001 "*" 			
%PROG% pid-0003 "*"			
%PROG% pid-0004 "*"			
%PROG% pid-002a "*" "*"			
%PROG% pid-0083 "*"			
%PROG% pioneer "*" "*"			
%PROG% pioneer "*" 0,85,170,255 "*"	
%PROG% proton "*" "*"			
%PROG% rc5 "*" -1 "*" "*"			
%PROG% rc5-7f "*" -1 "*" "*"		
%PROG% rc5-7f-57 "*" -1 "*" "*"		
%PROG% rc5 "*" -1 "*" "*"			

%PROG% rc5x "*" "*" "*" "*"			
%PROG% rc6-6-20 0,85,170,255 "*" "*" "*"	
%PROG% rc6 "*" -1 "*" "*"			
%PROG% "rca(old)" "*" "*"			
%PROG% "rca-38(old)" "*" "*"		
%PROG% rca-38 "*" "*"			
%PROG% rca "*" "*"				
%PROG% replay 0,85,170,255 0,85,170,255 "*" "*"	
%PROG% sampo "*" "*" "*" 			
%PROG% samsung20 "*" "*" "0,1:255<<1,255"	
%PROG% samsung36 E="*" "*" 0 0		
%PROG% samsung36 E=0 "*" "*" 0 		
%PROG% samsung36 E=0 0 "*" "*"		
%PROG% scatl-6 "*" "*"			
%PROG% sharp "*" "*"			
%PROG% sharpdvd "*" "*" "*"			
%PROG% sim2  0:255++2 "*"			
%PROG% somfy  "*" "*"			
%PROG% sony8 "*"				
%PROG% sony12 "*" "*"			
%PROG% sony15 "*" "*"			
%PROG% sony20 "*" "*"			

%PROG% sunfire "*" "*"			
%PROG% tdc-38 "*" "*" "*"			
%PROG% tdc-56 "*" "*" "*"			
%PROG% teac-k "*" 0,85,170,255 "*"		

%PROG% thomson7 "*" -1 "*" "*" 		
%PROG% tivo U="*" "*"			
%PROG% velodyne "*"  0,85,170,255 "*"	
%PROG% velodyne "*" "*" 0,85,170,255	
%PROG% velleman "*" -1 "*" "*"		
%PROG% viewstar "*" 			
%PROG% x10.n N="*" "*"			
%PROG% x10  "*"				
%PROG% xmp-2 "*" 0,85,170,255 "*"		
%PROG% zaptor-36 E=0 0,85,170,255 "*" "*"	
%PROG% zaptor-36 E="*" 0 0 "*"		
%PROG% zaptor-36 E=0 "*" "*" 0		
%PROG% zaptor-56 E=0 0,85,170,255 0,85 "*" 
%PROG% zenith "*" "*" "*"			


time /T >> %LOGFILE%
