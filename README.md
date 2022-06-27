# hsa-cache

Syftet med HSA-cachen är att kunna automatisera inläsning av HSA-data i VP.
Projektet består av två delar.
1. En javakomponent för att läsa in och validera hsa-filer
2. Ett groovy-skript med konfigurationsfiler för att kunna automatisera uppdatering av hsa-fil i vp.

Dessa finns också färdigpaketerade i form av en jar-fil och en zip-fil [här](http://repo1.maven.org/maven2/se/skltp/hsa-cache/hsa-cache/)

Följande skript kan användas för att köra applikationen:

source [path till environment filen]/environment

[path till groovy] -Dlogback.configurationFile=\[path till logback config]/logback.xml -Dhsa.skript.config=[path till skript config]/application.properties -Dgrape.root=[path till groovy dir]/.groovy/   [path till skriptet]/uppdateraHSACache.groovy

Se [här](https://skl-tp.atlassian.net/wiki/spaces/SKLTP/pages/426410008/SKLTP+-+UppdateraHSACache) för mer information.
