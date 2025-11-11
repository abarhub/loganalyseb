package org.loganalyseb.loganalyseb.properties;

import lombok.Data;

@Data
public class PushGatewayProperties {

    private String url;
    private String jobName;
    private boolean actif;

}
