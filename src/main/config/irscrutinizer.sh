#!/bin/sh

# This wrapper is used to start IrScrutinizer.

# Intended for Unix-like systems (like Linux and MacOsX).
# May need to be locally adapted.

# When changing this file, or updating the programs, it may be a good idea to
# delete the property file, normally ~/.config/IrScrutinizer/properties.xml.

# Set to the preferred Java VM, with or without directory.
JAVA=${JAVA:-java}

JVM_ARGS=

# Where the programs are installed, adjust if required
export IRSCRUTINIZERHOME="$(dirname -- "$(readlink -f -- "${0}")" )"
FATJAR=${IRSCRUTINIZERHOME}/IrScrutinizer-${project.version}-jar-with-dependencies.jar

checkgroup()
{
    if grep $1 /etc/group > /dev/null ; then
	if ! groups | grep $1 > /dev/null ; then
            MESSAGE=${MESSAGE}$1,
	fi
    fi
}

# Remove what looks like a trailing version
PROGNAME=$(basename "$0" .sh | sed -e 's/-[0-9\\.]*$//' )

# If called using the name irptransmogrifier, invoke that "program".
# Recall: exec does not return.
if [ ${PROGNAME} = "irptransmogrifier" ] ; then
    exec "${JAVA}" ${JVM_ARGS} -classpath "${FATJAR}" org.harctoolbox.irp.IrpTransmogrifier "$@"
fi

# If called using a name of one of the tools, invoke that "program".
if [ ${PROGNAME} != "irscrutinizer" -a ${PROGNAME} != "harchardware" ] ; then
    exec "${JAVA}" ${JVM_ARGS} -classpath "${FATJAR}" org.harctoolbox.guicomponents.${PROGNAME} "$@"
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
    MESSAGETAIL=", so you will probably not have access to some devices.\n"
    MESSAGETAIL+="You probably want to correct this. Otherwise, functionality will be limited.\n\n"
    MESSAGETAIL+="Depending on your operating system, the command for fixing this is typically \"sudo usermod -aG $MESSAGE $USER\",\n"
    MESSAGETAIL+="after which you should logout and login again.\n\n"
    MESSAGETAIL+="Proceed anyhow?"
    if ! "${JAVA}" ${JVM_ARGS} -classpath "${FATJAR}" \
        org.harctoolbox.guicomponents.StandalonePopupAnnoyer "${MESSAGEPRE}${MESSAGE}${MESSAGETAIL}" "$@" ; then
        exit 1
    fi
fi

if [ ${PROGNAME} = "harchardware" ] ; then
    exec "${JAVA}" ${JVM_ARGS} -classpath "${FATJAR}" org.harctoolbox.harchardware.Main --apphome "${IRSCRUTINIZERHOME}" "$@"
fi

exec "${JAVA}" ${JVM_ARGS} -jar "${FATJAR}" --apphome "${IRSCRUTINIZERHOME}" "$@"
