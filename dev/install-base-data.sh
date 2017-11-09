#!/bin/bash

NIORD_HOME=~/.niord-dk
BATCH_SET="niord-dev-basedata"

echo "Copy $BATCH_SET batch set to NIORD_HOME/batch-jobs/batch-sets/"

mkdir -p $NIORD_HOME/batch-jobs/batch-sets/

curl https://github.com/NiordOrg/niord-appsrv/blob/master/02-wildfly/niord-dev-basedata.zip?raw=true \
      -L -o /tmp/niord-dev-basedata.zip
mv /tmp/niord-dev-basedata.zip $NIORD_HOME/batch-jobs/batch-sets/

exit 0
