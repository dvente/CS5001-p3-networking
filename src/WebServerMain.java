import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileSystem;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.sun.xml.internal.ws.util.StringUtils;

public class WebServerMain {
	//next line taken from https://stackoverflow.com/questions/2041778/how-to-initialize-hashset-values-by-construction
	private Set<String> implementedRequests = new HashSet<String>(Arrays.asList("GET","HEAD"));
	
	
	private int port = -1;
	private File root = null;
	private ServerSocket ss = null;
	private Socket conn = null;
	private final String version = "HTTP/1.1";
	private PrintWriter out = null;
	private BufferedReader in = null;
	
	public WebServerMain(int port, File root) throws IOException {
		super();
		this.port = port;
		this.root = root;

		ss = new ServerSocket(port);
		conn = ss.accept();

		InputStreamReader isr = new InputStreamReader(conn.getInputStream());
		in = new BufferedReader(isr);
		out = new PrintWriter(conn.getOutputStream(), true);
		String line = in.readLine();
		if(isRequestValid(line)) {
			handleRequest(line);
		}
		conn.close();

	}
	
	private void handleRequest(String line) {
		//out.println(line);
		out.println(version + " 200 OK");
		System.out.println(line);
		
	}

	public boolean isRequestValid(String request) {
		String[] splitRequest = request.split(" ");
		
		
		if(splitRequest.length != 3) {
			out.println(version + " 400 Bad Request");
			return false;
		}
		
		if(!splitRequest[splitRequest.length-1].trim().equals(version)) {
			out.println(version + " 505 HTTP Version Not Supported");
			return false;
		}
		
		if(! implementedRequests.contains(splitRequest[0])) {
			out.println(version + "505 HTTP Version Not Supported");
			return false;
		}
		
		//regex addapted from https://stackoverflow.com/questions/37370301/how-do-make-a-regular-expression-to-match-file-paths
		// File.separator is for platform independence
		if(! Pattern.matches("\\" + File.separator + "?[^\\" + File.separator + "].*", splitRequest[1])) {
			out.println(version + "400 Bad Request");
			return false;
		}
		
		
		return true;
	}


	public static void main(String[] args) {

		try {
			File inRoot = new File(args[0]);
			int inPort = Integer.parseInt(args[1]);
			assert inRoot.exists() : "inRoot.exists(): " +  inRoot.exists();
			assert inRoot.isDirectory(): "inRoot.isDirectory(): " + inRoot.isDirectory();
			assert inRoot.canWrite() : "inRoot.canWrite(): " + inRoot.canWrite();
			 assert inRoot.canRead() : "inRoot.canRead(): " + inRoot.canRead();
			assert inPort > 0;

			new WebServerMain(inPort, inRoot);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("java WebServerMain <document_root> <port>");
		}

	}

}
