import groovy.transform.Field
import org.apache.commons.lang.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.skl.tp.hsa.cache.HsaFileVerifierImpl
import java.nio.file.Paths

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;

import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

@Grapes([
        @Grab(group = 'com.sun.mail', module = 'javax.mail', version = '1.6.1'),
        @Grab(group = 'commons-lang', module = 'commons-lang', version = '2.6'),
        @Grab(group = 'ch.qos.logback', module = 'logback-classic', version = '1.2.3'),
        @Grab(group = 'net.logstash.logback', module = 'logstash-logback-encoder', version='6.4'),
        @Grab(group = 'io.kubernetes', module = 'client-java', version = '18.0.0'),
        @GrabConfig(systemClassLoader = true)
])



@Field
static Logger logger = LoggerFactory.getLogger("scriptLogger")

logger.info("Börjar updatera hsa cache.")

try {
    downloadHSAFilesFromServer()
    File hsaFile = unzipFilesToDir()[0]
    validateHSAFileAndChangeSymlink(hsaFile)
    resetHSACache()
    deleteOldHsaFiles(hsaFile)
    logger.info("Hsa cache är uppdaterad.")
} catch (Exception e) {
    logger.error(ExceptionUtils.getMessage(e), e)
    sendProblemMail(e)
}


private static void changeSymlinksToHSAFiles(File hsaFile) {
    def symlink = System.getenv("HSA_SYMLINK_FILE")
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
    def symlinkFileName = System.getenv("HSA_SYMLINK_FILE")
    File symlinkFile = new File(symlinkFileName);
    return Files.readSymbolicLink(symlinkFile.toPath())
}

private static File[] unzipFilesToDir() {
    def destDirName = System.getenv("HSA_FILES_DIR")
    def zipFileName = System.getenv("HSA_FILE_NAME")

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

    int allowableDiff = Integer.parseInt(System.getenv("HSA_FILE_ALLOWABLE_DIFF"))
    if (diff > allowableDiff) {
        throw new HSAException("Fel under validering. Antalet skillnader mellan ny och gammal hsa fil är " + diff + ". Max antal tillåtna är " + allowableDiff)
    }
}

private static void downloadHSAFilesFromServer() {
    String serverURL = System.getenv("HSA_FILE_URL")
    String hsaZipFileName = System.getenv("HSA_FILE_NAME")
    String caCert = System.getenv("HSA_CA_CERT")
    String tlsCert = System.getenv("HSA_TLS_CERT")
    String tlsKey = System.getenv("HSA_TLS_KEY")

    String curlCommand = "curl ${serverURL} --cacert ${caCert} --cert ${tlsCert} --key ${tlsKey} --output ${hsaZipFileName}"
    logger.debug("Hämtar fil med: " + curlCommand)

    Process proc= curlCommand.execute()
    def b = new StringBuffer()
    proc.consumeProcessErrorStream(b)
    proc.waitFor(30, TimeUnit.SECONDS)
    logger.debug(proc.text)

    if (proc.exitValue() != 0) {
        throw new HSAException("Failed to get HSA file: " + b.toString())
    }
}

private static void sendProblemMail(Exception e) {
    if (!Boolean.parseBoolean(System.getenv("HSA_SEND_ALERT_MAIL"))) {
        logger.info("Mailalert om problem med hsa uppdatering är avstängd!")
        return
    }

    try {
        Properties smtpProperties = downloadProperties()

        Session session = Session.getInstance(smtpProperties)

        MimeMessage msg = new MimeMessage(session)

        def text = System.getenv("ALERT_MAIL_TEXT")
        msg.setText(String.format(text, ExceptionUtils.getStackTrace(e)));

        def subject = System.getenv("ALERT_MAIL_SUBJECT")
        msg.setSubject(subject)

        def login = System.getenv("ALERT_MAIL_FROM")
        msg.setFrom(new InternetAddress(login))

        def recipients = System.getenv("ALERT_MAIL_TO")
        String [] mailAddress = recipients.split("\\s*,\\s*")
        for(String currentAddress:mailAddress){
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(currentAddress.trim()))
        }

        Transport.send(msg);

        logger.info("Mail skickad till: {}'", recipients)
    } catch (Exception ex) {
        logger.error(ExceptionUtils.getStackTrace(ex))
    }
}


private static List<String> getVPServerUrls() {
    List<String> urls = new ArrayList<>();

    try {
        ApiClient client = Config.defaultClient();
        CoreV1Api api = new CoreV1Api(client);

        String podNamespace = System.getenv("HSA_RESET_POD_NAMESPACE")
        String labelSelector = System.getenv("HSA_RESET_LABEL_SELECTOR")
        String urlFormat = System.getenv("HSA_RESET_URL_FORMAT")
        int timeout = Integer.parseInt(System.getenv("HSA_RESET_LOOKUP_TIMEOUT") ?: "10")

        V1PodList list = api.listNamespacedPod(podNamespace, null, false, null, null, labelSelector, null, null, null, timeout, false);

        list.getItems().each { item ->
            logger.debug("Name: {} IP: {} Phase: {}", item.getMetadata().getName(), item.getStatus().getPodIP(), item.getStatus().getPhase())
            if (item.getStatus().getPhase().equals("Running")) {
                urls.add(String.format(urlFormat, item.getStatus().getPodIP()))
            }
        }

        urls
    }
    catch (Exception ex) {
        logger.error(ExceptionUtils.getStackTrace(ex))
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

private static void deleteOldHsaFiles(File hsaFileToKeep) {
    logger.info("Tar bort gammla hsa-filer")
    def hsaFilesDirName = System.getenv("HSA_FILES_DIR")
    def dir = new File(hsaFilesDirName)

    dir.eachFile { file ->
        if (!file.equals(hsaFileToKeep) && !file.isDirectory() &&
                !Files.isSymbolicLink(Paths.get(file.absolutePath))) {
            file.delete()
            logger.info("Tar bort file " + Paths.get(file.absolutePath))
        }
    }
}

private static Properties downloadProperties() {
    String propertiesString = System.getenv("SMTP_PROPERTIES")
    Properties properties = new Properties()
    properties.load(new StringReader(propertiesString))

    properties
}

class HSAException extends Exception {
    HSAException(String msg) {
        super(msg)
    }
}