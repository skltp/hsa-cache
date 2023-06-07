FROM groovy:3.0.13-jdk8

COPY --chown=groovy:groovy k8s/* /home/groovy/

RUN groovy grabit.groovy

ENTRYPOINT groovy uppdateraHsaCacheK8s.groovy