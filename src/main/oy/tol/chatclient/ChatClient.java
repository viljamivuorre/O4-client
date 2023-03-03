package oy.tol.chatclient;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
	private static final String CMD_NICK = "/nick";
	private static final String CMD_JOIN = "/join";
	private static final String CMD_TOPIC = "/topic";
	private static final String CMD_COLOR = "/color";
	private static final String CMD_HELP = "/help";
	private static final String CMD_INFO = "/info";
	private static final String CMD_EXIT = "/exit";

	private String currentServer = SERVER; // URL of the server without paths.
	private String nick = null; // Nickname, user can change the name visible in chats.
	private String currentChannel = "main";

	private ChatTCPClient tcpClient = null; // Client handling the requests & responses.

	private static boolean useColorOutput = false;

	public static final Attribute colorDate = Attribute.GREEN_TEXT();
	public static final Attribute colorNick = Attribute.BRIGHT_BLUE_TEXT();
	public static final Attribute colorMsg = Attribute.CYAN_TEXT();
	public static final Attribute colorError = Attribute.BRIGHT_RED_TEXT();
	public static final Attribute colorInfo = Attribute.YELLOW_TEXT();

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
		if (null == nick) {
			println("!! Provide a nick in settings", colorError);
		}
		tcpClient = new ChatTCPClient(this);
		new Thread(tcpClient).start();
		printCommands();
		printInfo();
		Console console = System.console();
		boolean running = true;
		while (running) {
			try {
				String prompt = String.format("O4-chat @%s %s > ", currentChannel, nick);
				print(prompt, colorInfo);
				String command = console.readLine().trim();
				int spaceIndex = command.indexOf(" ");
				String commandString = null;
				String commandParameters = null;
				if (spaceIndex > 0) {
					commandString = command.substring(0, spaceIndex).trim();
					commandParameters = command.substring(spaceIndex+1).trim();
				} else {
					commandString = command;
				}	
				switch (commandString) {
					case CMD_NICK:
						changeNick(console, commandParameters);
						break;
					case CMD_JOIN:
						changeChannel(console, commandParameters);
						break;
					case CMD_TOPIC:
						changeTopic(console, commandParameters);
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
				println(" *** ERROR : " + e.getMessage(), colorError);
			}
		}
		println("Bye!", colorInfo);
	}

	private void changeTopic(Console console, String topic) {
		String newTopic;
		if (null == topic) {
			print("Change the channel topic > ", colorInfo);
			newTopic = console.readLine().trim().toLowerCase();
		} else {
			newTopic = topic;
		}
		if (newTopic.length() > 0) {
			tcpClient.changeChannelTo(currentChannel, newTopic);	
		}
	}

	private void changeChannel(Console console, String channel) throws IOException {
		String newChannel;
		if (null == channel) {
			print("Change to channel > ", colorInfo);
			newChannel = console.readLine().trim().toLowerCase();
		} else {
			newChannel = channel;
		}
		if (newChannel.length() > 0) {
			tcpClient.changeChannelTo(channel, "");	
		}
	}

	/**
	 * User wants to change the nick, so ask it.
	 * 
	 * @param console
	 */
	private void changeNick(Console console, String newNick) {
		if (null == newNick) {
			print("Enter nick > ", colorInfo);
			String changedNick = console.readLine().trim();
			if (changedNick.length() > 0) {
				nick = changedNick;
			}	
		} else {
			if (newNick.length() > 0) {
				nick = newNick;
			}
		}
	}

	/**
	 * Sends a new chat message to the server. User must be logged in to the server.
	 * 
	 * @param message The chat message to send.
	 */
	private void postMessage(String message) {
		if (null != nick) {
			tcpClient.postChatMessage(message);
		} else {
			println("Must set the nick before posting messages!", colorError);
		}
	}

	/**
	 * Print out the available commands.
	 */
	private void printCommands() {
		println("--- O4 Chat Client Commands ---", colorInfo);
		println("/nick      -- Specify a nickname to use in chat server", colorInfo);
		println("/channel   -- Specify a channel to switch to in the chat server", colorInfo);
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
		println("Nick: " + nick, colorInfo);
		println("Using color in output: " + (useColorOutput ? "yes" : "no"), colorInfo);
	}

	public static void print(String item, Attribute withAttribute) {
		if (useColorOutput) {
			System.out.print(Ansi.colorize(item, withAttribute));
		} else {
			System.out.print(item);
		}
	}

	public static void println(String item, Attribute withAttribute) {
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
		currentServer = config.getProperty("server", "127.0.0.1:10000");
		nick = config.getProperty("nick", "");
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
	public String getNick() {
		return nick;
	}

	@Override
	public void handleReceived(Message message) {
		switch (message.getType()) {
			case Message.CHAT_MESSAGE:
				if (message instanceof ChatMessage) {
					ChatMessage msg = (ChatMessage)message;
					print(msg.getNick() + ": ", colorNick);
					String channel = String.format("(%s) %s: > ", currentChannel, msg.sentAsString());
					print(channel, colorInfo); 
					println(message.getMessage(), colorMsg);						
				}
				break;
			case Message.STATUS_MESSAGE:
				println("note: " + message.getMessage(), colorInfo);
				break;
			case Message.ERROR_MESSAGE:
				println("ERROR: " + message.getMessage(), colorError);
				break;
			default:
				println("Unknown message type from server.", colorError);
				break;
		}
		print("O4-chat > ", colorInfo);
	}

}
