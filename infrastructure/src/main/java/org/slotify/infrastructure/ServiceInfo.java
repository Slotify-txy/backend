package org.slotify.infrastructure;

import lombok.Data;

@Data
public class ServiceInfo {
    private final String serviceName;
    private final Integer servicePort;
    private final String serviceAddress;
    private final Integer gRPCPort;
    private final String dbName;

    public ServiceInfo(String serviceName, Integer servicePort, Integer gRPCPort, String dbName) {
        this.serviceName = serviceName;
        this.servicePort = servicePort;
        this.gRPCPort = gRPCPort;
        this.dbName = dbName;
        this.serviceAddress = serviceName + ".slotify.local";
    }
}
