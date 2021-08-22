package com.sedmelluq.lava.extensions.iprotator.planner;

import com.sedmelluq.lava.extensions.iprotator.tools.ip.IpBlock;
import kotlin.Pair;
import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Frederik Arbjerg Mikkelsen
 */
@SuppressWarnings("WeakerAccess")
public class BalancingIpRoutePlanner extends AbstractRoutePlanner {

    private static final Logger log = LoggerFactory.getLogger(BalancingIpRoutePlanner.class);
    private final Predicate<InetAddress> ipFilter;

    /**
     * @param ipBlocks the block to perform balancing over.
     */
    public BalancingIpRoutePlanner(List<IpBlock> ipBlocks) {
        this(ipBlocks, i -> true);
    }

    /**
     * @param ipBlocks the block to perform balancing over.
     * @param ipFilter function to filter out certain IP addresses picked from the IP block, causing another random to be chosen.
     */
    public BalancingIpRoutePlanner(List<IpBlock> ipBlocks, Predicate<InetAddress> ipFilter) {
        this(ipBlocks, ipFilter, true);
    }

    /**
     * @param ipBlocks            the block to perform balancing over.
     * @param ipFilter            function to filter out certain IP addresses picked from the IP block, causing another random to be chosen.
     * @param handleSearchFailure whether a search 429 should trigger the ip as failing
     */
    public BalancingIpRoutePlanner(List<IpBlock> ipBlocks, Predicate<InetAddress> ipFilter, boolean handleSearchFailure) {
        super(ipBlocks, handleSearchFailure);
        this.ipFilter = ipFilter;
    }

    @Override
    protected Pair<InetAddress, InetAddress> determineAddressPair(Pair<Inet4Address, Inet6Address> remoteAddresses) throws HttpException {
        InetAddress localAddress;
        final InetAddress remoteAddress;
        if (ipBlock.getType() == Inet4Address.class) {
            if (remoteAddresses.getSecond() != null) {
                localAddress = getRandomAddress(ipBlock);
                remoteAddress = remoteAddresses.getFirst();
            } else {
                throw new HttpException("Could not resolve host");
            }
        } else if (ipBlock.getType() == Inet6Address.class) {
            if (remoteAddresses.getSecond() != null) {
                localAddress = getRandomAddress(ipBlock);
                remoteAddress = remoteAddresses.getSecond();
            } else if (remoteAddresses.getFirst() != null) {
                localAddress = null;
                remoteAddress = remoteAddresses.getFirst();
                log.warn("Could not look up AAAA record for host. Falling back to unbalanced IPv4.");
            } else {
                throw new HttpException("Could not resolve host");
            }
        } else {
            throw new HttpException("Unknown IpBlock type: " + ipBlock.getType().getCanonicalName());
        }
        return new Pair<>(localAddress, remoteAddress);
    }

    private InetAddress getRandomAddress(final IpBlock ipBlock) {
        InetAddress localAddress;
        BigInteger it = BigInteger.valueOf(0);
        do {
            if (ipBlock.getSize().multiply(BigInteger.valueOf(2)).compareTo(it) < 0) {
                throw new RuntimeException("Can't find a free ip");
            }
            it = it.add(BigInteger.ONE);
            localAddress = ipBlock.getRandomAddress();
        } while (localAddress == null || !ipFilter.test(localAddress) || !isValidAddress(localAddress));
        return localAddress;
    }
}
