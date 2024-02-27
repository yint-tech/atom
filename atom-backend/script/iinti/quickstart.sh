#!/usr/bin/env bash
now_dir=`pwd`

if [ ! -d atom_compose ]; then
   mkdir "atom"
else
   rm -rf atom/*
fi

cd atom;

curl -o "atom_compose.zip" "https://oss.iinti.cn/atom/atom-compose.zip"

(unzip --help) </dev/null >/dev/null 2>&1 || {
  echo
  echo "no unzip cmd , please install unzip first: yum install -y unzip"
  exit 4
}


unzip -o atom_compose.zip

cd atom-compose;

docker-compose down;

docker pull registry.cn-beijing.aliyuncs.com/iinti/atom:atom-server-latest;

docker-compose up -d