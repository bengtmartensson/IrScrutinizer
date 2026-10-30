#!/bin/sh

if [ "$1" = "--local-user" ] ; then
    LOCAL_USER="--local-user $2"
    shift 2
fi

ALGORITHMS="md5 sha1 sha512"

for algo in ${ALGORITHMS} ; do
    echo "Computing $algo checksums..." >&2
    rm -f checksums.${algo}

    for f in "$@" ; do
        # Allow for having non-existing arguments without confusing error messages
        if [ -f $f ] ; then
            ${algo}sum "$f" >> checksums.${algo}
        fi
    done

    if [ -z "${CI}" ] ; then
        if gpg ${LOCAL_USER} --clearsign -o checksums checksums.${algo} ; then
            mv checksums checksums.${algo}
        fi
    fi
done
