#!/usr/bin/env bash

## 此脚本仅因体产品使用，用于实现因体代码加密保护，授权agent注入，产品发布、docker发布
## 以及可能注入的部分针对于和网站联动的脚本等
(IntTool.sh --help) </dev/null >/dev/null 2>&1 || {
  echo
  echo "**Warning**: You need Install IntTool from :https://github.com/yint-tech/inttool."
  exit 1
}

now_dir=`pwd`
cd `dirname $0`
shell_dir=`pwd`

cd ${now_dir}

echo "call iinti proguard toolkit"
IntTool.sh Protection --input-dist $1  \
 --proguard-config ${shell_dir}/iinti/proguard.pro \
 --main-jar atom-server \
 --slave-jar mybatis-plus-extension \
 --insert-bin "${shell_dir}"/iinti/upgrade.sh.sh \
 --inject-rule "${shell_dir}"/iinti/inject_rule.txt \
 --output-dist ${shell_dir}/docker/AtomMain.zip

image_version=`date +%Y%m%d%H%M`;
echo $image_version;

cd ${shell_dir}/docker
echo "build compose img"
# compose 模式
docker build -t registry.cn-beijing.aliyuncs.com/iinti/common:atom-server-$image_version .
if [ $? != 0 ] ; then
    echo "docker build failed"
    exit 10
fi
docker tag registry.cn-beijing.aliyuncs.com/iinti/common:atom-server-$image_version registry.cn-beijing.aliyuncs.com/iinti/common:atom-server-latest;
docker push registry.cn-beijing.aliyuncs.com/iinti/common:atom-server-latest;
docker push registry.cn-beijing.aliyuncs.com/iinti/common:atom-server-$image_version;

echo "build allInOne img"
# all in one 模式
docker build -f Dockerfile.AllInOne -t registry.cn-beijing.aliyuncs.com/iinti/common:atom-allInOne-$image_version .
if [ $? != 0 ] ; then
    echo "docker build failed"
    exit 11
fi
docker tag registry.cn-beijing.aliyuncs.com/iinti/common:atom-allInOne-$image_version registry.cn-beijing.aliyuncs.com/iinti/common:atom-allInOne-latest;
docker push registry.cn-beijing.aliyuncs.com/iinti/common:atom-allInOne-latest;
docker push registry.cn-beijing.aliyuncs.com/iinti/common:atom-allInOne-$image_version;

echo "upload to oss"
scp ${shell_dir}/docker/AtomMain.zip  root@oss.iinti.cn:/root/local-deplpy/gohttpserver/data/atom/
scp ${shell_dir}/iinti/quickstart.sh  root@oss.iinti.cn:/root/local-deplpy/gohttpserver/data/atom/

echo "clean file"
rm -f ${shell_dir}/docker/AtomMain.zip

echo "all done"