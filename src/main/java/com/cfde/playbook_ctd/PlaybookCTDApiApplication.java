package com.cfde.playbook_ctd;

import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PlaybookCTDApiApplication {
	private static Logger logger = Logger.getLogger(PlaybookCTDApiApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(PlaybookCTDApiApplication.class, args);
		logger.info("------------------------ Starting Playbook CTD API ------------------------");
	}
}
