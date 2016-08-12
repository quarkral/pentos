package pentos.sim;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.tools.*;
import java.awt.Desktop;
import java.util.concurrent.*;

class Simulator {

    private static final String root = "pentos";

    public static void main(String[] args) throws Exception
    {
	boolean gui = false;
	boolean gui_manual_refresh_on_cutter = false;
	String group = "g0";
	Class <Player> g_class = null;
	Class <Sequencer> s_class = null;
	String sequencer = "random";
	long cpu_time_ms = 300 * 1000;
	String tournament_path = null;
	// long[] timeout = new long [] {1000, 10000, 1000};
	long gui_refresh = 250;
	try {
	    for (int a = 0 ; a != args.length ; ++a)
		if (args[a].equals("-g") || args[a].equals("--groups")) {
		    if (a + 1 >= args.length)
			throw new IllegalArgumentException("Missing group name");
		    group = args[++a];
		}
		else if (args[a].equals("-s") || args[a].equals("--sequencer")) {
		    if (a+1 >= args.length)
			throw new IllegalArgumentException("Missing sequencer name");
		    sequencer = args[++a];
		}
		else if (args[a].equals("--gui-fps")) {
		    if (++a == args.length)
			throw new IllegalArgumentException("Missing GUI FPS");
		    double gui_fps = Double.parseDouble(args[a]);
		    gui_refresh = gui_fps > 0.0 ? (long) Math.round(1000.0 / gui_fps) : -1;
		    gui = true;
		} else if (args[a].equals("--tournament")) {
		    if (++a == args.length)
			throw new IllegalArgumentException("Missing tournament file");
		    tournament_path = args[a];
		} else if (args[a].equals("--gui")) gui = true;
		else if (args[a].equals("--gui-mrc"))
		    gui = gui_manual_refresh_on_cutter = true;
		else throw new IllegalArgumentException("Unknown argument: " + args[a]);
	    g_class = load_player(group);
	    s_class = load_sequencer(sequencer);
	} catch (Exception e) {
	    System.err.println("Exception during setup: " + e.getMessage());
	    e.printStackTrace();
	    System.err.println("Exiting the simulator ...");
	    System.exit(1);
	}
	if (tournament_path != null) {
	    System.out.close();
	    System.err.close();
	} else if (!gui)
	    System.err.println("GUI: disabled");
	else if (gui_refresh < 0)
	    System.err.println("GUI: enabled  (0 FPS)");
	else if (gui_refresh == 0)
	    System.err.println("GUI: enabled  (maximum FPS)");
	else {
	    double gui_fps = 1000.0 / gui_refresh;
	    System.err.println("GUI: enabled  (up to " + gui_fps + " FPS)");
	}
	int[] score = new int[] {0, 0};
	int timeout = -1;
	try {
	    timeout = play(group, group_2, g_class, class_2,
			   gui, gui_manual_refresh_on_cutter,
			   gui_refresh, cpu_time_ms, score, 11, 8, 5);
	} catch (Exception e) {
	    if (tournament_path != null) throw e;
	    System.err.println("Exception during play: " + e.getMessage());
	    e.printStackTrace();
	    System.err.println("Exiting the simulator ...");
	    System.exit(1);
	}
	if (tournament_path == null) {
	    System.err.println("Player " + group + " scored " + score[0]);
	    System.err.println("Player " + group_2 + " scored " + score[1]);
	    if      (timeout == 0) System.err.println("1st player timed out!");
	    else if (timeout == 1) System.err.println("2nd player timed out!");
	} else {
	    PrintStream file = new PrintStream(new FileOutputStream(tournament_path, true));
	    file.println(group + "," + score[0] + "," + (timeout == 0 ? "yes" : "no") + "," +
			 group_2 + "," + score[1] + "," + (timeout == 1 ? "yes" : "no"));
	    file.close();
	}
	System.exit(0);
}

private static int play(String group,
			String group_2,
			Class <Player> g_class,
			Class <Player> class_2,
			boolean gui,
			boolean gui_manual_refresh_on_cutter,
			long gui_refresh,
			long cpu_time_ms,
			int[] score,
			int ... cutter_sizes) throws Exception
{
    Shape[] cutters_retry = new Shape [5];
    List <Move> moves = gui ? new ArrayList <Move> () : null;
    // initialize player
    Player player = new Player;
    Timer timer = new Timer();
    timer.start();
    final Class <Player> player_class = g_class;
    try {
	player = timer.call(new Callable <Player> () {

		public Player call() throws Exception
		{
		    return player_class.newInstance();
		}
	    }, cpu_time_ms);
    } catch (TimeoutException e) { return p; }

    // initialise GUI
    HTTPServer server = null;
    if (gui) {
	server = new HTTPServer();
	System.err.println("HTTP port: " + server.port());
	// try to open web browser automatically
	if (!Desktop.isDesktopSupported())
	    System.err.println("Desktop operations not supported");
	else if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
	    System.err.println("Desktop browsing not supported");
	else {
	    URI uri = new URI("http://localhost:" + server.port());
	    Desktop.getDesktop().browse(uri);
	}
	gui(server, state(group, 0, 0, cutters_1, cuts_1,
			  group_2, 0, 0, cutters_2, cuts_2,
			  gui_refresh, -1));
    }
    // initialize score and termination
    // initialize land
    int land_side = 50;
    Land land = new Land(land_side);
    System.err.println("Construction begins ...");
    do {
	// get next build request
	Building 
	// call the cut() method of player
	Player player = players[p];
	int land_cut = land.countCut();
	long timeout_ms = 0;
	if (cpu_time_ms > 0) {
	    long timeout_ns = cpu_time_ms * 1000000 - timer[p].time();
	    if (timeout_ns <= 0) return p;
	    timeout_ms = (timeout_ns / 1000000) + 1;
	}
	Move cut = null;
	try {
	    cut = timer[p].call(new Callable <Move> () {

		    public Move call() throws Exception
		    {
			return player.cut(land, your_cutters, oppo_cutters);
		    }
		}, timeout_ms);
	} catch (TimeoutException e) { return p; }
	// check if player cut the Land
	if (land.countCut() != land_cut)
	    throw new RuntimeException("Player cut the simulator land");
	// check if shape is valid
	List <Shape> cutters = p == 0 ? cutters_1 : cutters_2;
	if (cut.shape < 0 || cut.shape >= cutters.size())
	    throw new RuntimeException("Invalid cutter shape");
	Shape shape = cutters.get(cut.shape);
	Shape[] shape_rotations = shape.rotations();
	// check if rotation is valid
	if (cut.rotation < 0 || cut.rotation >= shape_rotations.length)
	    throw new RuntimeException("Invalid cutter rotation");
	shape = shape_rotations[cut.rotation];
	// validate first cut of first player
	if (score[0] + score[1] == 0) {
	    int min_cutter_size = Integer.MAX_VALUE;
	    for (int cutter_size : cutter_sizes)
		if (min_cutter_size > cutter_size)
		    min_cutter_size = cutter_size;
	    if (shape.size() != min_cutter_size)
		throw new RuntimeException("Invalid initial cut size: "
					   + shape.size() + " (should be " + min_cutter_size + ")");
	}
	// cut a piece and update score
	if (!land.cut(shape, cut.point))
	    throw new RuntimeException("Invalid cut");
	score[p] += shape.size();
	System.err.println("Player " + (p + 1) + " cut "
			   + shape.size() + " pieces!");
	if (!gui) continue;
	List <Move> cuts = p == 0 ? cuts_1 : cuts_2;
	cuts.add(cut);
	gui(server, state(group, score[0], timer[0].time(), cutters_1, cuts_1,
			  group_2, score[1], timer[1].time(), cutters_2, cuts_2,
			  gui_refresh, p));
    } while (!no_cuts[0] || !no_cuts[1]);
    // final GUI frame
    if (gui) {
	gui_refresh = -1;
	gui(server, state(group, score[0], timer[0].time(), cutters_1, cuts_1,
			  group_2, score[1], timer[1].time(), cutters_2, cuts_2,
			  gui_refresh, -1));
	server.close();
    }
    return -1;
}

public static String state(String group, int score_1, long cpu_1, List <Shape> cutters_1, List <Move> cuts_1,
			   String group_2, int score_2, long cpu_2, List <Shape> cutters_2, List <Move> cuts_2,
			   long gui_refresh, int highlight)
{
    StringBuffer buf = new StringBuffer();
    buf.append(group + ", " + score_1 + ", " + human_no_power(cpu_1 / 1.0e9, 2)
	       + ", " + cutters_1.size() + ", " + cuts_1.size() + "\n");
    buf.append(group_2 + ", " + score_2 + ", " + human_no_power(cpu_2 / 1.0e9, 2)
	       + ", " + cutters_2.size() + ", " + cuts_2.size() + "\n");
    // send cutters
    for (Shape s : cutters_1) {
	buf.append(s.toString(new Point(0, 0), false));
	buf.append("\n");
    }
    for (Shape s : cutters_2) {
	buf.append(s.toString(new Point(0, 0), false));
	buf.append("\n");
    }
    // send cuts
    for (Move m : cuts_1) {
	Shape s = cutters_1.get(m.shape).rotations()[m.rotation];
	buf.append(s.toString(m.point, false));
	buf.append("\n");
    }
    for (Move m : cuts_2) {
	Shape s = cutters_2.get(m.shape).rotations()[m.rotation];
	buf.append(s.toString(m.point, false));
	buf.append("\n");
    }
    buf.append(gui_refresh + ", " + highlight);
    return buf.toString();
}

public static void gui(HTTPServer server, String content)
    throws UnknownServiceException
{
    String path = null;
    for (;;) {
	// get request
	for (;;)
	    try {
		path = server.request();
		break;
	    } catch (IOException e) {
		System.err.println("HTTP request error: " + e.getMessage());
	    }
	// dynamic content
	if (path.equals("data.txt")) {
	    // send dynamic content
	    try {
		server.reply(content);
		return;
	    } catch (IOException e) {
		System.err.println("HTTP dynamic reply error: " + e.getMessage());
		continue;
	    }
	}
	// static content
	if (path.equals("")) path = "webpage.html";
	else if (!path.equals("favicon.ico") &&
		 !path.equals("apple-touch-icon.png") &&
		 !path.equals("script.js")) break;
	// send file
	File file = new File(root + File.separator + "sim"
			     + File.separator + path);
	try {
	    server.reply(file);
	} catch (IOException e) {
	    System.err.println("HTTP static reply error: " + e.getMessage());
	}
    }
    if (path == null)
	throw new UnknownServiceException("Unknown HTTP request (null path)");
    else
	throw new UnknownServiceException("Unknown HTTP request: \"" + path + "\"");
}

// scan directory (and subdirectories) for files with given extension
private static Set <File> directory(String path, String extension)
{
    Set <File> files = new HashSet <File> ();
    Set <File> prev_dirs = new HashSet <File> ();
    prev_dirs.add(new File(path));
    do {
	Set <File> next_dirs = new HashSet <File> ();
	for (File dir : prev_dirs)
	    for (File file : dir.listFiles())
		if (!file.canRead()) ;
		else if (file.isDirectory())
		    next_dirs.add(file);
		else if (file.getPath().endsWith(extension))
		    files.add(file);
	prev_dirs = next_dirs;
    } while (!prev_dirs.isEmpty());
    return files;
}

// last modified
private static long last_modified(Iterable <File> files)
{
    long last_date = 0;
    for (File file : files) {
	long date = file.lastModified();
	if (last_date < date)
	    last_date = date;
    }
    return last_date;
}
    
    // compile and load
private static Class <Player> load_player(String group) throws IOException, ReflectiveOperationException {
    String sep = File.separator;
    Set <File> player_files = directory(root + sep + group, ".java");
    File class_file = new File(root + sep + group + sep + "Player.class");
    long class_modified = class_file.exists() ? class_file.lastModified() : -1;
    if (class_modified < 0 || class_modified < last_modified(player_files) ||
	class_modified < last_modified(directory(root + sep + "sim", ".java"))) {
	JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	if (compiler == null)
	    throw new IOException("Cannot find Java compiler");
	StandardJavaFileManager manager = compiler.
	    getStandardFileManager(null, null, null);
	long files = player_files.size();
	System.err.print("Compiling " + files + " .java files ... ");
	if (!compiler.getTask(null, manager, null, null, null,
			      manager.getJavaFileObjectsFromFiles(player_files)).call())
	    throw new IOException("Compilation failed");
	System.err.println("done!");
	class_file = new File(root + sep + group + sep + "Player.class");
	if (!class_file.exists())
	    throw new FileNotFoundException("Missing class file");
    }
    ClassLoader loader = Simulator.class.getClassLoader();
    if (loader == null)
	throw new IOException("Cannot find Java class loader");
    @SuppressWarnings("rawtypes")
	Class raw_class = loader.loadClass(root + "." + group + ".Player");
    @SuppressWarnings("unchecked")
	Class <Player> player_class = raw_class;
    return player_class;
}

private static Class <Sequencer> load_sequencer(String sequencer) throws IOException, ReflectiveOperationException {
    
    String sep = File.separator;
    Set <File> sequencer_files = directory(root + sep + seq + sep + sequencer, ".java");
    File class_file = new File(root + sep + group + sep + "Sequencer.class");
    long class_modified = class_file.exists() ? class_file.lastModified() : -1;
    if (class_modified < 0 || class_modified < last_modified(sequencer_files) ||
	class_modified < last_modified(directory(root + sep + "sim", ".java"))) {
	JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	if (compiler == null)
	    throw new IOException("Cannot find Java compiler");
	StandardJavaFileManager manager = compiler.
	    getStandardFileManager(null, null, null);
	long files = sequencer_files.size();
	System.err.print("Compiling " + files + " .java files ... ");
	if (!compiler.getTask(null, manager, null, null, null,
			      manager.getJavaFileObjectsFromFiles(sequencer_files)).call())
	    throw new IOException("Compilation failed");
	System.err.println("done!");
	class_file = new File(root + sep + group + sep + "Sequencer.class");
	if (!class_file.exists())
	    throw new FileNotFoundException("Missing class file");
    }
    ClassLoader loader = Simulator.class.getClassLoader();
    if (loader == null)
	throw new IOException("Cannot find Java class loader");
    @SuppressWarnings("rawtypes")
	Class raw_class = loader.loadClass(root + "." + group + ".Sequencer");
    @SuppressWarnings("unchecked")
	Class <Sequencer> sequencer_class = raw_class;
    return sequencer_class;

}

// parse a real number and cut the number of decimals
private static String human_no_power(double x, int d)
{
    if (x == 0.0) return "0";
    if (d < 0) throw new IllegalArgumentException();
    int e = 1;
    double b = 10.0;
    while (b <= x) {
	b *= 10.0;
	e++;
    }
    StringBuffer buf = new StringBuffer();
    do {
	b *= 0.1;
	int i = (int) (x / b);
	x -= b * i;
	if (e == 0) buf.append(".");
	buf.append(i);
    } while (--e != -d);
    return buf.toString();
}
}