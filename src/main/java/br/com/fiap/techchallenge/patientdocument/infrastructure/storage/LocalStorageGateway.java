package br.com.fiap.techchallenge.patientdocument.infrastructure.storage;

import br.com.fiap.techchallenge.patientdocument.application.exception.StorageException;
import br.com.fiap.techchallenge.patientdocument.application.storage.command.StoreFileCommand;
import br.com.fiap.techchallenge.patientdocument.application.storage.gateway.StorageGateway;
import br.com.fiap.techchallenge.patientdocument.application.storage.result.StoredFile;
import br.com.fiap.techchallenge.patientdocument.application.storage.result.StoredFileContent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class LocalStorageGateway implements StorageGateway {

    private final Path rootPath;

    public LocalStorageGateway(@Value("${app.storage.local-path}") String localPath) {
        this.rootPath = Path.of(localPath);
    }

    @Override
    public StoredFile store(StoreFileCommand command) {
        try {
            Files.createDirectories(rootPath);

            String originalFileName = sanitizeFileName(command.originalFileName());
            String storedFileName = UUID.randomUUID() + getExtension(originalFileName);

            Path targetPath = rootPath.resolve(storedFileName).normalize();

            Files.copy(command.inputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return new StoredFile(
                    originalFileName,
                    storedFileName,
                    targetPath.toString(),
                    command.contentType(),
                    command.fileSize()
            );
        } catch (IOException exception) {
            throw new StorageException("Erro ao armazenar arquivo localmente.", exception);
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
            Path filePath = Path.of(storagePath);

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                throw new StorageException("Arquivo não encontrado no armazenamento local.");
            }

            byte[] content = Files.readAllBytes(filePath);

            return new StoredFileContent(content, content.length);
        } catch (IOException exception) {
            throw new StorageException("Não foi possível ler o arquivo armazenado.", exception);
        }
    }

    @Override
    public void delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new StorageException(
                    "O caminho do arquivo armazenado não foi informado."
            );
        }

        Path normalizedRootPath =
                rootPath.toAbsolutePath().normalize();

        Path normalizedFilePath =
                Path.of(storagePath).toAbsolutePath().normalize();

        if (!normalizedFilePath.startsWith(normalizedRootPath)) {
            throw new StorageException(
                    "Não é permitido excluir um arquivo fora "
                            + "do diretório de armazenamento."
            );
        }

        try {
            Files.deleteIfExists(normalizedFilePath);
        } catch (IOException exception) {
            throw new StorageException(
                    "Não foi possível excluir o arquivo armazenado localmente.",
                    exception
            );
        }
    }
}
