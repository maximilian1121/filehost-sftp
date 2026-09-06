package ca.maximilian.maxsfilehost;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jgit.ignore.IgnoreNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@RestController
public class IndexController {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexController.class);
    private final Path baseDir = Paths.get("./files").toAbsolutePath().normalize();

    @GetMapping(value = "/", produces = {MediaType.APPLICATION_JSON_VALUE, "application/x-yaml", "text/yaml"})
    public ResponseEntity<?> getFileTree(
            @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE) String acceptHeader) {

        try {
            Path ignoreFile = baseDir.resolve("index_ignore.txt");
            IgnoreNode ignoreNode = new IgnoreNode();

            if (Files.exists(ignoreFile)) {
                try (InputStream in = Files.newInputStream(ignoreFile)) {
                    ignoreNode.parse(in);
                }
            }

            FileNode root = buildTree(baseDir, ignoreNode);

            if (acceptHeader.contains("yaml")) {
                ObjectMapper yamlMapper = new YAMLMapper();
                String yamlContent = yamlMapper.writeValueAsString(root);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, "application/x-yaml")
                        .body(yamlContent);
            }

            return ResponseEntity.ok(root);

        } catch (IOException ioException) {
            LOGGER.error("Error generating file tree", ioException);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading file tree");
        }
    }

    private FileNode buildTree(Path currentPath, IgnoreNode ignoreNode) throws IOException {
        Path relativePath = baseDir.relativize(currentPath);

        String pathStr = relativePath.toString().replace("\\", "/");

        if (!pathStr.isEmpty()) {
            if (pathStr.equals("index_ignore.txt")) {
                return null;
            }

            boolean isDirectory = Files.isDirectory(currentPath);

            Boolean isIgnored = ignoreNode.checkIgnored(pathStr, isDirectory);
            if (Boolean.TRUE.equals(isIgnored)) {
                return null;
            }
        }

        FileNode node = new FileNode(currentPath.getFileName().toString());

        if (Files.isDirectory(currentPath)) {
            node.setType("directory");
            List<FileNode> children = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath)) {
                for (Path entry : stream) {
                    FileNode childNode = buildTree(entry, ignoreNode);
                    if (childNode != null) {
                        children.add(childNode);
                    }
                }
            }
            node.setChildren(children);
        } else {
            node.setType("file");
        }

        return node;
    }

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FileNode {
        private String name;
        private String type;
        private List<FileNode> children;

        public FileNode(String name) { this.name = name; }

    }
}