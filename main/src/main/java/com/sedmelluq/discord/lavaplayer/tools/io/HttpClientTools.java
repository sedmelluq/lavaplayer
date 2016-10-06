package com.sedmelluq.discord.lavaplayer.tools.io;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Tools for working with HttpClient
 */
public class HttpClientTools {
  /**
   * @return An HttpClientBuilder which uses the same cookie store for all clients
   */
  public static HttpClientBuilder createSharedCookiesHttpBuilder() {
    CookieStore cookieStore = new BasicCookieStore();
    HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
    httpClientBuilder.setDefaultCookieStore(cookieStore);
    return httpClientBuilder;
  }
}
