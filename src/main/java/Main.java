import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class Main {
    public static void main(String[] args) {
        CharStream inputStream = null;
        try {
            inputStream = CharStreams.fromFileName("src/main/resources/code_coroutine.dtl");
        } catch (IOException e) {
            e.printStackTrace();
        }

        var lexer = new DTlangLexer(inputStream);
        var tokenStream = new CommonTokenStream(lexer);

        String code;
        try {
            var parser = new DTlangParser(tokenStream);
            parser.removeErrorListeners();
            parser.addErrorListener(new SyntaxListener());
            var tree = parser.program();
            var visitor = new ProgramVisitor();
            code = visitor.visit(tree);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }

        String dirName = "out/";
        String fileName = dirName + "main.cpp";

        try {
            var dirPath = Path.of(dirName);
            var filePath = Path.of(fileName);

            if (Files.exists(dirPath)) {
                Files.walk(dirPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            Files.createDirectory(dirPath);
            Files.createFile(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try (var writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(code);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String outFileName = dirName + "yapis";
        try {
            String command = "g++ " + fileName + " -o " + outFileName + " -std=c++20";
            var proc = Runtime.getRuntime().exec(command);
            proc.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return;
        }
        if (!Files.exists(Path.of(outFileName))) {
            System.out.println("Compilation failed [GCC]");
        }
    }
}
