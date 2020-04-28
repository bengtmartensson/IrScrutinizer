#!/bin/sh

# This simple wrapper basically makes it simpler to invoke ghostscript.
# It converts pdf or ps to text, that can be imported in IrScrutinizer.

# Command used to invoke Ghostscript, change if desired/necessary
GHOSTSCRIPT=gs

"${GHOSTSCRIPT}" -dNOPAUSE -dBATCH -q -sOutputFile=- -sDEVICE=txtwrite -- "$@"
