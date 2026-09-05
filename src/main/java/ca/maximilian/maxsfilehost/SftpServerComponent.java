package ca.maximilian.maxsfilehost;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.UnknownCommandFactory;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

@Component
public class SftpServerComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(SftpServerComponent.class);

    private SshServer sshd;

    @Value("${sftp.port:2222}")
    private int port;

    @Value("${sftp.username:admin}")
    private String username;

    @Value("${sftp.password:secret123}")
    private String password;

    private final Path baseDir = Paths.get("./files").toAbsolutePath().normalize();

    @PostConstruct
    public void startServer() throws IOException, IllegalStateException {
        if (password.equals("changeme")) {
            throw new IllegalStateException("You must change the password from changeme!");
        }

        if (!Files.exists(baseDir)) {
            Files.createDirectories(baseDir);
        }

        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);

        sshd.setShellFactory(null);
        sshd.setCommandFactory(UnknownCommandFactory.INSTANCE);

        sshd.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));

        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));

        sshd.setPasswordAuthenticator((user, pass, _) ->
                username.equals(user) && password.equals(pass)
        );

        sshd.setFileSystemFactory(new VirtualFileSystemFactory(baseDir));

        sshd.start();
        LOGGER.info("Started sftp server on port {}", port);
    }

    @PreDestroy
    public void stopServer() throws IOException {
        if (sshd != null && sshd.isOpen()) {
            sshd.stop(true);
            LOGGER.info("Stopped sftp server on port {}", port);
        }
    }
}
