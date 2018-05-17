import groovy.transform.Field
import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.skl.tp.hsa.cache.HsaFileVerifierImpl

import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.net.ssl.*
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.zip.ZipFile

@Grapes([
        @Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.5'),
        @Grab(group = 'com.sun.mail', module = 'javax.mail', version = '1.6.1'),
        @Grab(group = 'commons-beanutils', module = 'commons-beanutils', version = '1.9.3'),
        @Grab(group = 'ch.qos.logback', module = 'logback-classic', version = '1.2.3'),
        @Grab(group = 'se.skltp.hsa-cache', module = 'hsa-cache', version = '1.0.1'),
        @GrabConfig(systemClassLoader = true)
])



@Field
static Logger logger = LoggerFactory.getLogger("scriptLogger")
@Field
static Properties appProperties = downloadProperties()

logger.info("Börjar updatera hsa cache.")

if (this.args.size() > 0) {
    logger.info("Scriptet kördes med argument: ")
    args.each { logger.info(it) }

    if (this.args.size() == 1) {
        File hsaFile = new File(this.args[0].toString())
        if (!hsaFile.exists()) {
            logger.error("Hsa filen {} finns inte.", hsaFile.absolutePath)
            return
        }

        changeSymlinksToHSAFiles(hsaFile)
        resetHSACache()
        return
    } else {
        logger.error("För att köra skriptet får du ställa in: \b" +
                "- ett argument med sökväg till hsa-filen som du vill uppdatera cachen med.\b" +
                "- inga argument om du vill ladda ner ny hsa-zip-fil, verifera och uppdatera cachen med den.")
        return
    }
}

try {
    downloadHSAFilesFromServer()
    File hsaFile = unzipFilesToDir()[0]
    validateHSAFileAndChangeSymlink(hsaFile)
    resetHSACache()
} catch (Exception e) {
    logger.error("", e)
    sendProblemMail(e)
}



private static void changeSymlinksToHSAFiles(File hsaFile) {
    def symlink = appProperties.getProperty("hsa.symlink.file")
    def symlinkFile = new File(symlink)

    String tmpFilePath = symlink + "_tmp"
    File tmpFile = new File(tmpFilePath)

    if (symlinkFile.exists()) {
        Files.move(symlinkFile.toPath(), tmpFile.toPath()) //flyttar symlink-filen till tmp
    }

    try {
        Files.createSymbolicLink(symlinkFile.toPath(), hsaFile.toPath());
    } catch (Exception e) {
        Files.move(tmpFile.toPath(), symlinkFile.toPath()) //flyttar symlink-filen tillbaka
        throw e;
    }
    Files.deleteIfExists(tmpFile.toPath())  //tar bort tmp
    logger.info("Symlink för hsa-filen uppdaterad till " + hsaFile.absolutePath)
}

private static String getCurrentHSAFile() {
    def symlinkFileName = appProperties.getProperty("hsa.symlink.file")
    File symlinkFile = new File(symlinkFileName);
    return Files.readSymbolicLink(symlinkFile.toPath())
}

private static File[] unzipFilesToDir() {
    def destDirName = appProperties.getProperty("hsa.files.dir")
    def zipFileName = appProperties.getProperty("hsa.file.name")

    def zip = new ZipFile(new File(zipFileName))

    def files = []
    zip.entries().each {
        File currentFile = createUniqueHsaFile(it.name, destDirName)
        def file = currentFile << zip.getInputStream(it).text
        files << file
    }
    logger.debug("Filen {} uppackad till {} ", zipFileName, destDirName)
    return files
}

private static File createUniqueHsaFile(String fileName, String destDir) {
    String fileNameWithDate = addCurrentDateToFileName(fileName)
    return addIndexIfFileExist(fileNameWithDate, destDir)
}

private static String addCurrentDateToFileName(String fileName) {
    int dotIndex = fileName.lastIndexOf(".")
    String date = new SimpleDateFormat("_yyyyMMdd").format(new Date())
    return fileName.substring(0, dotIndex) + date + fileName.substring(dotIndex, fileName.size())
}

private static File addIndexIfFileExist(String fileName, String destDir) {
    int dotIndex = fileName.lastIndexOf(".")
    def currentFile = new File(destDir, fileName)
    int i = 0
    while (currentFile.exists()) {
        def name = fileName.substring(0, dotIndex) + "(" + i++ + ")" + fileName.substring(dotIndex, fileName.size())
        currentFile = new File(destDir, name)
    }
    return currentFile;
}

private static validateHSAFileAndChangeSymlink(File hsaFile) {
    logger.info("Validering av hsa filen {}", hsaFile.getAbsolutePath())
    HsaFileVerifierImpl verifier = new HsaFileVerifierImpl(hsaFile.absolutePath)

    verifier.validateFileAgainstXSD()

    try {
        validateAgainstCurrentHSAFile(verifier)
    } catch (NoSuchFileException e) {
        logger.info("Det finns ingen nuvarande hsa fil.")
    }

    String date = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(verifier.getCreationDate())
    logger.info("HSA filen {} är skapad  {}", hsaFile.name, date)

    changeSymlinksToHSAFiles(hsaFile)
}

private static void validateAgainstCurrentHSAFile(HsaFileVerifierImpl verifier) {
    String currentHsaFile = getCurrentHSAFile()
    logger.info("Nuvarande hsa link är " + currentHsaFile)
    int diff = verifier.hsaCacheDiff(currentHsaFile)
    logger.info("Det är {} skillnader mellan gammal och ny hsa fil.", diff)

    int allowableDiff = Integer.parseInt(appProperties.getProperty("allowable.diff.hsa.file"))
    if (diff > allowableDiff) {
        throw new HSAException("Fel under validering. Antalet skillnader mellan ny och gammal hsa fil är " + diff + ". Max antal tillåtna är " + allowableDiff)
    }
}

private static Properties downloadProperties() {
    String propertiesFileName = System.getProperty("hsa.skript.config")
    File propertiesFile = new File(propertiesFileName)
    logger.debug("Load properties from file " + propertiesFile.absolutePath)

    Properties properties = new Properties()

    propertiesFile.withInputStream {
        properties.load(it)
    }

    properties
}

private static void downloadHSAFilesFromServer() {
    String serverURL = appProperties.getProperty("hsa.file.url")
    String hsaZipFileName = appProperties.getProperty("hsa.file.name")
    String trustStoreFile = appProperties.getProperty("truststore.path")
    String trustStorePass = appProperties.getProperty("truststore.password")
    String keyStoreFile = appProperties.getProperty("keystore.path")
    String keyStorePass = appProperties.getProperty("keystore.password")

    logger.debug("Laddar ner hsa filen " + hsaZipFileName + " från servern " + serverURL)
    KeyStore trustStore = KeyStore.getInstance("JKS")
    InputStream stream = new FileInputStream(new File(trustStoreFile))
    trustStore.load(stream, (trustStorePass).toCharArray())


    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm(), "SunJSSE")
    trustManagerFactory.init(trustStore)


    X509TrustManager x509TrustManager = null;
    for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
        if (trustManager instanceof X509TrustManager) {
            x509TrustManager = (X509TrustManager) trustManager
            break
        }
    }

    if (x509TrustManager == null) {
        throw new NullPointerException()
    }

    def trustManagers = new TrustManager[1]
    trustManagers[0] = x509TrustManager

    //keystore
    KeyStore keyStore = KeyStore.getInstance("JKS")
    InputStream stream2 = new FileInputStream(new File(keyStoreFile))
    keyStore.load(stream2, keyStorePass.toCharArray())

    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm(), "SunJSSE")
    keyManagerFactory.init(keyStore, keyStorePass.toCharArray())

    X509KeyManager x509KeyManager = null
    for (KeyManager keyManager : keyManagerFactory.getKeyManagers()) {
        if (keyManager instanceof X509KeyManager) {
            x509KeyManager = (X509KeyManager) keyManager
            break
        }
    }

    if (X509KeyManager == null) {
        throw new NullPointerException()
    }

    def keyManagers = new KeyManager[1]
    keyManagers[0] = x509KeyManager


    SSLContext sslContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagers, trustManagers, null)
    String[] supportedProtocols = ["TLSv1"].toArray()
    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, supportedProtocols, null, SSLConnectionSocketFactory.getDefaultHostnameVerifier())
    CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build()

    try {

        HttpGet httpget = new HttpGet(serverURL + hsaZipFileName)
        CloseableHttpResponse response = httpclient.execute(httpget)
        try {
            HttpEntity entity = response.getEntity()
            new File(hsaZipFileName) << entity.content

            EntityUtils.consume(entity)
        } finally {
            response.close()
        }
    } finally {
        httpclient.close()
    }
}

private static void sendProblemMail(Exception e) {
    Session session = Session.getDefaultInstance(appProperties)
    MimeMessage msg = new MimeMessage(session)
    def text = appProperties.getProperty("alert.mail.text")
    msg.setText(String.format(text, ExceptionUtils.getStackTrace(e)));

    sendMail(msg)

}

private static void sendMail(MimeMessage msg) {
    if (!Boolean.parseBoolean(appProperties.getProperty("send.mail"))) return

    def host = appProperties.getProperty("mail.smtp.host")
    def port = Integer.parseInt(appProperties.getProperty("mail.smtp.port"))
    def login = appProperties.getProperty("mail.smtp.login")
    def password = appProperties.getProperty("mail.smtp.password")
    def recipient = appProperties.getProperty("to.mail")
    def subject = appProperties.getProperty("alert.mail.subject")

    msg.setSubject(subject)
    msg.setFrom(new InternetAddress(login))
    msg.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient))

    Session session = Session.getDefaultInstance(appProperties)
    Transport transport = session.getTransport("smtps")
    transport.connect(host, port, login, password)
    transport.sendMessage(msg, msg.getAllRecipients())
    transport.close()

    logger.info("Mail skickad till support {}'", recipient)

}

private static List<String> getVPServerUrls() {
    List<String> urls = new ArrayList<>();
    String url = appProperties.getProperty("reset.HSA.cache.url")
    if (url != null && !url.isEmpty()) {
        urls.add(url);
    }

    int index = 1;
    while (true) {
        url = appProperties.getProperty("reset.HSA.cache.url." + index);
        if (url != null && !url.isEmpty()) {
            urls.add(url);
        } else {
            break;
        }
        index++;
    }
    urls
}


private static void resetHSACache() {
    List<String> urls = getVPServerUrls();
    if (urls.isEmpty()) {
        throw new HSAException("No VP servers to reset configured");
    }

    for (url in urls) {
        def result = new URL(url).text
        logger.info("Reset on '" + url + "' returns:\n" + result)

        if (!result.contains("Successfully reset HSA cache")) {
            throw new HSAException(result)
        }
    }
}


class HSAException extends Exception {
    HSAException(String msg) {
        super(msg)
    }
}