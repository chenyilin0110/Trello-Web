package com.smb.trelloExport;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(value = "com.smb.trelloExport.mapper")
public class SmbReportApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmbReportApplication.class, args);
    }
}
