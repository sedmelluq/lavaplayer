package com.sedmelluq.discord.lavaplayer.tools.io;

import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Builder for a trust manager with custom certificates.
 */
public class TrustManagerBuilder {
  private static final Logger log = LoggerFactory.getLogger(TrustManagerBuilder.class);

  private final List<Certificate> certificates = new ArrayList<>();

  /**
   * Add certificates from the default trust store
   * @return this
   * @throws Exception In case anything explodes.
   */
  public TrustManagerBuilder addBuiltinCertificates() throws Exception {
    TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    factory.init((KeyStore) null);

    X509TrustManager builtInTrustManager = findFirstX509TrustManager(factory);
    if (builtInTrustManager != null) {
      addFromTrustManager(builtInTrustManager);
    }
    return this;
  }

  /**
   * Add certificates from the specified resource directory, using {path}/bundled.txt and {path}/extended.txt as the
   * list of JKS file names to laoad from that directory.
   * @param path Path to the resource directory.
   * @return this
   * @throws Exception In case anything explodes.
   */
  public TrustManagerBuilder addFromResourceDirectory(String path) throws Exception {
    addFromResourceList(path, path + "/bundled.txt");
    addFromResourceList(path, path + "/extended.txt");
    return this;
  }

  /**
   * @return A trust manager with the loaded certificates.
   * @throws Exception In case anything explodes.
   */
  public X509TrustManager build() throws Exception {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(null, null);

    for (int i = 0; i < certificates.size(); i++) {
      keyStore.setCertificateEntry(String.valueOf(i), certificates.get(i));
    }

    TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    factory.init(keyStore);

    return findFirstX509TrustManager(factory);
  }

  private X509TrustManager findFirstX509TrustManager(TrustManagerFactory factory) {
    for (TrustManager trustManager : factory.getTrustManagers()) {
      if (trustManager instanceof X509TrustManager) {
        return (X509TrustManager) trustManager;
      }
    }

    return null;
  }

  private void addFromTrustManager(X509TrustManager trustManager) {
    for (Certificate certificate : trustManager.getAcceptedIssuers()) {
      certificates.add(certificate);
    }
  }

  private void addFromResourceList(String basePath, String listPath) throws Exception {
    InputStream listFileStream = TrustManagerBuilder.class.getResourceAsStream(listPath);

    if (listFileStream == null) {
      log.debug("Certificate list {} not present in classpath.", listPath);
      return;
    }

    try {
      for (String line : IOUtils.readLines(listFileStream, StandardCharsets.UTF_8)) {
        String fileName = line.trim();

        if (!fileName.isEmpty()) {
          addFromResourceFile(basePath + "/" + fileName);
        }
      }
    } finally {
      ExceptionTools.closeWithWarnings(listFileStream);
    }
  }

  private void addFromResourceFile(String resourcePath) throws Exception {
    InputStream fileStream = TrustManagerBuilder.class.getResourceAsStream(resourcePath);

    if (fileStream == null) {
      log.warn("Certificate {} not present in classpath.", resourcePath);
      return;
    }

    try {
      KeyStore keyStore = KeyStore.getInstance("JKS");
      keyStore.load(fileStream, null);
      addFromKeyStore(keyStore);
    } finally {
      ExceptionTools.closeWithWarnings(fileStream);
    }
  }

  private void addFromKeyStore(KeyStore keyStore) throws Exception {
    for (Enumeration<String> enumeration = keyStore.aliases(); enumeration.hasMoreElements(); ) {
      String alias = enumeration.nextElement();

      if (keyStore.isCertificateEntry(alias)) {
        certificates.add(keyStore.getCertificate(alias));
      }
    }
  }
}
