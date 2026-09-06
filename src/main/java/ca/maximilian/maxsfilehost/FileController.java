package ca.maximilian.maxsfilehost;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RestController
public class FileController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class);

    private final Path baseDir = Paths.get("./files").toAbsolutePath().normalize();

    @GetMapping("/**")
    public ResponseEntity<Object> getFile(
            HttpServletRequest request,
            @RequestParam(value = "dl", required = false) String dl,
            @RequestParam(value = "hash", required = false) HashAlgorithm hashAlgorithm
    ) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String filename = path != null ? UriUtils.decode(path.replaceFirst("^/", ""), StandardCharsets.UTF_8) : "";

        try {
            if (!baseDir.toFile().exists()) {
                Files.createDirectories(baseDir);
            }

            Path targetPath = baseDir.resolve(filename).normalize();

            if (!targetPath.startsWith(baseDir)) {
                return ResponseEntity.status(403).body(null);
            }

            Resource resource = new UrlResource(targetPath.toUri());

            if (resource.exists() && resource.isReadable()) {
                if (hashAlgorithm != null) {
                    String fileHash = calculateFileHash(targetPath, hashAlgorithm.getAlgorithmName());
                    ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                            .contentType(MediaType.TEXT_PLAIN);

                    LOGGER.info("Serving file hash at {} with algorithm {}", targetPath, hashAlgorithm);

                    return responseBuilder.body(fileHash);
                }

                String disposition = (dl != null) ? "attachment" : "inline";

                MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                        .orElse(MediaType.APPLICATION_OCTET_STREAM);

                ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                        .contentType(mediaType)
                        .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + resource.getFilename() + "\"");

                LOGGER.info("Serving file at {}", targetPath);

                return responseBuilder.body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException | NoSuchAlgorithmException e) {
            LOGGER.error("Error processing file request", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String calculateFileHash(Path filePath, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        try (BufferedInputStream fis = new BufferedInputStream(Files.newInputStream(filePath))) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}