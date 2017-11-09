#!/bin/bash

pushd `dirname ${BASH_SOURCE}` > /dev/null
export DEV_DIR=`pwd`

rm -rf niord-appsrv

echo "**********************************************"
echo "** Configure wildfly                        **"
echo "**********************************************"

# Build a fully configured Wildfly
git clone https://github.com/NiordOrg/niord-appsrv.git
source niord-appsrv/02-wildfly/wildfly-env.sh
./niord-appsrv/02-wildfly/install-wildfly.sh
rm -rf $WILDFLY_PATH/standalone/configuration/standalone_xml_history

mv $WILDFLY_PATH ../

rm -rf niord-appsrv

popd > /dev/null
exit
