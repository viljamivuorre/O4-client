package oy.tol.chatclient;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import com.diogonunes.jcolor.Ansi;
import com.diogonunes.jcolor.Attribute;

import oy.tol.chat.ChatMessage;
import oy.tol.chat.ErrorMessage;
import oy.tol.chat.ListChannelsMessage;
import oy.tol.chat.Message;
import oy.tol.chat.StatusMessage;

/**
 * ChatClient is the console based UI for the ChatServer. It profides the
 * necessary functionality for chatting. The actual comms with the ChatServer
 * happens in the ChatHttpClient class.
 */
public class ChatClient implements ChatClientDataProvider {

	private static final String SERVER = "localhost:10000";
	private static final String CMD_NICK = "/nick";
	private static final String CMD_JOIN = "/join";
	private static final String CMD_LIST = "/list";
	private static final String CMD_TOPIC = "/topic";
	private static final String CMD_COLOR = "/color";
	private static final String CMD_HELP = "/help";
	private static final String CMD_INFO = "/info";
	private static final String CMD_EXIT = "/exit";

	private String currentServer = SERVER; // URL of the server without paths.
	private String nick = null; // Nickname, user can change the name visible in chats.
	private String currentChannel = "main";
	private ChatTCPClient tcpClient = null; // Client handling the requests & responses.
	private boolean running = true;

	private static boolean useColorOutput = false;

	public static final Attribute colorDate = Attribute.GREEN_TEXT();
	public static final Attribute colorOwnNick = Attribute.BRIGHT_BLUE_TEXT();
	public static final Attribute colorMsg = Attribute.CYAN_TEXT();
	public static final Attribute colorError = Attribute.BRIGHT_RED_TEXT();
	public static final Attribute colorInfo = Attribute.YELLOW_TEXT();
	public static final Attribute colorOtherNick = Attribute.BRIGHT_YELLOW_TEXT();
	public static final Attribute fromServerInfo = Attribute.BRIGHT_BLUE_TEXT();

	private static final DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder()
			.appendValue(HOUR_OF_DAY, 2)
			.appendLiteral(':')
			.appendValue(MINUTE_OF_HOUR, 2)
			.optionalStart()
			.appendLiteral(':')
			.appendValue(SECOND_OF_MINUTE, 2)
			.toFormatter();

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
		while (running && tcpClient.isConnected()) {
			try {
				printPrompt(LocalDateTime.now(), nick, "", colorMsg);
				String command = console.readLine().trim();
				if (!running) {
					break;
				}
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
					case CMD_LIST:
						listChannels();
						break;
					case CMD_TOPIC:
						changeTopic(console, commandParameters);
						break;
					case CMD_COLOR:
						useColorOutput = !useColorOutput;
						println("Using color in output: " + (useColorOutput ? "yes" : "no"), colorInfo);
						break;
					case CMD_HELP:
					case "/?":
						printCommands();
						break;
					case CMD_INFO:
					case "/i":
						printInfo();
						break;
					case CMD_EXIT:
						running = false;
						if (null != tcpClient) tcpClient.close();
						tcpClient = null;
						break;
					default:
						if (command.length() > 0) { 
							if (!command.startsWith("/")) {
								postMessage(command);
							} else {
								println(" ** ERROR: " + command + " is not a valid command", colorError);
							}
						}
						break;
				}
			} catch (Exception e) {
				println(" *** ERROR : " + e.getMessage(), colorError);
			}
		}
		if (null != tcpClient) {			
			tcpClient.close();
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
			tcpClient.changeTopicTo(newTopic);
		}
	}

	private void changeChannel(Console console, String channel) {
		String newChannel;
		if (null == channel) {
			print("Change to channel > ", colorInfo);
			newChannel = console.readLine().trim().toLowerCase();
		} else {
			newChannel = channel;
		}
		if (newChannel.length() > 0) {
			currentChannel = newChannel;
			tcpClient.changeChannelTo(channel);	
		}
	}

	private void listChannels() {
		tcpClient.listChannels();	
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
		println("/join      -- Specify a channel to switch to in the chat server", colorInfo);
		println("/list      -- List the channels currently available in the chat server", colorInfo);
		println("/topic     -- Set a topic for the current channel", colorInfo);
		println("/color     -- Toggles color output on/off", colorInfo);
		println("/help, /?   -- Prints out this information", colorInfo);
		println("/info  /i  -- Prints out settings and user information", colorInfo);
		println("/exit      -- Exit the client app", colorInfo);
		println(" > To chat, write a message and press enter to send it.", colorInfo);
	}

	/**
	 * Prints out the configuration of the client.
	 */
	private void printInfo() {
		println("Server    : " + currentServer, colorInfo);
		println("Channel   : " + currentChannel, colorInfo);
		println("Nick      : " + nick, colorInfo);
		println("Use color : " + (useColorOutput ? "yes" : "no"), colorInfo);
	}

	private void printPrompt(LocalDateTime timeStamp, String user, String message, Attribute withAttribute) {
		String dateStr = timeFormatter.format(timeStamp);
		String prompt = String.format("%n[%8s @%s] ", dateStr, currentChannel);
		print(prompt, colorInfo);
		prompt = String.format("%8s > ", user);
		if (this.nick.equals(user)) {
			print(prompt, colorOwnNick);
			if (message.length() > 0) print(message, withAttribute);
		} else if (user.equals("SERVER")) {
			print(prompt, colorInfo);
			if (message.length() > 0) print(message, withAttribute);
		} else {
			print(prompt, colorOtherNick);
			if (message.length() > 0) print(message, withAttribute);
		}
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
	public boolean handleReceived(Message message) {
		boolean continueReceiving = true;
		switch (message.getType()) {
			case Message.CHAT_MESSAGE: {
				if (message instanceof ChatMessage) {
					ChatMessage msg = (ChatMessage)message;
					printPrompt(msg.getSent(), msg.getNick(), msg.getMessage(), colorOtherNick);
				}
				break;
			}

			case Message.LIST_CHANNELS: {
				ListChannelsMessage msg = (ListChannelsMessage)message;
				List<String> channels = msg.getChannels();
				if (null != channels) {
					printPrompt(LocalDateTime.now(), "SERVER", "Channels in server: " + channels.toString(), fromServerInfo);
				}
				break;
			}

			case Message.STATUS_MESSAGE: {
				StatusMessage msg = (StatusMessage)message;
				printPrompt(LocalDateTime.now(), "SERVER", msg.getStatus(), colorInfo);
				break;
			}

			case Message.ERROR_MESSAGE: {
				ErrorMessage msg = (ErrorMessage)message;
				printPrompt(LocalDateTime.now(), "SERVER", msg.getError(), colorError);
				if (msg.requiresClientShutdown()) {
					continueReceiving = false;
					running = false;
					println("\nPress enter", colorError);
				}
				break;
			}

			default:
				println("Unknown message type from server.", colorError);
				break;
		}
		printPrompt(LocalDateTime.now(), nick, "", colorMsg);
		return continueReceiving;
	}

	@Override
	public void connectionClosed() {
		if (running) println("Connection closed", colorError);
		running = false;
	}

}
