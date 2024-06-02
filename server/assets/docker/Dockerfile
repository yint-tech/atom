FROM openjdk:8-alpine
ENV TZ=Asia/Shanghai
WORKDIR /opt/atom
COPY ./AtomMain.zip /opt/atom/
RUN unzip AtomMain.zip
ENTRYPOINT [ "sh","./bin/Atom.sh" ]
