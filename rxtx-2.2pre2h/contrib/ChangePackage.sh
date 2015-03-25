#!/bin/sh

# Don't use! :)

#
# Sat, 03 May 2003 16:45:08 Modifications from Jörg Weule <weule@7b5.de>
# Create a ed-cmd for the change of one pattern
#
case $1 in
gnu)   X=g/javax_comm/s+javax_comm+gnu_io+g ;;
javax) X=g/gnu_io/s+gnu_io+javax_comm+g ;;
*) echo;echo;echo From the top rxtx directory run;echo;echo -e \\t./ChangePackage.sh gnu;echo -e \\t\\tor;echo -e \\t./ChangePackage.sh javax;echo;echo; exit 0 ;;
esac

#
# ed will be used to keep the owner and mode of the files unchanged.
# We have run the ed-script for the characters '.' '/' as well.
# "tr _ $D" do the change.
#
find . -type f -a -print |
   grep -v $0 |
   (while read F ; do
     ( echo $X;
       echo $X| tr _ /;
       echo $X| tr _ .;
       echo w;
       echo q
     ) | ed $F 2>&1 >/dev/null
    done )

#
# Now we do little changes at the Makefiles. Hope that all we need.
#
find . -name Makefile\* -a -print |
   (while read F  ; do cat <<EOF | ed $F
g/RXTXcomm.jar gnu/s/RXTXcomm.jar gnu/RXTXcomm.jar javax/g
g/CLASSTOP=gnu/s/gnu/javax/g
g/CLASSTOP = gnu/s/gnu/javax/g
g/gnu\\\\\\\\io/s/gnu\\\\\\\\io/javax\\\\\\\\comm/g
g/gnu\\\\\\\\\\\\io/s/gnu\\\\\\\\\\\\io/javax\\\\\\\\\\\\comm/g
g/mkdir gnu/s/mkdir gnu/makedir javax/g
g/include gnu/s/include gnu/include javax/g
g/gnu include/s/gnu include/javax include/g
w
q
EOF
   done)
