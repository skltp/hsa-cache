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
        @Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.5'),
        @Grab(group = 'com.sun.mail', module = 'javax.mail', version = '1.6.1'),
        @Grab(group = 'commons-beanutils', module = 'commons-beanutils', version = '1.9.3'),
        @Grab(group = 'ch.qos.logback', module = 'logback-classic', version = '1.2.3'),
        @Grab(group = 'net.logstash.logback', module = 'logstash-logback-encoder', version='6.4'),
        @Grab(group = 'io.kubernetes', module = 'client-java', version = '18.0.0'),
        @Grab(group = 'se.skltp.hsa-cache', module = 'hsa-cache', version = '1.0.1'),
        @GrabConfig(systemClassLoader = true)
])

import org.apache.http.util.EntityUtils

println 'Done grabbing'