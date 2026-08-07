package com.unibank.bankingSystem;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BankingSystemApplicationTests {

	// RabbitMQ auto-config is excluded in tests (see src/test/resources/application.properties),
	// so AuthService's RabbitTemplate dependency needs a stand-in for the context to start.
	@MockitoBean
	private RabbitTemplate rabbitTemplate;

	@Test
	void contextLoads() {
	}

}
