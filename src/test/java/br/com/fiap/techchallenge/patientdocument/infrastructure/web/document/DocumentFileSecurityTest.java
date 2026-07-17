package br.com.fiap.techchallenge.patientdocument.infrastructure.web.document;

import br.com.fiap.techchallenge.patientdocument.application.document.result.HealthDocumentFile;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.DownloadHealthDocumentFileUseCase;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.FindHealthDocumentByIdUseCase;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.ListProcessedDocumentResultsUseCase;
import br.com.fiap.techchallenge.patientdocument.application.document.usecase.UpdateDocumentAiResultUseCase;
import br.com.fiap.techchallenge.patientdocument.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
@Import(SecurityConfig.class)
class DocumentFileSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindHealthDocumentByIdUseCase findHealthDocumentByIdUseCase;

    @MockitoBean
    private UpdateDocumentAiResultUseCase updateDocumentAiResultUseCase;

    @MockitoBean
    private DownloadHealthDocumentFileUseCase downloadHealthDocumentFileUseCase;

    @MockitoBean
    private ListProcessedDocumentResultsUseCase listProcessedDocumentResultsUseCase;

    @MockitoBean
    private HealthDocumentWebMapper healthDocumentWebMapper;

    @MockitoBean
    private ProcessedDocumentResultWebMapper processedDocumentResultWebMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturnUnauthorizedWhenTokenIsMissing() throws Exception {
        UUID documentId = UUID.randomUUID();

        mockMvc.perform(get("/documents/{documentId}/file", documentId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(downloadHealthDocumentFileUseCase);
    }

    @Test
    void shouldReturnForbiddenWhenTokenDoesNotHaveFileReadScope() throws Exception {
        UUID documentId = UUID.randomUUID();

        mockMvc.perform(get("/documents/{documentId}/file", documentId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("SCOPE_documents:read")
                        )))
                .andExpect(status().isForbidden());

        verifyNoInteractions(downloadHealthDocumentFileUseCase);
    }

    @Test
    void shouldReturnFileWhenTokenHasFileReadScope() throws Exception {
        UUID documentId = UUID.randomUUID();
        byte[] content = "documento".getBytes();

        when(downloadHealthDocumentFileUseCase.execute(documentId))
                .thenReturn(new HealthDocumentFile(
                        "exame.txt",
                        "text/plain",
                        content.length,
                        content
                ));

        mockMvc.perform(get("/documents/{documentId}/file", documentId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority(
                                        "SCOPE_documents:file:read"
                                )
                        )))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain"))
                .andExpect(content().bytes(content))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment")
                ))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString("filename=\"exame.txt\"")
                ));
    }
}
