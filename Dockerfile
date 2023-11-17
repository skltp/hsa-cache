FROM groovy:3.0.13-jdk11

COPY --chown=groovy:groovy k8s/* /home/groovy/
COPY --chown=groovy:groovy target/hsa-cache-*.jar /home/groovy/.groovy/lib/

RUN groovy grabit.groovy
