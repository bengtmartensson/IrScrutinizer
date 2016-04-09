#!/bin/sh

# The name of this file is misleading ;-).

OUTPUTDIR=appimagekit

mkdir ${OUTPUTDIR}
wget -O ${OUTPUTDIR}/AppRun "https://github.com/probonopd/AppImageKit/releases/download/5/AppRun" # (64-bit)
chmod +x ${OUTPUTDIR}/AppRun

wget -O ${OUTPUTDIR}/AppImageAssistant "https://github.com/probonopd/AppImageKit/releases/download/5/AppImageAssistant" # (64-bit)
chmod +x ${OUTPUTDIR}/AppImageAssistant

wget https://github.com/probonopd/AppImageKit/archive/5.tar.gz
tar -x -f 5.tar.gz --strip-components=1 -C ${OUTPUTDIR} AppImageKit-5/desktopintegration
rm 5.tar.gz
