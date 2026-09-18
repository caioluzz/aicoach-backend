package com.aicoach.backend;

import com.aicoach.backend.client.GarminBotClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BackendApplicationTests {
	@MockitoBean
	private GarminBotClient garminBotClient;

	@Test
	void contextLoads() {
	}

}
