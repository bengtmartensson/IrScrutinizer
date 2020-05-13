#!/bin/sh

DIR="$(dirname -- "$(readlink -f -- "${0}")" )"

URL="http://www.harctoolbox.org/downloads/inno_6.0.3.tar.gz"
FILE=${DIR}/../inno.tar.gz

# and get it
echo -n "Downloading ${URL} ..."
wget -O ${FILE} --quiet ${URL}
echo "Done."

cd ${DIR}/..
echo -n "Unpacking..."
tar xf ${FILE}
echo "Done."
rm ${FILE}
