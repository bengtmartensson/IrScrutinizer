Summary: RXTX 
Name: rxtx
Version: 2.1
Release: 7pre17
License: LGPL
Group: Development/Libraries
Source: rxtx-%{PACKAGE_VERSION}-%{PACKAGE_RELEASE}.tar.gz
URL: www.rxtx.org
Buildroot: /var/tmp/rxtx-root

%description
rxtx is an full implementation of java commapi which aims to support RS232
IEEE 1284, RS485, I2C and RawIO.  This is a developers release. 
%prep
%setup -q -n rxtx-%{version}-%{release}

%build
export THREADS_FLAG=native
./autogen.sh
CFLAGS="$RPM_OPT_FLAGS" LDFLAGS=-s ./configure --prefix=/usr
pwd
make

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT$JAVA_HOME/jre/lib/ext $RPM_BUILD_ROOT$JAVA_HOME/jre/lib/i386
make RXTX_PATH=$RPM_BUILD_ROOT$JAVA_HOME/jre/lib/i386/ JHOME=$RPM_BUILD_ROOT$JAVA_HOME/jre/lib/ext install
echo "Driver=gnu.io.RXTXCommDriver" > $RPM_BUILD_ROOT$JAVA_HOME/jre/lib/ext/gnu.io.rxtx.properties

find $RPM_BUILD_ROOT/usr -xtype f -print | \
    sed "s@^$RPM_BUILD_ROOT@@g" > INSTALLED_FILES
 
if [ "$(cat INSTALLED_FILES)X" = "X" ] ; then
    echo "No files!"
    exit -1
fi

%files -f INSTALLED_FILES
%defattr(-,root,root)
%doc AUTHORS ChangeLog README RMISecurityManager.html COPYING INSTALL PORTING TODO

%clean
rm -rf $RPM_BUILD_ROOT

%changelog
* Sun Mar 21 2004  Willem Riede <wrrhdev@riede.org>
- adjust spec file to support rpmbuild by ordinary user in Fedora context.
