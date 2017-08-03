FROM frolvlad/alpine-oraclejdk8:slim
VOLUME /tmp
ADD target/controller-0.0.1-SNAPSHOT.jar SamsungSpeakerController.jar
EXPOSE 8888
ENV JAVA_OPTS=""
ENTRYPOINT [ "java -Dfile.encoding=UTF-8 -jar SamsungSpeakerController.jar" ]