package org.Chess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"org.Chess", "org.Chess.ChessController", "org.Chess.MainLoopServer"})
public class ChessApplication {
    public static void main(String[] args) { // start main app
        SpringApplication.run(ChessApplication.class, args);
    }
}

//!!!!
//mvn spring-boot:run
//!!!!