package sorcer.netlet;

import sorcer.netlet.util.LoaderConfigurationHelper;
import sorcer.netlet.util.ScriptExertException;
import sorcer.netlet.util.ScriptThread;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Sorcer Script Exerter - this class handles parsing an ntl (Netlet) script and executing it or returning its
 * content as an object
 *
 * User: prubach
 * Date: 02.07.13
 */
public class ScriptExerter {

    private final static Logger logger = Logger.getLogger(ScriptExerter.class
            .getName());

    private final static String LINE_SEP = "\n";

    private String input;

    private PrintStream out;

    private File outputFile;

    private File scriptFile;

    private String script;

    private List<String> loadLines = new ArrayList<String>();

    private List<String> codebaseLines = new ArrayList<String>();

    private ClassLoader classLoader;

    private static StringBuilder staticImports;

    private Object target;

    private Object result;

    private ScriptThread scriptThread;

    private String websterStrUrl;

    public ScriptExerter() {
        this(null, null, null);
    }

    public ScriptExerter(PrintStream out, ClassLoader classLoader, String websterStrUrl) {
        this.out = out;
        this.classLoader = classLoader;
        this.websterStrUrl = websterStrUrl;
        if (staticImports == null) {
            staticImports = readTextFromJar("static-imports.txt");
        }
    }

    public ScriptExerter(File scriptFile) throws IOException {
        this(scriptFile, null, null, null);
    }

    public ScriptExerter(File scriptFile, PrintStream out, ClassLoader classLoader, String websterStrUrl) throws IOException {
        this(out, classLoader, websterStrUrl);
        this.scriptFile = scriptFile;
        readFile(scriptFile);
    }

    public ScriptExerter(String script, PrintStream out, ClassLoader classLoader, String websterStrUrl) throws IOException {
        this(out, classLoader, websterStrUrl);
        readScriptWithHeaders(script);
    }


    public Object execute() throws Throwable {
        if (scriptThread!=null) {
            scriptThread.start();
            scriptThread.join();
            result = scriptThread.getResult();
            return result;
        }
        throw new ScriptExertException("You must first call parse() before calling execut() ");
    }

    public Object parse() throws Throwable {
        // Process "load" and generate a list of URLs for the classloader
        List<URL> urlsToLoad = new ArrayList<URL>();
        if (!loadLines.isEmpty()) {
            for (String jar : loadLines) {
                String loadPath = jar.substring(LoaderConfigurationHelper.LOAD_PREFIX.length()).trim();
                urlsToLoad = LoaderConfigurationHelper.load(loadPath);
            }
        }
        // Process "codebase" and set codebase variable
        urlsToLoad.addAll(LoaderConfigurationHelper.setCodebase(codebaseLines, websterStrUrl, out));

        scriptThread = new ScriptThread(script, urlsToLoad.toArray(new URL[] { }),  classLoader, out);
        this.target = scriptThread.getTarget();
        return target;
    }


    public void readFile(File file) throws IOException {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        List<String> loadLines = new ArrayList<String>();
        List<String> codebaseLines = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String nextLine = "";
        StringBuffer sb = new StringBuffer();
        sb.append(staticImports.toString());
        nextLine = br.readLine();
        if (nextLine.indexOf("#!") < 0) {
            sb.append(nextLine).append(LINE_SEP);
        }
        while ((nextLine = br.readLine()) != null) {
            // Check for "load" of jars
            if (nextLine.trim().startsWith(LoaderConfigurationHelper.LOAD_PREFIX)) {
                this.loadLines.add(nextLine.trim());
            } else if (nextLine.trim().startsWith(LoaderConfigurationHelper.CODEBASE_PREFIX)) {
                this.codebaseLines.add(nextLine.trim());
            } else {
                sb.append(nextLine.trim()).append(LINE_SEP);
            }
        }
        this.script = sb.toString();
    }

    public void readScriptWithHeaders(String script) throws IOException {
        String[] lines = script.split(LINE_SEP);
        StringBuilder sb = new StringBuilder(staticImports.toString());
        for (String line : lines) {
            // Check for "load" of jars
            if (line.trim().startsWith(LoaderConfigurationHelper.LOAD_PREFIX)) {
                this.loadLines.add(line.trim());
            } else if (line.trim().startsWith(LoaderConfigurationHelper.CODEBASE_PREFIX)) {
                this.codebaseLines.add(line.trim());
            } else {
                sb.append(line);
                sb.append(LINE_SEP);
            }
        }
        this.script = sb.toString();
    }

    private StringBuilder readTextFromJar(String filename) {
        InputStream is = null;
        BufferedReader br = null;
        String line;
        StringBuilder sb = new StringBuilder();

        try {
            is = getClass().getClassLoader().getResourceAsStream(filename);
            logger.finest("Loading " + filename + " from is: " + is);
            if (is != null) {
                br = new BufferedReader(new InputStreamReader(is));
                while (null != (line = br.readLine())) {
                    sb.append(line);
                    sb.append("\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
                if (is != null)
                    is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb;
    }

    public List<String> getLoadLines() {
        return loadLines;
    }

    public void setLoadLines(List<String> loadLines) {
        this.loadLines = loadLines;
    }

    public List<String> getCodebaseLines() {
        return codebaseLines;
    }

    public void setCodebaseLines(List<String> codebaseLines) {
        this.codebaseLines = codebaseLines;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public Object getTarget() {
        return target;
    }

    public Object getResult() {
        return result;
    }
}

