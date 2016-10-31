package com.sedmelluq.discord.lavaplayer.tools.io;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

/**
 * Tools for working with HttpClient
 */
public class HttpClientTools {
  private static final Logger log = LoggerFactory.getLogger(HttpClientTools.class);

  private static final SSLContext sslContext = setupSslContext();

  /**
   * @return An HttpClientBuilder which uses the same cookie store for all clients
   */
  public static HttpClientBuilder createSharedCookiesHttpBuilder() {
    CookieStore cookieStore = new BasicCookieStore();
    HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
    httpClientBuilder.setDefaultCookieStore(cookieStore);

    if (sslContext != null) {
      httpClientBuilder.setSSLContext(sslContext);
    }

    return httpClientBuilder;
  }

  private static SSLContext setupSslContext() {
    try {
      X509TrustManager trustManager = new TrustManagerBuilder()
          .addBuiltinCertificates()
          .addFromResourceDirectory("/certificates")
          .build();

      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, new X509TrustManager[] { trustManager }, null);
      return context;
    } catch (Exception e) {
      log.error("Failed to build custom SSL context, using default one.", e);
      return null;
    }
  }
}
