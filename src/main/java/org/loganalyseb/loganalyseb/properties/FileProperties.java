package org.loganalyseb.loganalyseb.properties;

import lombok.Data;

import java.nio.file.Path;

@Data
public class FileProperties {

    private Path path;
    private String nomMetrics;

}
