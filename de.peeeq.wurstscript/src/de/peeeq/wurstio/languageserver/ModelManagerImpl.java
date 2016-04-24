package de.peeeq.wurstio.languageserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import de.peeeq.wurstio.ModelChangedException;
import de.peeeq.wurstio.WurstCompilerJassImpl;
import de.peeeq.wurstscript.RunArgs;
import de.peeeq.wurstscript.WLogger;
import de.peeeq.wurstscript.ast.Ast;
import de.peeeq.wurstscript.ast.CompilationUnit;
import de.peeeq.wurstscript.ast.WImport;
import de.peeeq.wurstscript.ast.WPackage;
import de.peeeq.wurstscript.ast.WurstModel;
import de.peeeq.wurstscript.attributes.CompileError;
import de.peeeq.wurstscript.gui.WurstGui;
import de.peeeq.wurstscript.gui.WurstGuiLogger;
import de.peeeq.wurstscript.parser.WPos;
import de.peeeq.wurstscript.utils.LineOffsets;

/**
 * keeps a version of the model which is always the most recent one
 */
public class ModelManagerImpl implements ModelManager {

	private volatile @Nullable WurstModel model;
	private File projectPath;
	private boolean needsFullBuild = true;
	// dependency folders (folders mentioned in wurst.dependencies)
	private final Set<String> dependencies = Sets.newLinkedHashSet();
	// private WurstGui gui = new WurstGuiLogger();
	private List<Consumer<CompilationResult>> onCompilationResultListeners = new ArrayList<>();
	// compile errors for each file
	private Map<String, List<CompileError>> parseErrors = new LinkedHashMap<>();

	public ModelManagerImpl(String projectPath) {
		this(new File(projectPath));
	}

	public ModelManagerImpl(File projectPath) {
		this.projectPath = projectPath;
	}

	private WurstModel newModel(CompilationUnit cu, WurstGui gui) {
		try {
			CompilationUnit commonJ = compileFromJar(gui, "common.j");
			CompilationUnit blizzardJ = compileFromJar(gui, "blizzard.j");
			return Ast.WurstModel(blizzardJ, commonJ, cu);
		} catch (IOException e) {
			WLogger.severe(e);
			return Ast.WurstModel(cu);
		}
	}

	@Override
	public boolean removeCompilationUnit(String resource) {
		parseErrors.remove(resource);
		WurstModel model2 = model;
		if (model2 == null) {
			return false;
		}
		ListIterator<CompilationUnit> it = model2.listIterator();
		while (it.hasNext()) {
			CompilationUnit cu = it.next();
			if (cu.getFile().equals(resource)) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	@Override
	public void clean() {
		model = null;
		dependencies.clear();
		needsFullBuild = true;
		WLogger.info("Clean done.");
	}

	/**
	 * does a full build, reading whole directory
	 *
	 * @throws IOException
	 */
	public void buildProject() {
		try {
			WurstGui gui = new WurstGuiLogger();
			readDependencies(gui);

			File wurstFolder = new File(projectPath, "wurst");
			processWurstFiles(wurstFolder);

			resolveImports(gui);

			doTypeCheck(gui, true);
			// TODO report errors

			needsFullBuild = false;
			// Map<String, List<CompileError>> groupedByFile =
			// gui.getErrorsAndWarnings().stream().collect(Collectors.groupingBy(err
			// -> err.getSource().getFile()));
			// for (Entry<String, List<CompileError>> err :
			// groupedByFile.entrySet()) {
			// CompilationResult r = CompilationResult.create(err.getKey(),
			// err.getValue());
			// onCompilationResultListeners.forEach(c -> c.accept(r));
			// }
		} catch (IOException e) {
			WLogger.severe(e);
			throw new ModelManagerException(e);
		}
	}



	private void processWurstFiles(File dir) throws IOException {
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				processWurstFiles(f);
			} else if (f.getName().endsWith(".wurst") || f.getName().endsWith(".jurst")) {
				processWurstFile(f);
			}
		}
	}

	private void processWurstFile(File f) throws IOException {
		System.out.println("processing file " + f);
		replaceCompilationUnit(f);
	}

	private void readDependencies(WurstGui gui) throws IOException {
		dependencies.clear();
		File depFile = new File(projectPath, "wurst.dependencies");
		if (!depFile.exists()) {
			WLogger.info("no dependency file found.");
			return;
		}
		try (BufferedReader reader = new BufferedReader(new FileReader(depFile))) {
			LineOffsets lineOffsets = new LineOffsets();
			int offset = 0;
			int lineNr = 0;
			while (true) {
				String line = reader.readLine();
				lineNr++;
				lineOffsets.set(lineNr, offset);
				if (line == null)
					break;
				int endOffset = offset + line.length();
				addDependency(gui, depFile, line, lineOffsets, offset, endOffset);
				offset = endOffset;
			}
		}

	}

	private String getProjectRelativePath(File f) {
		Path pathRelative = projectPath.getAbsoluteFile().toPath().relativize(f.getAbsoluteFile().toPath());
		return pathRelative.toString();
	}

	private void addDependency(WurstGui gui, File depfile, String fileName, LineOffsets lineOffsets, int offset,
							   int endOffset) {
		WLogger.info("Adding dependency: " + fileName);
		File f = new File(fileName);
		WPos pos = new WPos(getProjectRelativePath(depfile), lineOffsets, offset, endOffset);
		if (!f.exists()) {
			gui.sendError(new CompileError(pos, "Path '" + fileName + "' could not be found."));
			return;
		} else if (!f.isDirectory()) {
			gui.sendError(new CompileError(pos, "Path '" + fileName + "' is not a folder."));
			return;
		}

		dependencies.add(fileName);
	}

	private List<CompilationUnit> getCompilationUnits(List<String> fileNames) {
		WurstModel model2 = model;
		if (model2 == null) {
			return Collections.emptyList();
		}
		return model2.stream().filter(cu -> fileNames.contains(cu.getFile())).collect(Collectors.toList());
	}

	private List<String> getfileNames(List<CompilationUnit> compilationUnits) {
		return compilationUnits.stream().map(CompilationUnit::getFile).collect(Collectors.toList());
	}

	/**
	 * clear the attributes for all compilation units that import something from
	 * 'toCheck' and for 'toCheck'
	 *
	 * @return a list of all the CUs which have been cleared
	 */
	private List<CompilationUnit> clearAttributes(List<CompilationUnit> toCheck) {
		WurstModel model2 = model;
		if (model2 == null) {
			return Collections.emptyList();
		}
		List<CompilationUnit> cleared = new ArrayList<>(toCheck);
		model2.clearAttributesLocal();
		Set<String> packageNames = Sets.newHashSet();
		for (CompilationUnit cu : toCheck) {
			cu.clearAttributes();
			for (WPackage p : cu.getPackages()) {
				packageNames.add(p.getName());
			}
		}
		for (CompilationUnit cu : model2) {
			if (imports(cu, packageNames, false)) {
				cu.clearAttributes();
				cleared.add(cu);
			}
		}
		return cleared;
	}

	/**
	 * check whether cu imports something from 'toCheck'
	 */
	private boolean imports(CompilationUnit cu, Set<String> packageNames, boolean importPublic) {
		for (WPackage p : cu.getPackages()) {
			if (imports(p, packageNames, false, Sets.<WPackage>newHashSet())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * check whether p imports something from 'toCheck'
	 */
	private boolean imports(WPackage p, Set<String> packageNames, boolean importPublic, HashSet<WPackage> visited) {
		if (visited.contains(p)) {
			return false;
		}
		visited.add(p);
		for (WImport imp : p.getImports()) {
			if ((!importPublic || imp.getIsPublic()) && packageNames.contains(imp.getPackagename())) {
				return true;
			} else {
				WPackage importedPackage = imp.attrImportedPackage();
				if (imp.getIsPublic() && importedPackage != null) {
					if (imports(importedPackage, packageNames, true, visited)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void doTypeCheck(WurstGui gui, boolean addErrorMarkers) {
		WurstCompilerJassImpl comp = getCompiler(gui);
		long time = System.currentTimeMillis();
		if (gui.getErrorCount() > 0) {
			if (addErrorMarkers) {
				reportErrorsForProject("build project, doTypecheck, early", gui);
			}
			WLogger.info("finished typechecking* in " + (System.currentTimeMillis() - time) + "ms");
			return;
		}
		@Nullable
		WurstModel model2 = model;
		if (model2 == null) {
			return;
		}

		try {
			model2.clearAttributes();
			comp.addImportedLibs(model2);
			comp.checkProg(model2);
		} catch (CompileError e) {
			gui.sendError(e);
		}
		WLogger.info("finished typechecking in " + (System.currentTimeMillis() - time) + "ms");
		if (addErrorMarkers) {
			reportErrorsForProject("build project, doTypecheck, end", gui);
		}
	}

	private void reportErrorsForProject(String extra, WurstGui gui) {
		Multimap<String, CompileError> typeErrors = ArrayListMultimap.create();
		for (CompileError e : gui.getErrorsAndWarnings()) {
			typeErrors.put(e.getSource().getFile(), e);
		}

		for (String file : parseErrors.keySet()) {
			List<CompileError> errors = new ArrayList<>(parseErrors.get(file));
			errors.addAll(typeErrors.get(file));
			reportErrors(extra, file, errors);
		}
	}

	private void reportErrorsForFiles(String extra, List<String> filenames, WurstGui gui) {
		Multimap<String, CompileError> typeErrors = ArrayListMultimap.create();
		for (CompileError e : gui.getErrorsAndWarnings()) {
			typeErrors.put(e.getSource().getFile(), e);
		}

		for (String file : filenames) {
			List<CompileError> errors = new ArrayList<>(parseErrors.get(file));
			errors.addAll(typeErrors.get(file));
			reportErrors(extra, file, errors);
		}
	}

	private void reportErrors(String extra, String filename, List<CompileError> errors) {
		CompilationResult cr = CompilationResult.create(extra, filename, errors);
		for (Consumer<CompilationResult> consumer : onCompilationResultListeners) {
			consumer.accept(cr);
		}
	}

	private WurstCompilerJassImpl getCompiler(WurstGui gui) {
		RunArgs runArgs = RunArgs.defaults();
		runArgs.addLibs(dependencies);
		WurstCompilerJassImpl comp = new WurstCompilerJassImpl(gui, null, runArgs);
		comp.setHasCommonJ(true);
		return comp;
	}

	private void updateModel(CompilationUnit cu, WurstGui gui) {
		System.out.println("update model with " + cu.getFile());
		parseErrors.put(cu.getFile(), new ArrayList<>(gui.getErrorsAndWarnings()));


		WurstModel model2 = model;
		if (model2 == null) {
			model = model2 = newModel(cu, gui);
		} else {
			ListIterator<CompilationUnit> it = model2.listIterator();
			boolean updated = false;
			while (it.hasNext()) {
				CompilationUnit c = it.next();
				if (c.getFile().equals(cu.getFile())) {
					// replace old compilationunit with new one:
					it.set(cu);
					updated = true;
					break;
				}
			}
			if (!updated) {
				model2.add(cu);
			}
		}
		//doTypeCheckPartial(gui, false, ImmutableList.of(cu.getFile()));
	}

	private CompilationUnit compileFromJar(WurstGui gui, String filename) throws IOException {
		InputStream source = this.getClass().getResourceAsStream("/" + filename);
		if (source == null) {
			System.err.println("could not find " + filename + " in jar");
			source = new FileInputStream("./resources/" + filename);
		}
		WurstCompilerJassImpl comp = new WurstCompilerJassImpl(gui, null, RunArgs.defaults());

		try (InputStreamReader reader = new InputStreamReader(source)) {
			CompilationUnit cu = comp.parse(filename, reader);
			cu.setFile(filename);
			return cu;
		}
	}

	private void resolveImports(WurstGui gui) {
		WurstCompilerJassImpl comp = getCompiler(gui);
		try {
			WurstModel m = model;
			if (m == null) {
				return;
			}
			comp.addImportedLibs(m);
		} catch (CompileError e) {
			gui.sendError(e);
		}
	}

	@Override
	public void replaceCompilationUnit(String filename) {
		try {
			File f = getFile(filename);
			replaceCompilationUnit(f);
		} catch (IOException e) {
			WLogger.severe(e);
			throw new ModelManagerException(e);
		}
	}

	/**
	 * get a file object, where relative files are resolved with respect to
	 * the project root
	 */
	private File getFile(String filename) {
		File f = new File(filename);
		if (f.isAbsolute()) {
			return f;
		}
		return new File(projectPath, filename);
	}

	private void replaceCompilationUnit(File f) throws IOException {
		WLogger.info("replaceCompilationUnit 1 " + f);
		String filename = getProjectRelativePath(f);
		WLogger.info("replaceCompilationUnit 2 " + f);
		replaceCompilationUnit(filename, new FileReader(f), true);
		WLogger.info("replaceCompilationUnit 3 " + f);
	}

	@Override
	public void syncCompilationUnit(String filename) {
		WLogger.info("syncCompilationUnit " + filename);
		try {
			File f = getFile(filename);
			syncCompilationUnit(f);
		} catch (IOException e) {
			WLogger.severe(e);
			throw new ModelManagerException(e);
		}
	}

	@Override
	public void syncCompilationUnitContent(String filename, String contents) {
		WLogger.info("sync contents for " + filename + " with content: " + contents);
		filename = normalizeFilename(filename);
		Reader reader = new StringReader(contents);
		try {
			replaceCompilationUnit(filename, reader, true);
			WurstGui gui = new WurstGuiLogger();
			doTypeCheckPartial(gui, true, ImmutableList.of(filename));
		} catch (IOException e) {
			WLogger.severe(e);
			throw new ModelManagerException(e);
		}
	}

	@Override
	public CompilationUnit replaceCompilationUnitContent(String filename, String contents) {
		filename = normalizeFilename(filename);
		Reader reader = new StringReader(contents);
		try {
			return replaceCompilationUnit(filename, reader, true);
		} catch (IOException e) {
			WLogger.severe(e);
			throw new ModelManagerException(e);
		}
	}

	private String normalizeFilename(String filename) {
		File f = getFile(filename);
		filename = getProjectRelativePath(f);
		return filename;
	}

	private void syncCompilationUnit(File f) throws IOException {
		WLogger.info("syncCompilationUnit File " + f);
		replaceCompilationUnit(f);
		WLogger.info("replaced file " + f);
		WurstGui gui = new WurstGuiLogger();
		doTypeCheckPartial(gui, true, ImmutableList.of(getProjectRelativePath(f)));
	}

	private CompilationUnit replaceCompilationUnit(String filename, Reader fReader, boolean reportErrors) throws IOException {
		WLogger.info("replace CU " + filename);
		WurstGui gui = new WurstGuiLogger();
		WurstCompilerJassImpl c = getCompiler(gui);
		CompilationUnit cu;
		try (Reader reader = fReader) {
			cu = c.parse(filename, reader);
			cu.setFile(filename);
		}
		updateModel(cu, gui);
		if (reportErrors) {
			System.out.println("found " + gui.getErrorCount() + " errors in file " + filename);
			reportErrors("sync cu " + filename, filename, gui.getErrorsAndWarnings());
		}
		return cu;
	}

	@Override
	public void updateCompilationUnit(String filename, String contents, boolean reportErrors) {
		try {
			replaceCompilationUnit(filename, new StringReader(contents), reportErrors);
		} catch (IOException e) {
			WLogger.severe(e);
			throw new ModelManagerException(e);
		}
	}

	@Override
	public void onCompilationResult(Consumer<CompilationResult> f) {
		onCompilationResultListeners.add(f);
	}

	public static void main(String[] args) throws IOException {
		// WurstGui gui = new WurstGuiCliImpl();
		// WurstCompilerJassImpl c = new WurstCompilerJassImpl(gui, null, new
		// RunArgs(Arrays.asList()));
		// CompilationUnit cu = c.parse("RegionData.wurst", new
		// FileReader("/home/peter/work/EBR/wurst/region/RegionData.wurst"));
		//
		// for (CompileError err : gui.getErrorsAndWarnings()) {
		// System.out.println(err);
		// System.out.println(ExternCompileError.convert(err));
		// }

		ModelManagerImpl m = new ModelManagerImpl("/home/peter/work/EBR");
		m.onCompilationResult(cr -> {
			for (ExternCompileError err : cr.getErrors()) {
				System.err.println(cr.getFilename());
				System.err.println("	" + err);
			}
		});
		m.buildProject();

	}

	private void doTypeCheckPartial(WurstGui gui, boolean addErrorMarkers, List<String> toCheckFilenames) {
		WLogger.info("do typecheck partial of " + toCheckFilenames);
		WurstCompilerJassImpl comp = getCompiler(gui);
		List<CompilationUnit> toCheck = getCompilationUnits(toCheckFilenames);
		WurstModel model2 = model;
		if (model2 == null) {
			return;
		}

		List<CompilationUnit> clearedCUs = Collections.emptyList();
		try {
			clearedCUs = clearAttributes(toCheck);
			comp.addImportedLibs(model2);
			comp.checkProg(model2, toCheck);
		} catch (ModelChangedException e) {
			// model changed, early return
			return;
		} catch (CompileError e) {
			gui.sendError(e);
		}
		if (addErrorMarkers) {
			List<String> fileNames = getfileNames(clearedCUs);
			reportErrorsForFiles("partial ", fileNames, gui);
		}
	}

}
