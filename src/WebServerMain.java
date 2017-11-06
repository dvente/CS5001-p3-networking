import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import sun.net.www.content.text.plain;

public class WebServerMain {
	//next line taken from https://stackoverflow.com/questions/2041778/how-to-initialize-hashset-values-by-construction
	private Set<String> implementedRequests = new HashSet<String>(Arrays.asList("GET","HEAD"));
	private Map<Integer,String> responseMessage;
	
	private String root = null;
	private ServerSocket ss = null;
	private Socket conn = null;
	private final String protocol = "HTTP/1.1";
	private final String crlf = "\r\n";
	private PrintWriter out = null;
	private BufferedReader in = null;
	
	public WebServerMain(int port, String root) throws IOException {
		super();
		this.root = root;

		responseMessage = new HashMap<Integer,String>();
		responseMessage.put(200,"OK");
		responseMessage.put(400, "Bad Request");
		responseMessage.put(505, "HTTP Version Not Supported");
		responseMessage.put(501, "Not Implemented");
		responseMessage.put(403, "Forbidden ");
		responseMessage.put(500, "Internal Server Error");
		responseMessage.put(404, "Not Found");
		
		
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
	
	private String readFile(File file) {
		String contents = "";
		
		try (Scanner s = new Scanner(file);){
			while(s.hasNextLine()) {
				contents += s.nextLine() + crlf;
			}
		} catch (Exception e) {
			//we checked the file existed so this should never happen
			e.printStackTrace();
			System.exit(1);
		}

		return contents;
	}
	
	private void handleRequest(String line) {
		String[] splitRequest = line.split(" ");
		String method = splitRequest[0];
		String path = root + File.separatorChar + splitRequest[1];
		
		File requestedFile = new File(path);
		if(!requestedFile.exists()) {
			out.println( getResponseHeader(404, ""));
			System.out.println( getResponseHeader(404, ""));
			return;
		}
		if(!requestedFile.canRead()) {
			out.println(getResponseHeader(403, ""));
			System.out.println(getResponseHeader(403, ""));
			return;
		}
		
		if(method.equals("GET")) {
			String fileContents =  readFile(requestedFile);
			out.println(getResponseHeader(200,fileContents)); 
			out.println(fileContents);
			return;
		} else if(method.equals("HEAD")) {
			String fileContents =  readFile(requestedFile);
			out.println(getResponseHeader(200,fileContents));
			return;
		}
		
	}

	private String getResponseHeader(int responsCode, String responseBody) {
		assert responseMessage.containsKey(responsCode) : "invalid response code" + Integer.toString(responsCode);
		String mimeType = "text/html";
		String header = protocol + " " + Integer.toString(responsCode) + " " + responseMessage.get(responsCode) + crlf;
		header += "Server: Simple Java Http Server" + crlf;
		header += "Content-Type: " + mimeType + crlf;
		header += "Content-Length: " + responseBody.length() + crlf;
		return header;
		
	}
	
	public boolean isRequestValid(String request) {
		String[] splitRequest = request.split(" ");
		
		if(splitRequest.length != 3) {
			out.println(getResponseHeader(400, ""));
			System.out.println(getResponseHeader(400, ""));
			return false;
		}
		
		if(!splitRequest[splitRequest.length-1].trim().equals(protocol)) {
			out.println(getResponseHeader(505, ""));
			System.out.println(getResponseHeader(505, ""));
			return false;
		}
		
		if(! implementedRequests.contains(splitRequest[0])) {
			out.println(getResponseHeader(501, ""));
			System.out.println(getResponseHeader(501, ""));
			return false;
		}
		
		//regex addapted from https://stackoverflow.com/questions/37370301/how-do-make-a-regular-expression-to-match-file-paths
		// File.separator is for platform independence
		if(! Pattern.matches("\\" + File.separator + "?[^\\" + File.separator + "].*", splitRequest[1])) {
			out.println(getResponseHeader(400, ""));
			System.out.println(getResponseHeader(400, ""));
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

			new WebServerMain(inPort, args[0]);

		} catch (Exception e) {
			//e.printStackTrace();
			System.out.println("Usage: java WebServerMain <document_root> <port>");
		}

	}

}
