#!/bin/sh

# This wrapper is used to start IrScrutinizer.

# Intended for Unix-like systems (like Linux and MacOsX).
# May need to be locally adapted.

# When changing this file, or updating the programs, it may be a good idea to
# delete the property file, normally ~/.config/IrScrutinizer/properties.xml.

if [ "$1" = "-s" -o "$1" = "--scale" ] ; then
    SCALE_FACTOR=$2
    shift 2
fi

# Set to the preferred Java VM, with or without directory.
JAVA=${JAVA:-java}

# Modify for Hight DPI usage.
# Not all JVMs recognize this; some only accept integer arguments.
JVM_ARGS=-Dsun.java2d.uiScale=${SCALE_FACTOR:-1}

# Where the programs are installed, adjust if required
export IRSCRUTINIZERHOME="$(dirname -- "$(readlink -f -- "${0}")" )"
FATJAT=${IRSCRUTINIZERHOME}/IrScrutinizer-jar-with-dependencies.jar

checkgroup()
{
    if grep $1 /etc/group > /dev/null ; then
	if ! groups | grep $1 > /dev/null ; then
            MESSAGE=${MESSAGE}$1,
	fi
    fi
}

# If called using the name irptransmogrifier, invoke that "program".
# Recall: exec does not return.
if [ $(basename "$0" ) = "irptransmogrifier" ] ; then
    exec "${JAVA}" ${JVM_ARGS} -classpath "${FATJAT}" org.harctoolbox.irp.IrpTransmogrifier "$@"
fi

# If called using a name of one of the tools, invoke that "program".
if [ $(basename "$0" ) != "irscrutinizer" -a $(basename "$0" ) != "harchardware" ] ; then
    exec "${JAVA}" ${JVM_ARGS} -classpath "${FATJAT}" org.harctoolbox.guicomponents.$(basename "$0") "$@"
fi

# Check that the use is a member of some groups ...
checkgroup dialout
checkgroup lock
checkgroup lirc

# ... if not, barf, and potentially bail out.
if [ "x${MESSAGE}" != "x" ] ; then
     # Remove last , of $MESSAGE
    MESSAGE=$(echo ${MESSAGE} | sed -e "s/,$//")
    MESSAGEPRE="You are not a member of the group(s) "
    MESSAGETAIL=", so you will probably not have access to some devices.\nYou probably want to correct this. Otherwise, functionality will be limited.\n\nDepending on your operating system, the command for fixing this may be \"sudo usermod -aG $MESSAGE $USER\".\n\nProceed anyhow?"
    if ! "${JAVA}" ${JVM_ARGS} -classpath "${FATJAT}" \
        org.harctoolbox.guicomponents.StandalonePopupAnnoyer "${MESSAGEPRE}${MESSAGE}${MESSAGETAIL}" "$@" ; then
        exit 1
    fi
fi

if [ $(basename "$0" ) = "harchardware" ] ; then
    exec "${JAVA}" ${JVM_ARGS} -classpath "${FATJAT}" org.harctoolbox.harchardware.Main --apphome "${IRSCRUTINIZERHOME}" "$@"
fi

exec "${JAVA}" ${JVM_ARGS} -jar "${FATJAT}" --apphome "${IRSCRUTINIZERHOME}" "$@"
