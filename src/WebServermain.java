import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class WebServermain {

	public WebServermain(int port, File root) throws IOException {
		super();
		this.port = port;
		this.root = root;

		ss = new ServerSocket(port);
		conn = ss.accept();

		InputStreamReader isr = new InputStreamReader(conn.getInputStream());
		BufferedReader in = new BufferedReader(isr);
		PrintWriter out = new PrintWriter(conn.getOutputStream(), true);
		String line = in.readLine();
		out.println(line);
		conn.close();

	}
	
//	public boolean isRequestValid(String request) {
//		
//		
//		return false;
//	}
//	
//	private void parseRequest(String requestLine) {
//		
//		//Request-Line   = Method SP Request-URI SP HTTP-Version CRLF
//		// taken from https://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html
//		String[] splitRequestLine = requestLine.split(" ");
//		assert splitRequestLine.length == 3;
//		String request = inputArray[0];
//	}
	
	private int port = -1;
	private File root = null;
	private ServerSocket ss = null;
	private Socket conn = null;

	public static void main(String[] args) {

		try {
			File inRoot = new File(args[0]);
			int inPort = Integer.parseInt(args[1]);
			assert inRoot.exists() && inRoot.isDirectory() && inRoot.canWrite() && inRoot.canRead();
			assert inPort > 0;

			new WebServermain(inPort, inRoot);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("java WebServerMain <document_root> <port>");
		}

	}

}
