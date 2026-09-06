    package ca.maximilian.maxsfilehost;

    import org.apache.sshd.server.command.Command;
    import org.apache.sshd.server.Environment;
    import org.apache.sshd.server.ExitCallback;
    import org.apache.sshd.server.channel.ChannelSession;
    import org.apache.sshd.server.shell.ShellFactory;

    import java.io.IOException;
    import java.io.InputStream;
    import java.io.OutputStream;

    public class DisabledShellFactory implements ShellFactory {
        @Override
        public Command createShell(ChannelSession channel) throws IOException {
            return new Command() {
                private OutputStream out;
                private OutputStream err;
                private ExitCallback callback;

                @Override
                public void setInputStream(InputStream in) {}

                @Override
                public void setOutputStream(OutputStream out) {
                    this.out = out;
                }

                @Override
                public void setErrorStream(OutputStream err) {
                    this.err = err;
                }

                @Override
                public void setExitCallback(ExitCallback callback) {
                    this.callback = callback;
                }

                @Override
                public void start(ChannelSession channel, Environment env) throws IOException {
                    try {
                        String message = "\u0007\r\nSSH is disabled on this server\r\n\r\n";
                        if (err != null) {
                            err.write(message.getBytes());
                            err.flush();
                        } else if (out != null) {
                            out.write(message.getBytes());
                            out.flush();
                        }
                    } finally {
                        if (callback != null) {
                            callback.onExit(1);
                        }
                    }
                }

                @Override
                public void destroy(ChannelSession channel) {}
            };
        }
    }