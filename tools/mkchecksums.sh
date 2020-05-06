#!/bin/sh

ALGORITHMS="md5 sha1 sha512"

for algo in ${ALGORITHMS} ; do
    #test `${algo}sum -v /dev/null` || continue
    echo "Computing $algo checksums..." >&2
    rm -f checksum.${algo}
    # Allow for having non-existing arguments without confusing error messages
    for f in "$@" ; do
        if [ -f $f ] ; then
            ${algo}sum "$f" >> checksum.${algo}
        fi
    done
done
