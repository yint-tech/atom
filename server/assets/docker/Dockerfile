FROM registry.cn-beijing.aliyuncs.com/iinti/ubuntu-openjdk:17
ENV TZ=Asia/Shanghai
WORKDIR /opt/atom
COPY ./AtomMain.zip /opt/atom/
RUN unzip AtomMain.zip
RUN rm -f AtomMain.zip
EXPOSE 8081
ENTRYPOINT [ "sh","./bin/AtomMain" ]
