#!/usr/bin/env bash
now_dir=`pwd`

if [ ! -d katom_compose ]; then
   mkdir "katom"
else
   rm -rf katom/*
fi

cd katom;

curl -o "katom_compose.zip" "https://oss.iinti.cn/katom/katom-compose.zip"

(unzip --help) </dev/null >/dev/null 2>&1 || {
  echo
  echo "no unzip cmd , please install unzip first: yum install -y unzip"
  exit 4
}


unzip -o katom_compose.zip

cd katom-compose;

docker-compose down;

docker pull registry.cn-beijing.aliyuncs.com/iinti/katom:katom-server-latest;

docker-compose up -d