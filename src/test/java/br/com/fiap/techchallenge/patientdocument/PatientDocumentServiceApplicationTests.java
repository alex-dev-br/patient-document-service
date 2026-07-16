package br.com.fiap.techchallenge.patientdocument;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(
		properties = {
				"app.messaging.kafka.enabled=false"
		}
)
@Import(TestcontainersConfiguration.class)
class PatientDocumentServiceApplicationTests {

	@Test
	void contextLoads() {
	}
}
