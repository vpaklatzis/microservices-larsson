package com.util.http;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServiceUtil {
    private static final Logger log = LoggerFactory.getLogger(ServiceUtil.class);
    private final String port;
    private String serviceAddress = null;
    private String hostname = null;
    private String ipAddress = null;

    public ServiceUtil(@Value("${server.port}") String port) {
        this.port = port;
    }

    public String getServiceAddress() {
        if (serviceAddress == null) {
            try {
                serviceAddress = findMyHostname() + "/" + findMyIpAddress() + ":" + port;
            } catch (Exception e) {
                log.warn("Couldn't obtain the service address: " + e.getMessage());
            }
        }
        return serviceAddress;
    }

    private String findMyHostname() {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("Couldn't obtain the hostname: " + e.getMessage());
        }
        return hostname;
    }

    private String findMyIpAddress() {
        try {
            ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.warn("Couldn't obtain the ip address: " + e.getMessage());
        }
        return ipAddress;
    }
}