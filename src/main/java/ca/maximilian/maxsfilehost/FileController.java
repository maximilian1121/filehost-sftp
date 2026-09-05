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

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class FileController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class);

    private final Path baseDir = Paths.get("./files").toAbsolutePath().normalize();

    @GetMapping("/**")
    public ResponseEntity<Resource> getFile(
            HttpServletRequest request,
            @RequestParam(value = "dl", required = false) String dl) {

        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String filename = path != null ? path.replaceFirst("^/", "") : "";

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
                String disposition = (dl != null) ? "attachment" : "inline";

                MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                        .orElse(MediaType.APPLICATION_OCTET_STREAM);

                LOGGER.info("Serving file at {}", targetPath);

                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}