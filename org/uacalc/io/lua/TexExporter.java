package org.uacalc.io.lua;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class TexExporter {

    private final String inputString;

    public TexExporter(String inputString) {
        this.inputString = inputString;
    }

    /**
     * Runs the Lua exporter.lua script, sending the input string to its stdin,
     * and returns the full output from Lua as a String.
     */
    public String runLua() throws IOException, InterruptedException {

        //System.out.println("Working directory = " + System.getProperty("user.dir"));
        //String luaScript = Files.readString(Paths.get("org/uacalc/io/lua/exporter.lua"));
        InputStream in = TikzExporter.class.getResourceAsStream("/org/uacalc/io/lua/TexExporter.lua");
        File tmp = File.createTempFile("TexExporter", ".lua");
        tmp.deleteOnExit();
        Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        ProcessBuilder pb = new ProcessBuilder("lua", tmp.getAbsolutePath());

        Process process = pb.start();


        // Write some input to Lua process stdin
        try (BufferedWriter luaInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            System.out.println(inputString);
            luaInput.write(inputString);
            luaInput.flush();
            luaInput.close();  // signal EOF
        }

        // Read output from Lua
        StringBuilder outputBuilder = new StringBuilder();
        try (BufferedReader luaOutput = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = luaOutput.readLine()) != null) {
                outputBuilder.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Lua process exited with non-zero code: " + exitCode);
        }
        System.out.println("Lua exited with code " + exitCode);
        return outputBuilder.toString();
    }
}
