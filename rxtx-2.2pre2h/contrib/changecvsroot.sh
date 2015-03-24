#!/bin/sh
#find . -name Root -exec sed s/$1/$2/ {} >tmp ; mv tmp {} \;
for i in `find . -name Root`; do cat $i | sed s/$1/$2/ > tmp; mv tmp $i;  done

