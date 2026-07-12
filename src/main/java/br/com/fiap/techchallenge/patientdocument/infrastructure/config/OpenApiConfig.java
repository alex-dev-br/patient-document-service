package br.com.fiap.techchallenge.patientdocument.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI patientDocumentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Meu Histórico de Saúde API")
                        .description("""
                                API para cadastro de pacientes, envio e consulta de documentos
                                de saúde, organização da timeline médica e atualização dos
                                resultados produzidos pelo serviço de inteligência artificial.
                                """)
                        .version("1.0.0")
                );
    }
}
