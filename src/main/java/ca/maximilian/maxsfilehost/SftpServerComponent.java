package ca.maximilian.maxsfilehost;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.sshd.common.config.keys.AuthorizedKeyEntry;
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
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    @Value("${sftp.password-auth-enabled:true}")
    private boolean passwordAuthEnabled;
    @Value("${sftp.authorized-keys-file:authorized_keys}")
    private String authorizedKeysFile;

    private final Path baseDir = Paths.get("./files").toAbsolutePath().normalize();

    @PostConstruct
    public void startServer() throws IOException, IllegalStateException {
        if (passwordAuthEnabled && password.equals("changeme")) {
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

        if (passwordAuthEnabled) {
            sshd.setPasswordAuthenticator((user, pass, _) ->
                    username.equals(user) && password.equals(pass)
            );
        }

        Path authorizedKeysPath = Paths.get(authorizedKeysFile).toAbsolutePath().normalize();
        boolean pubkeyEnabled = Files.exists(authorizedKeysPath);
        if (pubkeyEnabled) {
            List<PublicKey> allowedKeys = loadAuthorizedKeys(authorizedKeysPath);
            sshd.setPublickeyAuthenticator((user, key, _) ->
                    username.equals(user) && allowedKeys.stream().anyMatch(k -> k.equals(key))
            );
            LOGGER.info("Loaded {} authorized key(s) from {}", allowedKeys.size(), authorizedKeysPath);
        } else {
            LOGGER.warn("Authorized keys file {} not found, pubkey auth disabled", authorizedKeysPath);
        }

        if (!passwordAuthEnabled && !pubkeyEnabled) {
            throw new IllegalStateException("Password auth is disabled and no authorized_keys file was found — no auth method available!");
        }

        sshd.setFileSystemFactory(new VirtualFileSystemFactory(baseDir));
        sshd.start();
        LOGGER.info("Started sftp server on port {} (password auth: {}, pubkey auth: {})", port, passwordAuthEnabled, pubkeyEnabled);
    }

    private List<PublicKey> loadAuthorizedKeys(Path path) throws IOException {
        List<PublicKey> keys = new ArrayList<>();
        for (AuthorizedKeyEntry entry : AuthorizedKeyEntry.readAuthorizedKeys(path)) {
            try {
                keys.add(entry.resolvePublicKey(null, Collections.emptyMap(), null));
            } catch (Exception e) {
                LOGGER.warn("Skipping unparseable authorized_keys entry: {}", e.getMessage());
            }
        }
        return keys;
    }

    @PreDestroy
    public void stopServer() throws IOException {
        if (sshd != null && sshd.isOpen()) {
            sshd.stop(true);
            LOGGER.info("Stopped sftp server on port {}", port);
        }
    }
}