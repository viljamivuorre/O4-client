package oy.tol.chatclient;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;

import com.diogonunes.jcolor.Ansi;
import com.diogonunes.jcolor.Attribute;

import oy.tol.chat.ChatMessage;
import oy.tol.chat.Message;

/**
 * ChatClient is the console based UI for the ChatServer. It profides the
 * necessary functionality for chatting. The actual comms with the ChatServer
 * happens in the ChatHttpClient class.
 */
public class ChatClient implements ChatClientDataProvider {

	private static final String SERVER = "localhost:10000";
	private static final String CMD_REGISTER = "/register";
	private static final String CMD_LOGIN = "/login";
	private static final String CMD_NICK = "/nick";
	private static final String CMD_COLOR = "/color";
	private static final String CMD_HELP = "/help";
	private static final String CMD_INFO = "/info";
	private static final String CMD_EXIT = "/exit";

	private String currentServer = SERVER; // URL of the server without paths.
	private String clientCertificateFile = null; // The client cert for the server.
	private String payloadFormat = null;
	private String username = null; // Registered & logged user.
	private String password = null; // The password in clear text.
	private String email = null; // Email address of user, needed for registering.
	private String nick = null; // Nickname, user can change the name visible in chats.

	private ChatTCPClient tcpClient = null; // Client handling the requests & responses.

	private boolean autoFetch = false;
	private boolean useColorOutput = false;

	static final Attribute colorDate = Attribute.GREEN_TEXT();
	static final Attribute colorNick = Attribute.BRIGHT_BLUE_TEXT();
	static final Attribute colorMsg = Attribute.CYAN_TEXT();
	static final Attribute colorError = Attribute.BRIGHT_RED_TEXT();
	static final Attribute colorInfo = Attribute.YELLOW_TEXT();

	public static void main(String[] args) {
		if (args.length == 1) {								
			try {
				System.out.println("Launching ChatClient with config file " + args[0]);
				ChatClient client = new ChatClient();
				client.run(args[0]);
			} catch (Exception e) {
				System.out.println("Failed to run the ChatClient");
				System.out.println("Reason: " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		} else {
			System.out.println("Usage: java -jar chat-client-jar-file chatclient.properties");
			System.out.println("Where chatclient.properties is the client configuration file");
		}
	}

	/**
	 * Runs the show: - Creates the http client - displays the menu - handles
	 * commands until user enters command /exit.
	 */
	public void run(String configFile) throws IOException {
		println("Reading configuration...", colorInfo);
		readConfiguration(configFile);

		tcpClient = new ChatTCPClient(this, clientCertificateFile);
		new Thread(tcpClient).start();
		printCommands();
		printInfo();
		Console console = System.console();
		if (null == username || null == password) {
			println("!! Register or login to server first.", colorInfo);
		}
		boolean running = true;
		while (running) {
			try {
				print("O4-chat > ", colorInfo);
				String command = console.readLine().trim();
				switch (command) {
					case CMD_REGISTER:
						registerUser(console);
						break;
					case CMD_LOGIN:
						getUserCredentials(console, false);
						break;
					case CMD_NICK:
						getNick(console);
						break;
					case CMD_COLOR:
						useColorOutput = !useColorOutput;
						println("Using color in output: " + (useColorOutput ? "yes" : "no"), colorInfo);
						break;
					case CMD_HELP:
						printCommands();
						break;
					case CMD_INFO:
						printInfo();
						break;
					case CMD_EXIT:
						tcpClient.close();
						running = false;
						break;
					default:
						if (command.length() > 0 && !command.startsWith("/")) {
							postMessage(command);
						}
						break;
				}
			} catch (Exception e) {
				println(" *** ERROR : " + e.getMessage(),colorError);
				e.printStackTrace();
			}
		}
		println("Bye!", colorInfo);
	}

	/**
	 * Get user credentials from console (i.e. login or register). Registering a new
	 * user actually communicates with the server. When logging in, user enters the
	 * credentials (username & password), but no comms with server happens until
	 * user actually either retrieves new chat messages from the server or posts a
	 * new chat message.
	 * 
	 * @param console        The console for the UI
	 * @param forRegistering If true, asks all registration data, otherwise just
	 *                       login data.
	 */
	private void getUserCredentials(Console console, boolean forRegistering) {
		print("Enter username > ", colorInfo);
		String newUsername = console.readLine().trim();
		if (newUsername.length() > 0) {
			// Need to cancel autofetch since username/pw not usable anymore
			// until login has been fully done (including password).
			username = newUsername;
			nick = username;
			email = null;
			password = null;
		} else {
			print("Continuing with existing credentials", colorInfo);
			printInfo();
			return;
		}
		print("Enter password > ", colorInfo);
		char[] newPassword = console.readPassword();
		if (null != newPassword && newPassword.length > 0) {
			password = new String(newPassword);
		} else {
			print("Canceled, /register or /login!", colorError);
			username = null;
			password = null;
			email = null;
			nick = null;
			return;
		}
		if (forRegistering) {
			print("Enter email > ", colorInfo);
			String newEmail = console.readLine().trim();
			if (null != newEmail && newEmail.length() > 0) {
				email = newEmail;
			} else {
				print("Canceled, /register or /login!", colorError);
				username = null;
				password = null;
				email = null;
				nick = null;
			}
		}
	}

	/**
	 * User wants to change the nick, so ask it.
	 * 
	 * @param console
	 */
	private void getNick(Console console) {
		print("Enter nick > ", colorInfo);
		String newNick = console.readLine().trim();
		if (newNick.length() > 0) {
			nick = newNick;
		}
	}

	/**
	 * Handles the registration of the user with the server. All credentials
	 * (username, email and password) must be given. User is then registered with
	 * the server.
	 * 
	 * @param console
	 */
	private void registerUser(Console console) {
		println("Give user credentials for new user for server " + currentServer, colorInfo);
		getUserCredentials(console, true);
		try {
			if (username == null || password == null || email == null) {
				println("Must specify all user information for registration!", colorError);
				return;
			}
			// Execute the HTTPS request to the server.
			int response = tcpClient.registerUser();
			if (response >= 200 || response < 300) {
				println("Registered successfully, you may start chatting!", colorInfo);
			} else {
				println("Failed to register!", colorError);
			}
		} catch (KeyManagementException | KeyStoreException | CertificateException | NoSuchAlgorithmException
				| FileNotFoundException e) {
			println(" **** ERROR in server certificate", colorError);
			println(e.getLocalizedMessage(),colorError);
		} catch (Exception e) {
			println(" **** ERROR in user registration with server " + currentServer, colorError);
			println(e.getLocalizedMessage(),colorError);
		}
	}

	/**
	 * Sends a new chat message to the server. User must be logged in to the server.
	 * 
	 * @param message The chat message to send.
	 */
	private void postMessage(String message) {
		if (null != username) {
			try {
				tcpClient.postChatMessage(message);
			} catch (KeyManagementException | KeyStoreException | CertificateException | NoSuchAlgorithmException
					| FileNotFoundException e) {
				println(" **** ERROR in server certificate",colorError);
				println(e.getLocalizedMessage(), colorError);
			} catch (IOException e) {
				println(" **** ERROR in posting message to server " + currentServer, colorError);
				println(e.getLocalizedMessage(), colorError);
			}
		} else {
			println("Must register/login to server before posting messages!", colorInfo);
		}
	}

	/**
	 * Print out the available commands.
	 */
	private void printCommands() {
		println("--- O4 Chat Client Commands ---", colorInfo);
		println("/register  -- Register as a new user in server", colorInfo);
		println("/login     -- Login using already registered credentials", colorInfo);
		println("/nick      -- Specify a nickname to use in chat server", colorInfo);
		println("/get       -- Get new messages from server", colorInfo);
		println("/color     -- Toggles color output on/off", colorInfo);
		println("/help      -- Prints out this information", colorInfo);
		println("/info      -- Prints out settings and user information", colorInfo);
		println("/exit      -- Exit the client app", colorInfo);
		println(" > To chat, write a message and press enter to send a message.", colorInfo);
	}

	/**
	 * Prints out the configuration of the client.
	 */
	private void printInfo() {
		println("Server: " + currentServer, colorInfo);
		println("Content type used: " + payloadFormat, colorInfo);
		println("User: " + username, colorInfo);
		println("Nick: " + nick, colorInfo);
		println("Autofetch is " + (autoFetch ? "on" : "off"), colorInfo);
		println("Using color in output: " + (useColorOutput ? "yes" : "no"), colorInfo);
	}

	private void print(String item, Attribute withAttribute) {
		if (useColorOutput) {
			System.out.print(Ansi.colorize(item, withAttribute));
		} else {
			System.out.print(item);
		}
	}

	private void println(String item, Attribute withAttribute) {
		if (useColorOutput) {
			System.out.println(Ansi.colorize(item, withAttribute));
		} else {
			System.out.println(item);
		}
	}

	private void readConfiguration(String configFileName) throws FileNotFoundException, IOException {
		System.out.println("Using configuration: " + configFileName);
		File configFile = new File(configFileName);
		Properties config = new Properties();
		FileInputStream istream;
		istream = new FileInputStream(configFile);
		config.load(istream);
		currentServer = config.getProperty("server", "localhost:10000");
		clientCertificateFile = config.getProperty("certfile", "");
		if (config.getProperty("usecolor", "false").equalsIgnoreCase("true")) {
			useColorOutput = true;
		}
		istream.close();
	}
	
	/*
	 * Implementation of the ChatClientDataProvider interface. The ChatHttpClient
	 * calls these methods to get configuration info needed in communication with
	 * the server.
	 */

	@Override
	public String getServer() {
		return currentServer;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getNick() {
		return nick;
	}

	@Override
	public String getEmail() {
		return email;
	}

	@Override
	public void handleReceived(Message message) {
		switch (message.getType()) {
			case Message.CHAT_MESSAGE:
				if (message instanceof ChatMessage) {
					ChatMessage msg = (ChatMessage)message;
					print(msg.getNick() + ": ", colorNick);
					println(message.getMessage(), colorMsg);						
				}
				break;
			case Message.STATUS_MESSAGE:
				println(message.getMessage(), colorInfo);
				break;
			case Message.ERROR_MESSAGE:
				println(message.getMessage(), colorError);
				break;
			default:
				println("Unknown message type from server.", colorError);
				break;
		}
	}

}
