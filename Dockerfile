FROM ubuntu:20.04 as basebuilder
ENV TZ=Europe/Stockholm
ENV DEBUG='True'

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN /bin/bash -c "apt-get update && apt-get install openjdk-11-jdk-headless maven python3-pip nginx -y"

COPY . /app
RUN /bin/bash -c "cd /app && /usr/bin/mvn clean -Dmaven.test.skip=true package && cd .."
