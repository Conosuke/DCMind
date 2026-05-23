package com.dcmind;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.dcmind.support.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class) // TestcontainersConfiguration를 import하여 테스트 컨테이너 설정을 적용
class DcmindBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
