package br.com.dsin.challengefogo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ChallengefogoApplicationTests {

	@Test
	void contextLoads() {
	}

}
