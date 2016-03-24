package com.graphhopper.converter.api;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Peter Karich
 */
public class IPFilter implements Filter {

    private final Logger logger = LoggerFactory.getLogger(IPFilter.class);
    private final Set<String> whites;
    private final Set<String> blacks;

    public IPFilter(String whiteList, String blackList) {
        whites = createSet(whiteList.split(","));
        blacks = createSet(blackList.split(","));
        if (!whites.isEmpty()) {
            logger.debug("whitelist:" + whites);
        }
        if (!blackList.isEmpty()) {
            logger.debug("blacklist:" + blacks);
        }

        if (!blacks.isEmpty() && !whites.isEmpty()) {
            String message = "blacklist and whitelist at the same time?";
            throw new IllegalArgumentException(message);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String ip = request.getRemoteAddr();
        if (accept(ip)) {
            chain.doFilter(request, response);
        } else {
            logger.warn("ip rejected: " + ip);
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    public boolean accept(String ip) {
        if (whites.isEmpty() && blacks.isEmpty()) {
            return true;
        }

        if (!whites.isEmpty()) {
            for (String w : whites) {
                if (simpleMatch(ip, w)) {
                    return true;
                }
            }
            return false;
        }

        if (blacks.isEmpty()) {
            throw new IllegalStateException("cannot happen");
        }

        for (String b : blacks) {
            if (simpleMatch(ip, b)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }

    private Set<String> createSet(String[] split) {
        Set<String> set = new HashSet<String>(split.length);
        for (String str : split) {
            str = str.trim();
            if (!str.isEmpty()) {
                set.add(str);
            }
        }
        return set;
    }

    public boolean simpleMatch(String ip, String pattern) {
        String[] ipParts = pattern.split("\\*");
        for (String ipPart : ipParts) {
            int idx = ip.indexOf(ipPart);
            if (idx == -1) {
                return false;
            }

            ip = ip.substring(idx + ipPart.length());
        }

        return true;
    }
}
