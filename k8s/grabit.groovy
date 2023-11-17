#!/usr/bin/env groovy

/**
 *  The purpose of this scripts is to grab dependencies at build time to avoid downloading every time
 *  the container is started.
 *
 *  When running a Groovy script that uses Grab for dependencies Grape is used as the dependency manager.
 *  Grape is using Ivy for dependency management. Grape uses the ~/.groovy/grapes directory for
 *  downloading libraries.
 */

@Grapes([
        @Grab(group = 'com.sun.mail', module = 'javax.mail', version = '1.6.1'),
        @Grab(group = 'commons-lang', module = 'commons-lang', version = '2.6'),
        @Grab(group = 'ch.qos.logback', module = 'logback-classic', version = '1.2.3'),
        @Grab(group = 'net.logstash.logback', module = 'logstash-logback-encoder', version='6.4'),
        @Grab(group = 'io.kubernetes', module = 'client-java', version = '18.0.0'),
        @GrabConfig(systemClassLoader = true)
])

import io.kubernetes.client.openapi.ApiClient;

println 'Done grabbing'