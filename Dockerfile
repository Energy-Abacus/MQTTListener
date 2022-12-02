FROM maven
RUN mkdir /app
WORKDIR /app
COPY . /app
EXPOSE 1883
RUN mvn clean install
CMD "mvn" "exec:java"