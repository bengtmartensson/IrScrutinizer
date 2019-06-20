#!/bin/sh

# The name of this file is misleading ;-).

OUTPUTDIR=appimagekit

mkdir ${OUTPUTDIR}
wget -O ${OUTPUTDIR}/AppRun "https://github.com/AppImage/AppImageKit/releases/download/12/AppRun-x86_64" # (64-bit)
chmod +x ${OUTPUTDIR}/AppRun
