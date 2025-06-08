#!/usr/bin/env bash

cd `dirname $0`
script_dir=`pwd`

# enter workspace
cd ..

# backup config
cp conf/application.properties conf/application.properties.backup

echo "download assets"
curl -o AtomMain.zip https://oss.iinti.cn/katom/AtomMain.zip

echo "unzip archive in katom server ..."

unzip -q -o -d . AtomMain.zip

cp conf/application.properties.backup conf/application.properties

echo "bootstrap app"
sh bin/startup.sh