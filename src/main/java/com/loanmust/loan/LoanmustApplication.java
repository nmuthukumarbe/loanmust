package com.loanmust.loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LoanmustApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanmustApplication.class, args);
    }
}
