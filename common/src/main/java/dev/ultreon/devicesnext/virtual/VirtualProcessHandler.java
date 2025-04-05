package dev.ultreon.devicesnext.virtual;

import dev.ultreon.devicesnext.mineos.VirtualComputer;
import org.graalvm.polyglot.io.ProcessHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class VirtualProcessHandler implements ProcessHandler {
    private final VirtualComputer virtualComputer;

    public VirtualProcessHandler(VirtualComputer virtualComputer) {
        this.virtualComputer = virtualComputer;
    }

    @Override
    public Process start(ProcessCommand command) throws IOException {
        String directory = command.getDirectory();
        List<String> command1 = command.getCommand();
        Map<String, String> environment = command.getEnvironment();
        Redirect errorRedirect = command.getErrorRedirect();
        Redirect outputRedirect = command.getOutputRedirect();
        Redirect inputRedirect = command.getInputRedirect();

        if (virtualComputer.processEvent == null) {
            throw new IOException("No process handler!");
        }

        try {
            Process capture = VirtualProcess.capture(() -> {
                try {
                    virtualComputer.processEvent.execute(command1, directory, environment, errorRedirect, outputRedirect, inputRedirect);
                } catch (Exception e) {
                    throw new IOException(e);
                }
                return null;
            });
            if (capture == null) {
                throw new IOException("Process capture failed!");
            }
            return capture;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to execute process", e);
        }
    }
}
