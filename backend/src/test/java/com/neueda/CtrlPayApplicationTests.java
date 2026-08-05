package com.neueda;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"spring.sql.init.mode=never",
	"spring.datasource.hikari.initialization-fail-timeout=-1",
	"management.health.db.enabled=false"
})
class CtrlPayApplicationTests {

	@Test
	void contextLoads() {
	}

}
