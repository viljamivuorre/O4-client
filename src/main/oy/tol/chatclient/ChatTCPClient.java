package oy.tol.chatclient;

import javax.swing.*;

import oy.tol.chat.ChangeTopicMessage;
import oy.tol.chat.ChatMessage;
import oy.tol.chat.ErrorMessage;
import oy.tol.chat.ListChannelsMessage;
import oy.tol.chat.Message;
import oy.tol.chat.StatusMessage;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

public class ChatClient extends JFrame implements ChatClientDataProvider {

    private static final String SERVER = "localhost:10000";
	private static final String CMD_NICK = "/nick";
	private static final String CMD_JOIN = "/join";
	private static final String CMD_LIST = "/list";
	private static final String CMD_TOPIC = "/topic";
	private static final String CMD_COLOR = "/color";
	private static final String CMD_HELP = "/help";
	private static final String CMD_INFO = "/info";
	private static final String CMD_EXIT = "/exit";

	private String currentServer = "localhost"; // URL of the server without paths.
	private int serverPort = 10000; // The server port listening to client connections.
	private String nick = null; // Nickname, user can change the name visible in chats.
	private String currentChannel = "main";
	private String topic;
	private ChatTCPClient tcpClient = null; // Client handling the requests & responses.

	private boolean nickEdit = true;
	private boolean topicEdit = false;
	private boolean channelEdit = false;
	private boolean newUser = false;

	private List<ArrayList<String>> privateChats; //variable to save private chats
	private String chatWithUser; //user with private chat
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");


	// all components in grid
    	private JTextArea chatTextArea1;
	private JTextArea chatTextArea2;
	private JScrollPane scrollPane1;
	private JScrollPane scrollPane2;
	private JPopupMenu channelMenu;
	private JPopupMenu userMenu;
	private JTextField messageTextField;
	private JTextField messageTextField2;
	private JTextField nickTextField;
	private JTextField nickTextField2;
	private JTextField newUserTextField;
	private JTextField newChanneltTextField;
	private JTextField newTopicTextField;
	private JButton sendButton;
	private JButton sendButton2;
	private JButton nickButton;
	private JButton nickButton2;
	private JButton userButton;
	private JButton newUserButton;
	private JButton channelButton;
	private JButton newChannelButton;
	private JButton topicButton;
	private JLabel nickLabel;
	private JLabel nickLabel2;
	private JLabel channelLabel;
	private JLabel userLabel;
	private JLabel topicLabel;
	private JTabbedPane tabbedPane;
	private JPanel mainChatPanel;
	private JPanel privateChatPanel;


    public ChatClient() {

        setTitle("chat client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(160, 200);

		//create all components
		chatTextArea1 = new JTextArea();
		chatTextArea2 = new JTextArea();
        scrollPane1 = new JScrollPane(chatTextArea1);
		scrollPane2 = new JScrollPane(chatTextArea2);
		messageTextField = new JTextField();
		messageTextField2 = new JTextField();
		sendButton = new JButton("send");
		sendButton2 = new JButton("send");
	
		nickLabel = new JLabel();
		nickButton = new JButton("create username to chat");
		nickTextField = new JTextField();
		nickLabel2 = new JLabel();
		nickButton2 = new JButton("create username to chat");
		nickTextField2 = new JTextField();

		channelLabel = new JLabel();
		newChanneltTextField = new JTextField();
		newChannelButton = new JButton("new");
		channelButton = new JButton("channels");
		channelMenu = new JPopupMenu();
		
		topicLabel = new JLabel();
		topicButton = new JButton("new topic");
		newTopicTextField = new JTextField();

		userLabel = new JLabel();
		userButton = new JButton("users");
		newUserButton = new JButton("new");
		newUserTextField = new JTextField();
		userMenu = new JPopupMenu();

		//edit nick main page
		nickButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!nickEdit) {
					nickLabel.setVisible(false);
					nickLabel2.setVisible(false);
					nickTextField.setText("");
					nickTextField.setVisible(true);
					nickButton.setText("change");
					nickTextField2.setText("");
					nickTextField2.setVisible(true);
					nickButton2.setText("change");
					nickEdit = true;
				} else {
					String newNick = nickTextField.getText();
					if (!newNick.isEmpty()) {
						if (nick == null) {
							messageTextField.setVisible(true);
							messageTextField2.setVisible(true);
							sendButton.setVisible(true);
							sendButton2.setVisible(true);
							if (currentChannel.length() > 20) {
								channelLabel.setText("channel: " + currentChannel.substring(0, 20) + "...");
							} else {
								channelLabel.setText("channel: " + currentChannel);
							}
							topicLabel.setVisible(true);
							userLabel.setText("");
							nick = newNick;
							tcpClient.postChatMessage("joined to server!");
							pack();
							appendToChatArea("channel: " + currentChannel, 1);
							appendToChatArea("topic: " + topic, 1);
						} else {
							nick = newNick;
						}
						if (nick.length() > 13) {
							nickLabel.setText(nick.substring(0, 10) + "...");
							nickLabel2.setText(nick.substring(0, 10) + "...");
						} else {
							nickLabel.setText(nick);
							nickLabel2.setText(nick);
						}
					}
					nickButton.setText("edit");
					nickTextField.setVisible(false);
					nickButton2.setText("edit");
					nickTextField2.setVisible(false);
					nickLabel.setVisible(true);
					nickLabel2.setVisible(true);
					nickEdit = false;
				}
			}
		});

		//edit nick private page
		nickButton2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!nickEdit) {
					nickLabel.setVisible(false);
					nickLabel2.setVisible(false);
					nickTextField.setText("");
					nickTextField.setVisible(true);
					nickButton.setText("change");
					nickTextField2.setText("");
					nickTextField2.setVisible(true);
					nickButton2.setText("change");
					nickEdit = true;
				} else {
					String newNick = nickTextField2.getText();
					if (!newNick.isEmpty()) {
						if (nick == null) {
							messageTextField.setVisible(true);
							messageTextField2.setVisible(true);
							sendButton.setVisible(true);
							sendButton2.setVisible(true);
							if (currentChannel.length() > 20) {
								channelLabel.setText("channel: " + currentChannel.substring(0, 20) + "...");
							} else {
								channelLabel.setText("channel: " + currentChannel);
							}
							topicLabel.setVisible(true);
							userLabel.setText("");
							nick = newNick;
							tcpClient.postChatMessage("joined to server!");
							appendToChatArea("channel: " + currentChannel, 1);
							appendToChatArea("topic: " + topic, 1);
						} else {
							nick = newNick;
						}
						nickLabel.setText("nick: " + nick);
						nickLabel2.setText("nick: " + nick);
					}
					nickButton.setText("edit");
					nickTextField.setVisible(false);
					nickButton2.setText("edit");
					nickTextField2.setVisible(false);
					nickLabel.setVisible(true);
					nickLabel2.setVisible(true);
					nickEdit = false;
				}
			}
		});

		//send message main page
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
				String message = messageTextField.getText();
				tcpClient.postChatMessage(message);
				String msg = areaMessage(LocalDateTime.now().format(formatter) + " " + nick + ": " + message);
				appendToChatArea(msg, 1);
				messageTextField.setText("");
			}
        });

		//send message private page
		sendButton2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
				String message = messageTextField2.getText();
				tcpClient.postChatMessage("@" + chatWithUser + " " + message);
				String msg = areaMessage(LocalDateTime.now().format(formatter) + " " + nick + ": " + message);

				appendToChatArea(msg, 2);
				//append message in private chat container
				for (ArrayList<String> privateChat : privateChats) {
					if (privateChat.get(0).equals(chatWithUser)) {
						privateChat.add(msg);
						privateChats.remove(privateChat);
						privateChats.add(privateChat);
					}
				}
				messageTextField2.setText("");
			}
        });

		channelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (nick != null) {
					tcpClient.listChannels(); //list channel request
				}
			}
		});

		newChannelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
				if (nick != null) {
					String chn = newChanneltTextField.getText();
					if (!channelEdit) {
						newChannelButton.setText("change");
						newChanneltTextField.setText("");
						newChanneltTextField.setVisible(true);
						channelEdit = true;
					} else {
						if (!chn.isEmpty()) {
							currentChannel = chn;
							tcpClient.changeChannelTo(chn);
							if (chn.length() > 13) {
								channelLabel.setText("channel: " + chn.replaceAll(" \\(\\d+\\)$", "")
								.substring(0, 10) + "...");
							} else {
								channelLabel.setText("channel: " + chn.replaceAll(" \\(\\d+\\)$", ""));
							}
							appendToChatArea("channel: " + currentChannel.replaceAll(" \\(\\d+\\)$", ""), 1);
						}
						newChannelButton.setText("new");
						newChanneltTextField.setVisible(false);
						channelEdit = false;
					}
				}
            }
        });

		topicButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
				if (nick !=null) {
					if (!topicEdit) {
						newTopicTextField.setVisible(true);
						topicButton.setText("change");
						topicEdit = true;
					} else {
						String newTopic = newTopicTextField.getText();
						if (!newTopic.isEmpty()) {
							tcpClient.changeTopicTo(newTopic);
						}
						newTopicTextField.setText("");
						topicButton.setText("new topic");
						newTopicTextField.setVisible(false);
						topicEdit = false;
					}
				}
            }
        });

		userButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (nick != null) {
					userMenu = new JPopupMenu();
					for (ArrayList<String> privateChat : privateChats) {  //find all users with private chat
							JMenuItem menuItem = new JMenuItem(privateChat.get(0));
							menuItem.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent e) {
									chatTextArea2.setText("");
									for (int i=1; i<privateChat.size(); i++) {
										appendToChatArea(privateChat.get(i), 2);
									}
									chatWithUser = privateChat.get(0);
									appendToChatArea("chat with user: " + chatWithUser, 2);
									if (chatWithUser.length() > 15) {
										userLabel.setText("@" + chatWithUser .substring(0, 15) + "...");
									} else {
										userLabel.setText("@" + chatWithUser);
									}
								}
							});
							userMenu.add(menuItem); //append user to list
					}
					userMenu.show(userButton, userButton.getWidth()/2, userButton.getHeight()/2); //show list of users
				}
			}
		});

		newUserButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (nick != null) {
					if (!newUser) {
						newUserTextField.setText("");
						newUserTextField.setVisible(true);
						newUserButton.setText("chat");
						newUser = true;
					} else {
						String user = newUserTextField.getText();
						if (!user.isEmpty()) {
							ArrayList<String> privateChat = new ArrayList<>();
							for (int i=0; i<privateChats.size(); i++) {
								if (privateChats.get(i).get(0).equals(user)) {
									privateChat = privateChats.get(i);
									break;
								}
							}
							if (privateChat.isEmpty()) {
								privateChat.add(user);
								privateChats.add(privateChat);
							}
							chatTextArea2.setText("");
							for (int i=1; i<privateChat.size(); i++) {
								appendToChatArea(privateChat.get(i), 2);
							}
							chatWithUser = user;
							if (chatWithUser.length() > 15) {
								userLabel.setText("@" + chatWithUser .substring(0, 15) + "...");
							} else {
								userLabel.setText("@" + chatWithUser);
							}
						}
						newUserTextField.setVisible(false);
						newUserButton.setText("new");
						newUser = false;
					}
				}
			}
		});

		//create tabs
		tabbedPane = new JTabbedPane();
        mainChatPanel = new JPanel(new GridBagLayout());
        privateChatPanel = new JPanel(new GridBagLayout());

		//add tabs
        tabbedPane.addTab("Main", mainChatPanel);
        tabbedPane.addTab("Private", privateChatPanel);

		GridBagConstraints c = new GridBagConstraints();

		//add all components
		c.fill = GridBagConstraints.BOTH;
		

		// main tab contents

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 6;
		mainChatPanel.add(Box.createVerticalStrut(10), c);

		c.gridx = 15;
		c.gridy = 0;
		c.gridwidth = 6;
		mainChatPanel.add(Box.createVerticalStrut(10), c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 13;
		mainChatPanel.add(Box.createHorizontalStrut(10), c);

		c.gridx = 5;
		c.gridy = 1;
		mainChatPanel.add(Box.createHorizontalStrut(10), c);

		c.gridheight = 2;
		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 2;
		mainChatPanel.add(Box.createVerticalStrut(60), c);

		c.ipady = 0;
		c.gridheight = 1;
		c.gridx = 1;
		c.gridy = 6;
		mainChatPanel.add(channelButton, c);
		channelButton.setMinimumSize(new Dimension(40, 10));
		channelButton.setMaximumSize(new Dimension(60, 10));

		c.gridx = 1;
		c.gridy = 7;
		c.gridwidth = 2;
		mainChatPanel.add(Box.createVerticalStrut(30), c);

		c.gridx = 1;
		c.gridy = 8;
		mainChatPanel.add(newChannelButton, c);
		newChannelButton.setMinimumSize(new Dimension(40, 10));
		newChannelButton.setMaximumSize(new Dimension(60, 10));

		c.gridx = 1;
		c.gridy = 9;
		mainChatPanel.add(newChanneltTextField, c);
		newChanneltTextField.setMinimumSize(new Dimension(40, 10));
		newChanneltTextField.setMaximumSize(new Dimension(60, 10));

		c.gridx = 1;
		c.gridy = 10;
		mainChatPanel.add(Box.createVerticalStrut(30), c);

		c.gridx = 1;
		c.gridy = 11;
		mainChatPanel.add(topicButton, c);
		topicButton.setMinimumSize(new Dimension(40, 10));
		topicButton.setMaximumSize(new Dimension(60, 10));

		c.gridx = 1;
		c.gridy = 12;
		mainChatPanel.add(newTopicTextField, c);
		newTopicTextField.setMinimumSize(new Dimension(40, 10));
		newTopicTextField.setMaximumSize(new Dimension(60, 10));

		c.gridheight = 2;
		c.gridx = 1;
		c.gridy = 13;
		c.gridwidth = 2;
		c.gridheight = 2;
		mainChatPanel.add(Box.createVerticalStrut(30), c);

		c.gridheight = 1;
		c.gridx = 3;
		c.gridy = 2;
		mainChatPanel.add(channelLabel, c);
		channelLabel.setMinimumSize(new Dimension(70, 10));
		channelLabel.setMaximumSize(new Dimension(70, 10));

		c.gridwidth = 1;
		c.gridx = 4;
		c.gridy = 1;
		mainChatPanel.add(nickLabel, c);
		nickLabel.setMinimumSize(new Dimension(40, 20));
		nickLabel.setMaximumSize(new Dimension(40, 20));

		c.gridx = 4;
		c.gridy = 1;
		mainChatPanel.add(nickTextField, c);
		nickButton.setMinimumSize(new Dimension(40, 20));
		nickButton.setMaximumSize(new Dimension(40, 20));

		c.gridwidth = 2;
		c.gridx = 3;
		c.gridy = 3;
		mainChatPanel.add(topicLabel, c);
		topicLabel.setMinimumSize(new Dimension(80, 10));
		topicLabel.setMaximumSize(new Dimension(80, 10));

		c.gridwidth = 1;
		c.gridx = 4;
		c.gridy = 2;
		mainChatPanel.add(nickButton, c);
		nickButton.setMinimumSize(new Dimension(10, 10));
		nickButton.setMaximumSize(new Dimension(10, 10));

		c.gridx = 3;
		c.gridy = 4;
		mainChatPanel.add(Box.createVerticalStrut(10), c);

		c.gridheight = 1;
		c.gridx = 4;
		c.gridy = 4;
		c.gridwidth = 2;
		c.gridheight = 2;
		mainChatPanel.add(Box.createVerticalStrut(20), c);

		c.ipadx = 70;
		c.gridwidth = 2;
		c.gridheight = 9;
		c.gridx = 3;
		c.gridy = 5;
		mainChatPanel.add(scrollPane1, c);
		scrollPane1.setMinimumSize(new Dimension(50, 130));
		scrollPane1.setMaximumSize(new Dimension(110, 200));

		c.gridheight = 1;
		c.gridwidth = 1;
		c.ipadx = 300;
		c.gridx = 3;
		c.gridy = 14;
		mainChatPanel.add(messageTextField, c);
		messageTextField.setMinimumSize(new Dimension(70, 20));
		messageTextField.setMaximumSize(new Dimension(70, 20));

		c.ipadx = 10;
		c.gridx = 4;
		c.gridy = 14;
		mainChatPanel.add(sendButton, c);
		messageTextField.setMinimumSize(new Dimension(10, 20));
		messageTextField.setMaximumSize(new Dimension(10, 20));



		// private tab contents

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 6;
		privateChatPanel.add(Box.createVerticalStrut(10), c);

		c.gridx = 15;
		c.gridy = 0;
		c.gridwidth = 6;
		privateChatPanel.add(Box.createVerticalStrut(10), c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 13;
		privateChatPanel.add(Box.createHorizontalStrut(10), c);

		c.gridx = 5;
		c.gridy = 1;
		privateChatPanel.add(Box.createHorizontalStrut(10), c);

		c.gridheight = 2;
		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 2;
		privateChatPanel.add(Box.createVerticalStrut(60), c);

		c.ipady = 0;
		c.gridheight = 1;
		c.gridx = 1;
		c.gridy = 6;
		privateChatPanel.add(userButton, c);
		channelButton.setMinimumSize(new Dimension(40, 10));
		channelButton.setMaximumSize(new Dimension(60, 10));

		c.gridx = 1;
		c.gridy = 7;
		c.gridwidth = 2;
		privateChatPanel.add(Box.createVerticalStrut(20), c);

		c.gridx = 1;
		c.gridy = 8;
		privateChatPanel.add(newUserButton, c);
		newChannelButton.setMinimumSize(new Dimension(40, 10));
		newChannelButton.setMaximumSize(new Dimension(60, 10));

		c.gridx = 1;
		c.gridy = 9;
		privateChatPanel.add(newUserTextField, c);
		newChanneltTextField.setMinimumSize(new Dimension(40, 10));
		newChanneltTextField.setMaximumSize(new Dimension(60, 10));

		c.gridheight = 5;
		c.gridx = 1;
		c.gridy = 10;
		c.gridwidth = 2;
		c.gridheight = 2;
		privateChatPanel.add(Box.createVerticalStrut(60), c);

		c.gridheight = 1;
		c.gridx = 3;
		c.gridy = 2;
		privateChatPanel.add(userLabel, c);
		channelLabel.setMinimumSize(new Dimension(70, 10));
		channelLabel.setMaximumSize(new Dimension(70, 10));

		c.gridwidth = 1;
		c.gridx = 4;
		c.gridy = 1;
		privateChatPanel.add(nickLabel2, c);
		nickLabel.setMinimumSize(new Dimension(40, 20));
		nickLabel.setMaximumSize(new Dimension(40, 20));

		c.gridx = 4;
		c.gridy = 1;
		privateChatPanel.add(nickTextField2, c);
		nickButton.setMinimumSize(new Dimension(40, 20));
		nickButton.setMaximumSize(new Dimension(40, 20));

		c.gridwidth = 1;
		c.gridx = 4;
		c.gridy = 2;
		privateChatPanel.add(nickButton2, c);
		nickButton.setMinimumSize(new Dimension(10, 10));
		nickButton.setMaximumSize(new Dimension(10, 10));

		c.gridx = 3;
		c.gridy = 4;
		privateChatPanel.add(Box.createVerticalStrut(10), c);

		c.gridheight = 1;
		c.gridx = 4;
		c.gridy = 4;
		c.gridwidth = 2;
		c.gridheight = 2;
		privateChatPanel.add(Box.createVerticalStrut(20), c);

		c.ipadx = 70;
		c.gridwidth = 2;
		c.gridheight = 9;
		c.gridx = 3;
		c.gridy = 5;
		privateChatPanel.add(scrollPane2, c);
		scrollPane1.setMinimumSize(new Dimension(50, 100));
		scrollPane1.setMaximumSize(new Dimension(110, 200));

		c.gridheight = 1;
		c.gridwidth = 1;
		c.ipadx = 300;
		c.gridx = 3;
		c.gridy = 14;
		privateChatPanel.add(messageTextField2, c);
		messageTextField.setMinimumSize(new Dimension(70, 20));
		messageTextField.setMaximumSize(new Dimension(70, 20));

		c.ipadx = 10;
		c.gridx = 4;
		c.gridy = 14;
		privateChatPanel.add(sendButton2, c);
		messageTextField.setMinimumSize(new Dimension(10, 20));
		messageTextField.setMaximumSize(new Dimension(10, 20));


		add(tabbedPane);


		messageTextField.setVisible(false);
		messageTextField2.setVisible(false);
		sendButton.setVisible(false);
		sendButton2.setVisible(false);

		newTopicTextField.setVisible(false);
		newChanneltTextField.setVisible(false);
		newUserTextField.setVisible(false);
		topicLabel.setVisible(false);

		//create private chat container
		privateChats = new ArrayList<ArrayList<String>>();
		setUndecorated(false);
		pack();

    }

	//append in chat area
    public void appendToChatArea(String message, int area) {
		if (nick != null) {
			if (area == 1) { //main tab
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						chatTextArea1.append(message + "\n");
					}
				});
			} else if (area == 2) { //private tab
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						chatTextArea2.append(message + "\n");
					}
				});
			} else { //both tabs
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						chatTextArea1.append(message + "\n");
					}
				});
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						chatTextArea2.append(message + "\n");
					}
				});
			}
		}
    }

	private String areaMessage(String msg) {

		if (msg.length() < 70) {
			return msg;
		}

		//make msg to fit in screen horizontal

		String areaMessage = msg; // areaMessage will be final form
		int i = 50;
		int j;
		for (j=i; j>i-9; j--) {
				if (msg.charAt(j) == ' ') {
					areaMessage = msg.substring(0, j) + "\n";
					i = j + 50;
					break;
				}
				if (j == i-8) {
					areaMessage = msg.substring(0, i) + "\n";
					i = i + 50;
					break;
				}
		}
		while (i < msg.length()-1) {
			for (j=i; j>i-9; j--) {
				if (msg.charAt(j) == ' ') {
					areaMessage = areaMessage + msg.substring(i-50, j) + "\n";
					i = j+50;
					break;
				}
				if (j == i-8) {
					areaMessage = areaMessage + msg.substring(i-50, i) + "\n";
					i = i + 50;
					break;
				}
			}
		}
		return areaMessage + msg.substring(i-50, msg.length()); //final form of msg
    }

    public void run() throws FileNotFoundException, IOException {
        // Käynnistä TCP-asiakas säikeessä
        tcpClient = new ChatTCPClient(this);
        new Thread(tcpClient).start();
        // Näytä ikkuna
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ChatClient chatClientGUI = new ChatClient();
                try {
                    chatClientGUI.run();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public String getServer() {
        return currentServer;
    }

    @Override
    public int getPort() {
        return serverPort;  // Palvelimen porttinumero
    }

    @Override
    public String getNick() {
        return nick;
    }

    @Override
    public boolean handleReceived(Message message) {
        // Käsittele vastaanotettu viesti
		switch (message.getType()) {

        	case Message.CHAT_MESSAGE:
				ChatMessage chatMessage = (ChatMessage) message;

				String msg = areaMessage(chatMessage.getSent().format(formatter) + " " +
				 chatMessage.getNick() + ": " + chatMessage.getMessage());

				if (chatMessage.isDirectMessage()) {
					ArrayList<String> privateChat = new ArrayList<String>(); //create new chat object

					//find chat with senders username and add message to chat. 
					for (int i=0; i<privateChats.size(); i++) {
						if (privateChats.get(i).get(0).equals(chatMessage.getNick())) {
							privateChat = privateChats.get(i);
							privateChats.remove(i);
							break;
						}
					}

					if (privateChat.isEmpty()) {
						privateChat.add(chatMessage.getNick());
					}

					privateChat.add(msg);
					privateChats.add(privateChat);

					//if chat with sender is open, append to private tab chatarea. else note new private chat in both tabs
					if (chatMessage.getNick().equals(chatWithUser)) {
						appendToChatArea(msg, 2);
					} else {
						appendToChatArea(chatMessage.getSent().format(formatter) +
						 " " + chatMessage.getNick() + " sent you private message!", 0);
					}

				} else {
					appendToChatArea(msg, 1);
				}
				break;

            case Message.ERROR_MESSAGE:
				ErrorMessage errorMessage = (ErrorMessage) message;
				appendToChatArea("Virhe: " + errorMessage.getError(), 0);
				break;

			case Message.LIST_CHANNELS: //after clicking channelbutton program request channels from servers and creates popuplist of current channels
				ListChannelsMessage listChannelsMessage = (ListChannelsMessage)message;
				channelMenu = new JPopupMenu();
					for (String chn : listChannelsMessage.getChannels()) {
							JMenuItem menuItem = new JMenuItem(chn);
							menuItem.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent e) {
									currentChannel = chn;
									if (chn.length() > 13) {
										channelLabel.setText("channel: " + chn.replaceAll(" \\(\\d+\\)$", "")
										.substring(0, 10) + "...");
									} else {
										channelLabel.setText("channel: " + chn.replaceAll(" \\(\\d+\\)$", ""));
									}
									channelLabel.setText("channel: " + chn.replaceAll(" \\(\\d+\\)$", ""));
									tcpClient.changeChannelTo(chn.replaceAll(" \\(\\d+\\)$", ""));
									appendToChatArea("channel: " + currentChannel.replaceAll(" \\(\\d+\\)$", ""), 1);
								}
							});
							channelMenu.add(menuItem);
					}
					channelMenu.show(channelButton, channelButton.getWidth()/2, channelButton.getHeight()/2);
				break;

			case Message.CHANGE_TOPIC:
				ChangeTopicMessage topicMessage = (ChangeTopicMessage)message;
				topic = topicMessage.getTopic();
				if (topic.length() > 20) {
					topicLabel.setText("topic: " + topic.substring(0, 20) + "...");
				} else {
					topicLabel.setText("topic: " + topic);
				}
				appendToChatArea("topic: " + topic, 1);
				break;
        }
        return true;
    }

    @Override
    public void connectionClosed() {
        // Suoritetaan kun yhteys suljetaan
        appendToChatArea("Yhteys suljettu.", 0);
    }

}
