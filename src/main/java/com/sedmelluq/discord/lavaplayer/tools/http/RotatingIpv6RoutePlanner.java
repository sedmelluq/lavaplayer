package com.sedmelluq.discord.lavaplayer.tools.http;

import com.sedmelluq.discord.lavaplayer.tools.Ipv6Block;
import com.sedmelluq.discord.lavaplayer.tools.Tuple;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.UnsupportedSchemeException;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.function.Predicate;

public final class RotatingIpv6RoutePlanner implements HttpRoutePlanner {

    private static RotatingIpv6RoutePlanner instance = null;
    private static final Logger log = LoggerFactory.getLogger(BalancingIpv6RoutePlanner.class);
    private static final Random random = new Random();
    private final Ipv6Block ipBlock;
    private final Predicate<Inet6Address> ipFilter;
    private final SchemePortResolver schemePortResolver;
    private Inet6Address currentAddress;
    private Inet6Address remoteAddress;
    private boolean next;
    private int index = 0;

    @Nullable
    public static RotatingIpv6RoutePlanner getInstance() {
        return instance;
    }

    /**
     * @param ipBlock the block to perform balancing over.
     */
    public RotatingIpv6RoutePlanner(final Ipv6Block ipBlock) {
        this(ipBlock, i -> true, DefaultSchemePortResolver.INSTANCE);
    }

    /**
     * @param ipBlock  the block to perform balancing over.
     * @param ipFilter function to filter out certain IP addresses picked from the IP block, causing another random to be chosen.
     */
    public RotatingIpv6RoutePlanner(final Ipv6Block ipBlock, final Predicate<Inet6Address> ipFilter) {
        this(ipBlock, ipFilter, DefaultSchemePortResolver.INSTANCE);
    }

    /**
     * @param ipBlock            the block to perform balancing over.
     * @param ipFilter           function to filter out certain IP addresses picked from the IP block, causing another random to be chosen.
     * @param schemePortResolver for resolving ports for schemes where the port is not explicitly stated.
     */
    public RotatingIpv6RoutePlanner(final Ipv6Block ipBlock, final Predicate<Inet6Address> ipFilter, final SchemePortResolver schemePortResolver) {
        this.ipBlock = ipBlock;
        this.ipFilter = ipFilter;
        this.schemePortResolver = schemePortResolver;
        instance = this;
    }

    public void next() {
        this.next = true;
    }

    @Override
    public HttpRoute determineRoute(final HttpHost host, final HttpRequest request, final HttpContext context) throws HttpException {
        Args.notNull(request, "Request");
        if (host == null) {
            throw new ProtocolException("Target host is not specified");
        }
        final HttpClientContext clientContext = HttpClientContext.adapt(context);
        final RequestConfig config = clientContext.getRequestConfig();
        int remotePort;
        if (host.getPort() <= 0) {
            try {
                remotePort = schemePortResolver.resolve(host);
            } catch (final UnsupportedSchemeException e) {
                throw new HttpException(e.getMessage());
            }
        } else
            remotePort = host.getPort();
        if (currentAddress != null && !next) {
            final HttpHost target = new HttpHost(remoteAddress, host.getHostName(), remotePort, host.getSchemeName());
            final HttpHost proxy = config.getProxy();
            final boolean secure = target.getSchemeName().equalsIgnoreCase("https");
            if (proxy == null) {
                return new HttpRoute(target, currentAddress, secure);
            } else {
                return new HttpRoute(target, currentAddress, proxy, secure);
            }
        }

        final InetAddress remoteAddress;
        Inet6Address localAddress;
        final Tuple<Inet4Address, Inet6Address> remoteAddresses = getRandomAdressesFromHost(host);

        if (remoteAddresses.r != null) {
            do {
                try {
                    localAddress = ipBlock.getSlash64AtIndex(index++);
                } catch (final IllegalArgumentException ex) {
                    log.warn("Reached end of CIDR block, starting from start again");
                    index = 0;
                    localAddress = null;
                }
            } while (localAddress == null || !ipFilter.test(localAddress));
            remoteAddress = remoteAddresses.r;
            log.info("Selected " + remoteAddress.toString() + " as new outgoing ip");
            this.currentAddress = localAddress;
            this.remoteAddress = remoteAddresses.r;
        } else if (remoteAddresses.l != null) {
            localAddress = null;
            remoteAddress = remoteAddresses.l;
            log.warn("Could not look up AAAA record for {}. Falling back to unbalanced IPv4.", host.getHostName());
        } else {
            throw new HttpException("Could not resolve " + host.getHostName());
        }

        final HttpHost target = new HttpHost(remoteAddress, host.getHostName(), remotePort, host.getSchemeName());
        final HttpHost proxy = config.getProxy();
        final boolean secure = target.getSchemeName().equalsIgnoreCase("https");
        if (proxy == null) {
            return new HttpRoute(target, localAddress, secure);
        } else {
            return new HttpRoute(target, localAddress, proxy, secure);
        }
    }

    private Tuple<Inet4Address, Inet6Address> getRandomAdressesFromHost(final HttpHost host) throws HttpException {
        final List<InetAddress> ipList;
        try {
            ipList = Arrays.asList(InetAddress.getAllByName(host.getHostName()));
        } catch (final UnknownHostException e) {
            throw new HttpException("Could not resolve " + host.getHostName(), e);
        }
        final List<Inet6Address> ip6 = new ArrayList<>();
        final List<Inet4Address> ip4 = new ArrayList<>();

        Collections.reverse(ipList);
        for (final InetAddress ip : ipList) {
            if (ip instanceof Inet6Address)
                ip6.add((Inet6Address) ip);
            else if (ip instanceof Inet4Address)
                ip4.add((Inet4Address) ip);
        }
        return new Tuple<>(getRandomFromList(ip4), getRandomFromList(ip6));
    }

    private <T> T getRandomFromList(final List<T> list) {
        if (list.isEmpty())
            return null;
        if (list.size() == 1)
            return list.get(0);
        return list.get(random.nextInt(list.size()));
    }
}
