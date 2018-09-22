package de.peeeq.wurstio.languageserver.requests;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import de.peeeq.wurstio.CompiletimeFunctionRunner;
import de.peeeq.wurstio.UtilsIO;
import de.peeeq.wurstio.WurstCompilerJassImpl;
import de.peeeq.wurstio.languageserver.ModelManager;
import de.peeeq.wurstio.languageserver.WFile;
import de.peeeq.wurstio.map.importer.ImportFile;
import de.peeeq.wurstio.mpq.MpqEditor;
import de.peeeq.wurstio.mpq.MpqEditorFactory;
import de.peeeq.wurstscript.RunArgs;
import de.peeeq.wurstscript.WLogger;
import de.peeeq.wurstscript.ast.CompilationUnit;
import de.peeeq.wurstscript.ast.WImport;
import de.peeeq.wurstscript.ast.WPackage;
import de.peeeq.wurstscript.ast.WurstModel;
import de.peeeq.wurstscript.attributes.CompileError;
import de.peeeq.wurstscript.gui.WurstGui;
import de.peeeq.wurstscript.jassAst.JassProg;
import de.peeeq.wurstscript.jassprinter.JassPrinter;
import de.peeeq.wurstscript.parser.WPos;
import de.peeeq.wurstscript.utils.LineOffsets;
import de.peeeq.wurstscript.utils.Utils;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.services.LanguageClient;

import java.io.File;
import java.io.PrintStream;
import java.nio.channels.NonWritableChannelException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static de.peeeq.wurstio.CompiletimeFunctionRunner.FunctionFlagToRun.CompiletimeFunctions;

public abstract class MapRequest extends UserRequest<Object> {
    protected final File map;
    protected final List<String> compileArgs;
    protected final WFile workspaceRoot;

    public MapRequest(File map, List<String> compileArgs, WFile workspaceRoot) {
        this.map = map;
        this.compileArgs = compileArgs;
        this.workspaceRoot = workspaceRoot;
    }

    @Override
    public void handleException(LanguageClient languageClient, Throwable err, CompletableFuture<Object> resFut) {
        if (err instanceof RequestFailedException) {
            RequestFailedException rfe = (RequestFailedException) err;
            languageClient.showMessage(new MessageParams(rfe.getMessageType(), rfe.getMessage()));
            resFut.complete(new Object());
        } else {
            super.handleException(languageClient, err, resFut);
        }
    }

    protected void processMapScript(RunArgs runArgs, WurstGui gui, ModelManager modelManager, File mapCopy) throws Exception {
        File existingScript = new File(new File(workspaceRoot.getFile(), "wurst"), "war3map.j");
        // If runargs are no extract, either use existing or throw error
        // Otherwise try loading from map, if map was saved with wurst, try existing script, otherwise error
        if (runArgs.isNoExtractMapScript()) {
            WLogger.info("flag -isNoExtractMapScript set");
            if (existingScript.exists()) {
                modelManager.syncCompilationUnit(WFile.create(existingScript));
                return;
            } else {
                throw new CompileError(new WPos(mapCopy.toString(), new LineOffsets(), 0, 0),
                        "RunArg noExtractMapScript is set but no mapscript is provided inside the wurst folder");
            }
        }
        WLogger.info("extracting mapscript");
        byte[] extractedScript;
        try (MpqEditor mpqEditor = MpqEditorFactory.getEditor(mapCopy)) {
            extractedScript = mpqEditor.extractFile("war3map.j");
        }
        if (new String(extractedScript, StandardCharsets.UTF_8).startsWith(JassPrinter.WURST_COMMENT_RAW)) {
            WLogger.info("map has already been compiled with wurst");
            // file generated by wurst, do not use
            if (existingScript.exists()) {
                WLogger.info(
                        "Cannot use war3map.j from map file, because it already was compiled with wurst. " + "Using war3map.j from Wurst directory instead.");
            } else {
                CompileError err = new CompileError(new WPos(mapCopy.toString(), new LineOffsets(), 0, 0),
                        "Cannot use war3map.j from map file, because it already was compiled with wurst. " + "Please add war3map.j to the wurst directory.");
                gui.showInfoMessage(err.getMessage());
                WLogger.severe(err);
            }
        } else {
            WLogger.info("new map, use extracted");
            // write mapfile from map to workspace
            Files.write(extractedScript, existingScript);
        }

        // push war3map.j to modelmanager

        modelManager.syncCompilationUnit(WFile.create(existingScript));
    }

    protected File compileMap(WurstGui gui, File mapCopy, File origMap, RunArgs runArgs, WurstModel model) {
        try (MpqEditor mpqEditor = MpqEditorFactory.getEditor(mapCopy)) {
            //WurstGui gui = new WurstGuiLogger();
            if (!mpqEditor.canWrite()) {
                WLogger.severe("The supplied map is invalid/corrupted/protected and Wurst cannot write to it.\n" +
                        "Please supply a valid .w3x input map that can be opened in the world editor.");
                throw new NonWritableChannelException();
            }
            WurstCompilerJassImpl compiler = new WurstCompilerJassImpl(gui, mpqEditor, runArgs);
            compiler.setMapFile(mapCopy);
            purgeUnimportedFiles(model);

            gui.sendProgress("Check program");
            compiler.checkProg(model);

            if (gui.getErrorCount() > 0) {
                throw new RequestFailedException(MessageType.Warning, "Could not compile project: " + gui.getErrorList().get(0));
            }

            print("translating program ... ");
            compiler.translateProgToIm(model);

            if (gui.getErrorCount() > 0) {
                throw new RequestFailedException(MessageType.Error, "Could not compile project (error in translation): " + gui.getErrorList().get(0));
            }


            compiler.runCompiletime();

            print("translating program to jass ... ");
            compiler.transformProgToJass();

            JassProg jassProg = compiler.getProg();
            if (jassProg == null) {
                print("Could not compile project\n");
                throw new RuntimeException("Could not compile project (error in JASS translation)");
            }

            gui.sendProgress("Printing program");
            JassPrinter printer = new JassPrinter(!runArgs.isOptimize(), jassProg);
            String compiledMapScript = printer.printProg();

            File buildDir = getBuildDir();
            File outFile = new File(buildDir, "compiled.j.txt");
            Files.write(compiledMapScript.getBytes(Charsets.UTF_8), outFile);
            return outFile;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * removes everything compilation unit which is neither
     * - inside a wurst folder
     * - a jass file
     * - imported by a file in a wurst folder
     */
    private void purgeUnimportedFiles(WurstModel model) {

        Set<CompilationUnit> imported = model.stream()
                .filter(cu -> isInWurstFolder(cu.getFile()) || cu.getFile().endsWith(".j")).distinct().collect(Collectors.toSet());
        addImports(imported, imported);

        model.removeIf(cu -> !imported.contains(cu));
    }

    private boolean isInWurstFolder(String file) {
        Path p = Paths.get(file);
        Path w = workspaceRoot.getPath();
        return p.startsWith(w)
                && java.nio.file.Files.exists(p)
                && Utils.isWurstFile(file);
    }

    protected File getBuildDir() {
        File buildDir = new File(workspaceRoot.getFile(), "_build");
        if (!buildDir.exists()) {
            UtilsIO.mkdirs(buildDir);
        }
        return buildDir;
    }

    private void addImports(Set<CompilationUnit> result, Set<CompilationUnit> toAdd) {
        Set<CompilationUnit> imported =
                toAdd.stream()
                        .flatMap((CompilationUnit cu) -> cu.getPackages().stream())
                        .flatMap((WPackage p) -> p.getImports().stream())
                        .map(WImport::attrImportedPackage)
                        .filter(Objects::nonNull)
                        .map(WPackage::attrCompilationUnit)
                        .collect(Collectors.toSet());
        boolean changed = result.addAll(imported);
        if (changed) {
            // recursive call terminates, as there are only finitely many compilation units
            addImports(result, imported);
        }
    }

    protected void print(String s) {
        WLogger.info(s);
    }

    protected void println(String s) {
        WLogger.info(s);
    }
}
