#!/bin/sh
# Please check/adjust the next few lines before using this script!
DATAFILE=data/IrpProtocols.ini
JARFILE=dist/IrpMaster.jar
LOGFILE=protocols.log
JAVA=java


PROG="$JAVA -jar $JARFILE -c $DATAFILE --decodeir --analyze --logfile +$LOGFILE"

date > $LOGFILE
$JAVA -jar $JARFILE -c $DATAFILE --decodeir --version >> $LOGFILE

#$PROG airasync B=0			# ?
#$PROG airasync B=85			# ?
#$PROG airasync B=170			# ?
#$PROG airasync B=255			# ?
$PROG adnotam \* \*			# new, all wrong
$PROG aiwa 0,85,170,255 \* \*		# ok
exit
$PROG akai \* \*			# 57 fails
$PROG archer \*				# 2 fails
$PROG amino-37 \* \*			# ok
$PROG amino-56 \* \*			# ok
$PROG apple PairID=\* \*		# 130 fails
$PROG blaupunkt \* \*			# 1 fail: 3 127
$PROG bose \*				# ok
$PROG canalsat 0,85,127 \* \*		# ok 
$PROG denon \* \*			# ok
$PROG denon-k 0   0  \*			# 36 fails
$PROG denon-k '0,1:15<<1,15' '0,1:15<<1,15' 0:4095\#100 #
$PROG dgtec \* \*			# 4096 fails (out of 65536) for D=15, 30, 45, 60, 75, 90, 105, 120,...
$PROG directv \* \*			# ok
$PROG dish_network \* \* \*		# 2 fails 
$PROG dishplayer \* \* \*		# 2 fails
$PROG emerson \* \*			# ok, 64 fails, for D=63 (DecodeIR.cpp line 2096 return after recognizing "Sampo" decode
$PROG f12 \* \* \*			# ok
$PROG fujitsu E=\* 0 0 0		# ok
$PROG fujitsu E=0 0 0 \*		# 
$PROG fujitsu E=0 0 \* 0		# 
$PROG fujitsu E=0 \* 0 0		# 
$PROG fujitsu-56 X=\* E=\* 0 0 0	# ok
$PROG fujitsu-56 X=0 E=0 0 0 \*		# 
$PROG fujitsu-56 X=0 E=0 0 \* 0		# 
$PROG fujitsu-56 X=0 E=0 \* 0 0		# 
$PROG g.i.4dtv \* \*			# 1 fail for D=F=0n
$PROG g.i.cable \* \*			# ok
$PROG gxb \* \*				# ok
$PROG grundig16-30 \* -1 \* \* 		# ok
$PROG grundig16 \* -1 \* \* 		# ok
# $PROG iodatan \* \* \*                # Works, but has so weir parametrization, so that I do not care testing
#$PROG jvc-48 \* 0 0 			# fail whenever D % 16 != 2
$PROG jvc-48 2:255++16 \* \*		# 4096 fails
#$PROG jvc-56 X=0 \* 0 0		# fail whenever D % 16 != 2
$PROG jvc-56 X=0 0 0 \*			# all fail fail whenever D % 16 != 2
$PROG jvc-56 X=0 2 0 \*			# ok
$PROG jvc-56 X=0 2 \* 0			# 256 fails
$PROG jvc-56 X=\* 2 0 0			# ok
$PROG jvc \* \* 			# 2 Fail for D=F=0 and D=F=255.
$PROG jerrold \*			# ok 
$PROG kaseikyo-???-??? E=0 M=0 N=0 0 0 \* # one fail
$PROG kaseikyo-???-??? E=1 M=0 N=0 0 0 \* #
$PROG kaseikyo-???-??? E=2 M=0 N=0 0 0 \* #
$PROG kaseikyo-???-??? E=4 M=12 N=34 0 0 \* #
$PROG kaseikyo-???-??? E=8 M=56 N=78 0 0 \* #
$PROG kaseikyo-???-??? E=15 M=90 N=0 14 85 \* #
$PROG kaseikyo-???-??? E=15 M=33 N=99 45 53 \* #
$PROG kaseikyo-???-??? E=15 M=0 N=0 3 66 \* #
$PROG kaseikyo56-???-??? E=0 X=0 M=0 N=0 0 0 \* # one fail
$PROG kaseikyo56-???-??? E=1 X=85 M=0 N=0 0 0 \* #
$PROG kaseikyo56-???-??? E=2 X=170 M=0 N=0 0 0 \* #
$PROG kaseikyo56-???-??? E=4 X=0 M=12 N=34 0 0 \* #
$PROG kaseikyo56-???-??? E=8 X=0 M=56 N=78 0 0 \* #
$PROG kaseikyo56-???-??? E=15 X=0 M=90 N=0 14 85 \* #
$PROG kaseikyo56-???-??? E=15 X=0 M=33 N=99 45 53 \* #
$PROG kaseikyo56-???-??? E=15 X=0 M=0 N=0 3 66 \* #
$PROG kathrein \* \* 			# ok
$PROG konka \* \*			# ok
$PROG lumagen \* \*			# ok
$PROG matsui \* \*			# 12 fails
$PROG mce \* 0,85,170,255 \* \*		# ok
$PROG metz19 \* \*			# ok
$PROG mitsubishi-k 0,85,170,255 0,85,170,255 0 \* # ok
$PROG mitsubishi \* \*			# 1 fail: 0 0 failed
$PROG nec1 \* \*			# ok
$PROG nec1 \* 0,85,170,255 \*		# ok
$PROG nec2 \* \*			# ok
$PROG nec2 \* 0,85,170,255 \*		# ok
$PROG necx1 0 0 \*			# 4352 fails
$PROG necx1 0,85,170,255 0,85,170,255 \* # 
$PROG necx2 0 0 \*			# 1280 fails
$PROG necx2 0,85,170,255 0,85,170,255 \* #
$PROG nokia12 \* \*			# OK
$PROG nokia32 X=0,85,170,255 0,85,170,255 \* \*		# OK
$PROG nokia 0,85,170,255 \* \*		# OK
$PROG nrc17 \* \*			# 1 fail
$PROG ortekMCE \* \*			# ok
$PROG pctv \* \*			# ok
$PROG pacemss \* -1 \* \* 		# ok
#$PROG panasonic2 X=0 \* 0 0		# fails whenever D % 16 != 0
$PROG panasonic2 X=0 0 \* 0		# ok 
$PROG panasonic2 X=0 0 0 \*		# ok 
$PROG panasonic2 X=\* 0 0 0		# ok 
$PROG panasonic2 X=\* 0:255++16 0 0	# ok 
$PROG panasonic 0 0 \*			# ok
#$PROG panasonic \* 0 0			# fails whenever D % 16 != 0
$PROG panasonic 0:255++16 \* 0		# 16 fails
$PROG panasonic 0:255++16 0 \*		# ok
$PROG panasonic_old \* \*	  	# ok
$PROG pid-0001 \* 			# OK
$PROG pid-0003 \*			# OK
$PROG pid-0004 \*			# OK
$PROG pid-002a \* \*			# OK, after making last duration a gap instead
$PROG pid-0083 \*			# FAIL
$PROG pioneer \* \*			# ok
$PROG pioneer \* 0,85,170,255 \*	# ok
$PROG proton \* \*			# 137 fails when D=63,95,111,119,...  F=255
$PROG rc5 \* -1 \* \*			# ok
$PROG rc5-7f \* -1 \* \*		# 8192
$PROG rc5-7f-57 \* -1 \* \*		# 8192
$PROG rc5 \* -1 \* \*			# ok
#$PROG rc5x \* 0,'1:127<<1,127' \* \*	# ?? 9216 (was 4096 fails)
$PROG rc5x \* \* \* \*			# 131072, incl seg fault (31 * * 1)
$PROG rc6-6-20 0,85,170,255 \* \* \*	# ok
$PROG rc6 \* -1 \* \*			# ok
$PROG "rca(old)" \* \*			# decoded as rca, ok, was ok now error
$PROG "rca-38(old)" \* \*		# decoded as rca, ok, was ok now error
$PROG rca-38 \* \*			# ok 
$PROG rca \* \*				# ok
$PROG replay 0,85,170,255 0,85,170,255 \* \*	#
$PROG sampo \* \* \* 			# 4032 fails, was 1 fail, was 4032 fails, whenever S==63-D (decoded as Emerson, only) , except D=63,S=0
$PROG samsung20 \* \* '0,1:255<<1,255'	# 2 fails
$PROG samsung36 E=\* \* 0 0		# 
$PROG samsung36 E=0 \* \* 0 		# 
$PROG samsung36 E=0 0 \* \*		#
$PROG scatl-6 \* \*			# 64 fails: D=63 failed (decoded as Sampo, only)
$PROG sharp \* \*			# ok
$PROG sharpdvd \* \* \*			# ok
$PROG sim2  0:255++2 \*			# 129 fails
$PROG somfy  \* \*			# ok
$PROG sony8 \*				# ok
$PROG sony12 \* \*			# ok
$PROG sony15 \* \*			# ok
$PROG sony20 \* \*			# ok
# $PROG streamzap \* -1 \* \*		# decodes as rc5-7f by decodeir v2.43
$PROG sunfire \* \*			# ok
$PROG tdc-38 \* \* \*			# ok
$PROG tdc-56 \* \* \*			# ok
$PROG teac-k \* 0,85,170,255 \*		# ok
#$PROG thomson \* -1 \* \* 		# 40, was 4099 (was 2049) fails, Bit 5 in D always 00, also 0,0,0
$PROG thomson7 \* -1 \* \* 		# 3 fails
$PROG tivo U=\* \*			# ok
$PROG velodyne \*  0,85,170,255 \*	# new, all wrong
$PROG velodyne \* \* 0,85,170,255	# new, all wrong
$PROG velleman \* -1 \* \*		# ok
$PROG viewstar \* 			# ok
$PROG x10.n N=\* \*			# ok
$PROG x10  \*				# ok
$PROG xmp-2 \* 0,85,170,255 \*		# 1024 fails
$PROG zaptor-36 E=0 0,85,170,255 \* \*	# ok
$PROG zaptor-36 E=\* 0 0 \*		#
$PROG zaptor-36 E=0 \* \* 0		# 
$PROG zaptor-56 E=0 0,85,170,255 0,85 \* # ok
$PROG zenith \* \* \*			# ok but seg violation (6 1 0)


date >> $LOGFILE
