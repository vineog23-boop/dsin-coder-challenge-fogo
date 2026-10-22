package br.com.dsin.challengefogo;

import org.springframework.boot.SpringApplication;

public class TestChallengefogoApplication {

	public static void main(String[] args) {
		SpringApplication.from(ChallengefogoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
