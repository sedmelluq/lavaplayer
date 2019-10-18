package com.sedmelluq.discord.lavaplayer.tools.http;

import com.sedmelluq.discord.lavaplayer.tools.IpAddressTools;
import com.sedmelluq.discord.lavaplayer.tools.IpBlock;
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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.function.Predicate;

/**
 * @author Frederik Arbjerg Mikkelsen
 */
@SuppressWarnings("WeakerAccess")
public class BalancingIpRoutePlanner implements HttpRoutePlanner {

    private static final Logger log = LoggerFactory.getLogger(BalancingIpRoutePlanner.class);
    private final IpBlock ipBlock;
    private final Predicate<InetAddress> ipFilter;
    private final SchemePortResolver schemePortResolver;

    /**
     * @param ipBlock the block to perform balancing over.
     */
    public BalancingIpRoutePlanner(IpBlock ipBlock) {
        this(ipBlock, i -> {
            return true;
        }, DefaultSchemePortResolver.INSTANCE);
    }

    /**
     * @param ipBlock  the block to perform balancing over.
     * @param ipFilter function to filter out certain IP addresses picked from the IP block, causing another random to be chosen.
     */
    public BalancingIpRoutePlanner(IpBlock ipBlock, Predicate<InetAddress> ipFilter) {
        this(ipBlock, ipFilter, DefaultSchemePortResolver.INSTANCE);
    }

    /**
     * @param ipBlock            the block to perform balancing over.
     * @param ipFilter           function to filter out certain IP addresses picked from the IP block, causing another random to be chosen.
     * @param schemePortResolver for resolving ports for schemes where the port is not explicitly stated.
     */
    public BalancingIpRoutePlanner(IpBlock ipBlock, Predicate<InetAddress> ipFilter, SchemePortResolver schemePortResolver) {
        this.ipBlock = ipBlock;
        this.ipFilter = ipFilter;
        this.schemePortResolver = schemePortResolver;
    }

    @Override
    public HttpRoute determineRoute(HttpHost host, HttpRequest request, HttpContext context) throws HttpException {
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
            } catch (UnsupportedSchemeException e) {
                throw new HttpException(e.getMessage());
            }
        } else remotePort = host.getPort();

        final Tuple<Inet4Address, Inet6Address> remoteAddresses = IpAddressTools.getRandomAddressesFromHost(host);

        InetAddress localAddress;
        final InetAddress remoteAddress;
        if (ipBlock.getType() == Inet4Address.class) {
            if (remoteAddresses.l != null) {
                do {
                    localAddress = ipBlock.getRandomAddress();
                } while (!ipFilter.test(localAddress));
                remoteAddress = remoteAddresses.l;
            } else {
                throw new HttpException("Could not resolve " + host.getHostName());
            }
        } else if (ipBlock.getType() == Inet6Address.class) {
            if (remoteAddresses.r != null) {
                localAddress = ipBlock.getRandomAddress();
                remoteAddress = remoteAddresses.r;
            } else if (remoteAddresses.l != null) {
                localAddress = null;
                remoteAddress = remoteAddresses.l;
                log.warn("Could not look up AAAA record for {}. Falling back to unbalanced IPv4.", host.getHostName());
            } else {
                throw new HttpException("Could not resolve " + host.getHostName());
            }
        } else {
            throw new HttpException("Unknown IpBlock type: " + ipBlock.getType().getCanonicalName());
        }


        HttpHost target = new HttpHost(remoteAddress, host.getHostName(), remotePort, host.getSchemeName());
        HttpHost proxy = config.getProxy();
        final boolean secure = target.getSchemeName().equalsIgnoreCase("https");
        if (proxy == null) {
            return new HttpRoute(target, localAddress, secure);
        } else {
            return new HttpRoute(target, localAddress, proxy, secure);
        }
    }
}
