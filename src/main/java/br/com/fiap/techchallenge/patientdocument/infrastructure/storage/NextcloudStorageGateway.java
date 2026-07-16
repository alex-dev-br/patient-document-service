package br.com.fiap.techchallenge.patientdocument.infrastructure.storage;

import br.com.fiap.techchallenge.patientdocument.application.exception.StorageException;
import br.com.fiap.techchallenge.patientdocument.application.storage.command.StoreFileCommand;
import br.com.fiap.techchallenge.patientdocument.application.storage.gateway.StorageGateway;
import br.com.fiap.techchallenge.patientdocument.application.storage.result.StoredFile;
import br.com.fiap.techchallenge.patientdocument.application.storage.result.StoredFileContent;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component("nextcloudStorageGateway")
public class NextcloudStorageGateway implements StorageGateway {

    public static final ZoneId SP_ZONE_ID = ZoneId.of("America/Sao_Paulo");

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${nextcloud.webdav.url}")
    private String baseUrl;

    @Value("${nextcloud.username}")
    private String username;

    @Value("${nextcloud.app-password}")
    private String appPassword;


    @Override
    public StoredFile store(StoreFileCommand command) {
        try (var file = command.inputStream()) {
            String datePath = formatter.format(LocalDate.now(SP_ZONE_ID));

            String originalFileName = sanitizeFileName(command.originalFileName());
            String storedFileName = UUID.randomUUID() + getExtension(originalFileName);

            Sardine sardine = SardineFactory.begin(username, appPassword);
            var folder = baseUrl + datePath;
            if (!sardine.exists(folder)) {
                sardine.createDirectory(folder);
            }
            var urlFinal = URI.create(folder + "/" + storedFileName).normalize();
            byte[] fileBytes = file.readAllBytes();
            sardine.put(urlFinal.toString(), fileBytes);

            return new StoredFile(
                    originalFileName,
                    storedFileName,
                    urlFinal.toString(),
                    command.contentType(),
                    command.fileSize()
            );
        } catch (Exception exception) {
            throw new StorageException("Erro ao salvar o arquivo.", exception);
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "documento";
        }

        return Path.of(fileName).getFileName().toString();
    }

    private String getExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");

        if (lastDotIndex == -1) {
            return "";
        }

        return fileName.substring(lastDotIndex);
    }

    @Override
    public StoredFileContent load(String storagePath) {
        try {
            Sardine sardine = SardineFactory.begin(username, appPassword);
            if (!sardine.exists(storagePath)) {
                throw new StorageException("Arquivo não encontrado.");
            }
            try (var is = sardine.get(storagePath)) {
                byte[] content = is.readAllBytes();
                return new StoredFileContent(content, content.length);
            }
        } catch (Exception exception) {
            throw new StorageException("Não foi possível ler o arquivo armazenado.", exception);
        }
    }
}
