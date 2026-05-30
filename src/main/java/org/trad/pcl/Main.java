package org.trad.pcl;

import com.diogonunes.jcolor.AnsiFormat;
import com.diogonunes.jcolor.Attribute;
import org.trad.pcl.Exceptions.BadFileExtension;
import org.trad.pcl.Helpers.FileHelper;
import org.trad.pcl.Helpers.StringFormatHelper;
import org.trad.pcl.Lexer.Lexer;
import org.trad.pcl.Parser.Parser;
import org.trad.pcl.Services.ErrorService;
import org.trad.pcl.Services.PythonRunner;
import org.trad.pcl.annotation.MethodLoggerAspect;
import org.trad.pcl.asm.ASMGenerator;
import org.trad.pcl.ast.ProgramNode;
import org.trad.pcl.semantic.SemanticAnalysisVisitor;
import org.trad.pcl.semantic.SymbolTable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;

import static com.diogonunes.jcolor.Ansi.colorize;

public final class Main {

    public static void main(String[] args) throws BadFileExtension, IOException {
        if (args.length < 1) {
            System.err.println("Usage: java Main <file> [options]");
            return;
        }

        String argPath = args[0];
        System.out.println("Parsing file: " + argPath);
        //String argPath = "/test.canAda";
        File file = new File(Objects.requireNonNull(Main.class.getResource(argPath)).getFile());
        if (!file.exists() || !file.canRead()) {
            throw new IOException("File does not exist or cannot be read");
        }

        if (!FileHelper.getFileExtension(file).equalsIgnoreCase(Constants.REQUIRED_EXTENSION)) {
            throw new BadFileExtension(Constants.REQUIRED_EXTENSION);
        }

        Lexer lexer = new Lexer(file);
        ErrorService errorService = ErrorService.getInstance();
        if (args.length > 1 && "-t".equals(args[1])) {
            lexer.displayAllTokens();
        } else {
            Parser parser;
            if (args.length > 2 && "-p".equals(args[2])) {
                MethodLoggerAspect.setLogger(true);
                parser = new Parser(lexer, true);
            } else {
                parser = new Parser(lexer);
            }
            System.out.println("🔍 " + colorize("STARTING PARSING PHASE", new AnsiFormat(Attribute.WHITE_TEXT(), Attribute.BLUE_BACK(), Attribute.BOLD())));
            ProgramNode AST = parser.parse();
            if (args.length > 1 && "-json".equals(args[1])) {
                if (errorService.hasNoErrors()) {
                    AST.setIsJson(true);
                    System.out.println("@@AST_JSON@@" + AST.toString());
                }
                errorService.handleErrorsDisplay();
                return;
            }
            if (errorService.hasNoErrors()) {
                AnsiFormat fWarning = new AnsiFormat(Attribute.WHITE_TEXT(), Attribute.GREEN_BACK(), Attribute.BOLD());
                System.out.println("\n✅ " + colorize("PARSING PHASE COMPLETED, GENERATING AST", fWarning));
                System.out.println(AST);
                if (args.length > 1 && "-g".equals(args[1])) {
                    AST.setIsJson(true);
                    String json = AST.toString();
                    PythonRunner.exec(json);
                }
                System.out.println("🔍 " + colorize("STARTING SEMANTIC ANALYSIS PHASE", new AnsiFormat(Attribute.WHITE_TEXT(), Attribute.BLUE_BACK(), Attribute.BOLD())));
                SemanticAnalysisVisitor semanticChecker = new SemanticAnalysisVisitor();
                AST.accept(semanticChecker);
                List<SymbolTable> symbolTables = semanticChecker.getSymbolTables();
                for (SymbolTable symbolTable : symbolTables) {
                    System.out.println(symbolTable);
                }
                if (errorService.hasNoErrors()) {
                    AnsiFormat fWarning2 = new AnsiFormat(Attribute.WHITE_TEXT(), Attribute.GREEN_BACK(), Attribute.BOLD());
                    System.out.println("\n✅ " + colorize("SEMANTIC ANALYSIS PHASE COMPLETED", fWarning2));

                    System.out.println("🔍 " + colorize("STARTING ASM GENERATION PHASE", new AnsiFormat(Attribute.WHITE_TEXT(), Attribute.BLUE_BACK(), Attribute.BOLD())));
                    ASMGenerator asmGenerator = new ASMGenerator(symbolTables);
                    asmGenerator.visit(AST);
                    String output = asmGenerator.getOutput();
                    System.out.println(output);

                    Files.write(Paths.get("out.s"), output.getBytes());
                    System.out.println("▶️ " + colorize("RUNNING BINARY VIA HEADLESS VISUAL", new AnsiFormat(Attribute.WHITE_TEXT(), Attribute.BLUE_BACK(), Attribute.BOLD())));
                    System.out.println("▶️ " + colorize("RUNNING BINARY VIA HEADLESS VISUAL", new AnsiFormat(Attribute.WHITE_TEXT(), Attribute.BLUE_BACK(), Attribute.BOLD())));

                    ProcessBuilder pb = new ProcessBuilder("java", "-jar", "pcl.jar", "out.s");
                    pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    pb.redirectError(ProcessBuilder.Redirect.INHERIT);

                    try {
                        Process process = pb.start();
                        int exitCode = process.waitFor();

                        if (exitCode == 0) {
                            System.out.println("Success");
                        } else {
                            System.out.println("err : " + exitCode);
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }

                } else {
                    AnsiFormat fWarning2 = new AnsiFormat(Attribute.WHITE_TEXT(), Attribute.RED_BACK(), Attribute.BOLD());
                    System.out.println("\n❌ " + colorize("SEMANTIC ANALYSIS PHASE FAILED", fWarning2));
                }
            } else {
                AnsiFormat fWarning = new AnsiFormat(Attribute.WHITE_TEXT(), Attribute.RED_BACK(), Attribute.BOLD());
                System.out.println("\n❌ " + colorize("PARSING PHASE FAILED, STOPPING", fWarning));
            }

        }

        errorService.handleErrorsDisplay();

    }

}
