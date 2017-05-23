package org.globaltester.scriptrunner;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.globaltester.logging.legacy.logger.TestLogger;
import org.globaltester.smartcardshell.jsinterface.RhinoJavaScriptAccess;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.ScriptableObject;

/**
 * This class implements the methods of an internal shell.
 * 
 * @version Release 2.2.0
 * @author Holger Funke
 * 
 */
public class ScriptRunner implements FileEvaluator {

	private static Map<Context, ScriptRunner> runners = new HashMap<>();

	// Script context of this shell
	private Context context;

	// streams of this shell
	PrintStream ostream = null; // Stream to send output to
	InputStream istream = null; // Stream to read input from
	PrintStream estream = null; // Stream to send errors to

	// Active directories
	private File currentWorkingDir; // Current working directory
	private IContainer scriptRootDir;

	private GtRuntimeRequirements runtimeRequirements;
	public GtRuntimeRequirements getConfigurationObjects() {
		return runtimeRequirements;
	}

	private List<Runnable> cleanupHooks = new LinkedList<>();

	private ScriptableObject scope;

	/**
	 * Create ScriptRunner
	 * 
	 * @param scriptPath
	 *            path where the scripts could be found
	 */
	public ScriptRunner(IContainer scriptRoot, String scriptPath, GtRuntimeRequirements runtimeReqs) {
		currentWorkingDir = new File(scriptPath);
		scriptRootDir = scriptRoot;
		this.runtimeRequirements = runtimeReqs;
	}

	/**
	 * Return class name to identify different shells
	 * 
	 * @return className
	 */
	public String getClassName() {
		return "ScriptRunner";
	}

	/**
	 * Initialisation of this shell. Create new Card object and set the
	 * environment.
	 */
	public void init(ScriptableObject scope) {
		this.scope = scope;
		try {
			// Initialize ECMAScript environment
			RhinoJavaScriptAccess rhinoAccess = new RhinoJavaScriptAccess();
			context = rhinoAccess.activateContext();
			ScriptRunner.runners.put(context, this);

			addClassLoader(this.getClass().getClassLoader());

			context.initStandardObjects(scope);
			
			String[] names = { "print", "load", "defineClass", "getAbsolutePathForProject", "eval",
			"getRunnerInstance" };

			scope.defineFunctionProperties(names, scope.getClass(), ScriptableObject.DONTENUM);

			context.initStandardObjects(scope);

		} catch (Exception e) {
			TestLogger.error(makeExceptionMessage(e));
		}
	}

	public void addClassLoader(ClassLoader loader) {
		RhinoJavaScriptAccess.getClassLoader().addClass(loader);
	}

	/**
	 * Shut down card system
	 * 
	 */
	public void close() {
		runners.remove(context);
		for (Runnable runnable : cleanupHooks) {
			runnable.run();
		}
	}

	/**
	 * Turn various exceptions into a single string
	 * 
	 * @param e
	 *            Exception object
	 * @return String with meaningful message
	 */
	public static String makeExceptionMessage(Exception e) {
		if (e instanceof JavaScriptException) {
			JavaScriptException jse = (JavaScriptException) e;
			// Some form of JavaScript error.
			return (jse.getMessage());
		} else {
			return (e.toString());
		}
	}

	public Object exec(String command) throws JavaScriptException {
		return exec(command, "ScriptRunner", 1);
	}

	/**
	 * Execute command in this shell
	 * 
	 * @param command
	 *            command that should be executed
	 * @return object result of executed command
	 */
	public Object exec(String command, String sourceName, int lineNumber) throws JavaScriptException {

		if (!context.stringIsCompilableUnit(command)) {
			System.out.println("Error: Command is not compilable unit!");
			System.out.println("Command: " + command);
			return null;
		}

		Object result = context.evaluateString(scope, command, sourceName, lineNumber, null);

		// If the evaluator returns a function object, then we call
		// this object with the last result as an argument
		if (result instanceof Function) {
			Function fo = (Function) result;
			Object lastresult = scope.get("lastresult", scope);

			if (lastresult != null) {
				Object args[] = { lastresult };
				result = fo.call(context, scope, scope, args);
			} else {
				Object args[] = {};
				result = fo.call(context, scope, scope, args);
			}
		}

		if (result != Context.getUndefinedValue()) {
			scope.put("lastresult", scope, result);
		}
		return result;
	}

	/**
	 * Load and evaluate file
	 * 
	 * @param cx
	 *            current context
	 * @param filename
	 *            file to load and execute
	 */
	public void evaluateFile(String filename) {
		evaluateFile(null, filename);
	}

	/**
	 * Load and evaluate file
	 * 
	 * @param cx
	 *            current context
	 * @param parentContainer
	 *            the parent container the given filename is relative to
	 * @param filename
	 *            file to load and execute
	 */
	public void evaluateFile(IContainer parentContainer, String filename) {

		FileReader in = null;

		File file = new File(filename);
		
		if (parentContainer != null) {
			file = new File(parentContainer.getLocation().toOSString() + File.separator + filename);
		} else {
			if (!file.isAbsolute()){
				file = new File(this.currentWorkingDir + File.separator + filename);	
			}
		}

		File oldCWD = this.currentWorkingDir;
		this.currentWorkingDir = file.getParentFile();

		try {
			in = new FileReader(file);
		}

		catch (Exception e) {
			this.currentWorkingDir = oldCWD;
			Context.reportError(e.toString());
			return;
		}

		try {
			Object result = context.evaluateReader(scope, in, file.getAbsolutePath(), 1, null);

			if (result != Context.getUndefinedValue()) {
				scope.put("lastresult", scope, result);
			}
		}

		catch (IOException e) {
			estream.println(e);
		}

		finally {
			this.currentWorkingDir = oldCWD;
			try {
				in.close();
			} catch (Exception e) {
				estream.println(e);
			}
		}
	}

	/**
	 * Returns the current working directory
	 * 
	 * @return working directory File
	 */
	public File getWorkingDirectory() {
		return currentWorkingDir;
	}

	public File getCurrentWorkingDir() {
		return currentWorkingDir;
	}

	public File getUserDir() {
		return scriptRootDir.getLocation().toFile();
	}

	public File getSystemDir() {
		return scriptRootDir.getLocation().toFile();
	}

	public void addCleanupHook(Runnable hook) {
		cleanupHooks.add(hook);
	}

	public Context getContext() {
		return this.context;
	}

	public ScriptableObject getScope() {
		return scope;
	};

	public static ScriptRunner getRunnerForContext(Context context) {
		if (!runners.containsKey(context)) {
			throw new IllegalArgumentException("No runner is registered for this context");
		}
		return runners.get(context);
	}

	/**
	 * Return the configuration object associated with the given key.
	 * <p/>
	 * @see java.util.Map#get(Object)
	 */
	public Object getConfigurationObject(Class<?> key) {
		if (runtimeRequirements == null) return null;
		return runtimeRequirements.get(key);
	}

	public IContainer getScriptRoot() {
		return scriptRootDir;
	}

}
