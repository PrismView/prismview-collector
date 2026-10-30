package com.prismview.collector;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local") // 이 테스트는 local 프로필로 진행
class CollectorApplicationTests {

	@Test
	void contextLoads() {
	}

}
