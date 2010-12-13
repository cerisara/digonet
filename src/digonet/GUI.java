package digonet;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.Timer;

/**
 * Distributed Go Network

Dear go players,

 ** What is DiGoNet ? **

I would like to set-up a new network (called DiGoNet) for playing go "by emails", in the vein of
OGS (Online Go Server) and DGS (Dragon Go Server). The main difference between DiGoNet and
OGS or DGS is that DiGoNet is completely distributed, which means that there is no server at
all, but just a set of clients that are interconnected into a network dedicated to playing go.
There are two main advantages of not relying on a central server:
1- There is no cost at all for maintaining the server: playing DiGoNet is totally free !
2- There is never any downtime of the server: the network will always be alive, never down, as
long as there are players using it.

 ** How does it work ? **

Every single player in DiGoNet must have (1) a DiGoNet client, and (2) a personal webpage, which contains a complete "image"
of the state of the network at a given time. When player A is engaged in a game with player B,
A writes down his next move in the client, which automatically update the webpage with this move,
and eventually send an email to B. B then "refreshes" his client, which downloads from the webpage
of A the new move, and so on.

Player A may also broadcast a challenge (or announce a tournament): his client simply writes this challenge on his own website.
This announce shall "slowly" diffuse all over the network thanks to two mechanism:
(1) Every player's client engaged in games automatically updates at every move from his opponents' websites
(2) Every running client furthermore updates from a few random sites every hour

Having no server implies to support some degree of asynchrony between clients: within a single game,
the former mechanism reduces this asynchrony to the minimum, while the second mechanism ensures some
slower diffusion of challenges and tournaments.

 ** When can it be up and running ? **

For now, I have simply written in Java the basic building blocks of the client: the source if
available at GitHub://

The client can be finished in a few days only, but I need some help to finalize it, and in particular concerning:
1- What is the equation to update the rank of a player from his played games ?
2- Does anyone know how to automatically uploads a file on a personal website in Java ? The client shall support
   most standard connections protocols to access a website: local files, FTP, SSH, ... ?
3- Same question for sending emails in Java ?
4- Does anyone know open-source SGF editor in Java ? It is not an absolute requirement, but
   it may be easier for the players not to rely on an external SGF editor...

Thanks for your help !
Christophe

 * 
 * @author cerisara
 *
 */
public class GUI extends JFrame {
	Player myself = null;
	List<Player> players = new ArrayList<Player>();
	Timer websitesCheckerTimer = null;
	File localConfigDir = null;

	public GUI() {
		super("Distributed Go Network");

		initListes();

		initTimer();

		setSize(700,600);
		initgui();
		setVisible(true);
	}

	private void updateFromRandomSites() {
		Random r = new Random();
		for (int i=0;i<3;i++) {
			int j = r.nextInt(players.size());
			System.out.println("check web site "+players.get(j).website);
			updateFromSite(players.get(j).website);
		}
	}

	/**
	 * @param urls must design an UTF-8 text file through HTTP
	 */
	private void updateFromSite(String urls) {
		try {
			URL url = new URL(urls);
			URLConnection connection = url.openConnection();
			connection.connect();
			InputStream urlin = connection.getInputStream();
			BufferedReader f = new BufferedReader(new InputStreamReader(urlin));
			boolean updated=false;
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				// part1: list of players
				if (!s.startsWith("players ")) {
					System.out.println("ERROR in players list URL "+urls);
					break;
				}
				int nplayers = Integer.parseInt(s.substring(8));
				HashSet<Player> allplayers = new HashSet<Player>();
				// add known players
				allplayers.addAll(players);
				// add unkown players
				for (int i=0;i<nplayers;i++) {
					s = f.readLine();
					if (s==null) {
						System.out.println("ERROR in players list not enough URL "+urls);
					}
					Player p = new Player(s);
					allplayers.add(p);
				}
				if (allplayers.size()!=players.size()) {
					System.out.println("updated list of players ! "+allplayers.size());
					players.clear();
					players.addAll(allplayers);
					Collections.sort(players);
					updated = true;
				}
			}
			f.close();

			if (updated) {
				saveListsOnSite();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void saveListsOnSite() {
		// TODO
	}

	/**
	 * starts the timer that checks every hour 3 random websites for updates
	 */
	private void initTimer() {
		websitesCheckerTimer = new Timer(3600000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateFromRandomSites();
			}
		});
	}

	private void initmyself() {
		String nickname = JOptionPane.showInputDialog(null, "Enter your nickname");
		String rank = JOptionPane.showInputDialog(null, "Enter your estimated rank");
		// TODO: convert rank into a single number
		String myurl = JOptionPane.showInputDialog(null, "Enter your website URL");
		myself = new Player(nickname+";"+rank+";"+myurl);
		if (!players.contains(myself)) {
			players.add(myself);
			saveListsOnSite();
		}
		saveLocalConfig();
	}

	/**
	 * load the lists of players, challenges, ...
	 */
	private void initListes() {
		String hdir = System.getenv("HOME");
		if (hdir==null) {
			hdir = System.getenv("HOMEPATH");
		}
		System.out.println("HOme = "+hdir);
		localConfigDir = new File(hdir+"/.digonet");
		if (!localConfigDir.exists()) {
			String url0 = JOptionPane.showInputDialog(null, "No lists found: enter the URL to download one");
			if (url0==null) {
				JOptionPane.showMessageDialog(null, "No URL - you will not join DiGoNet");
			} else if (!downloadList(url0)) {
				JOptionPane.showMessageDialog(null, "URL not valid - you will not join DiGoNet");
			}
			initmyself();
		}
		localConfigDir = new File(hdir+"/.digonet");
		if (!localConfigDir.exists()) {
			JOptionPane.showMessageDialog(null, "ERRORS: no lists found, but some should have been downloaded ?");
			System.exit(1);
		}
		System.out.println("lists found: loading them...");
		loadLists();
	}

	private boolean downloadList(String url) {
		// TODO
		return false;
	}

	// ===================== Local Config Files management

	private void loadLists() {
		try {
			{ // myself.txt
				BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(localConfigDir.getAbsolutePath()+"/myself.txt"), Charset.forName("UTF-8")));
				// load my identity
				String s=f.readLine();
				myself = new Player(s);
				f.close();
			}
			{ // players.txt
				// load players
				players = new ArrayList<Player>();
				BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(localConfigDir.getAbsolutePath()+"/players.txt"), Charset.forName("UTF-8")));
				for (;;) {
					String s = f.readLine();
					if (s==null) break;
					Player p = new Player(s);
					players.add(p);
				}
				System.out.println("loaded "+players.size()+" players");
				f.close();
			}
			{
				// load challenges
				// TODO
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void saveLocalConfig() {
		try {
			localConfigDir.mkdirs();
			PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream(localConfigDir.getAbsolutePath()+"/myself.txt"), Charset.forName("UTF-8")));
			if (myself!=null) {
				f.println(myself.string4save());
			}
			f.close();
			f = new PrintWriter(new OutputStreamWriter(new FileOutputStream(localConfigDir.getAbsolutePath()+"/players.txt"), Charset.forName("UTF-8")));
			if (players!=null) {
				for (Player p : players) {
					f.println(p.string4save());
				}
			}
			f.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	// =============================================================
	private void playOneMove() {
	}

	private void initgui() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JMenuBar bar = new JMenuBar();
		setJMenuBar(bar);

		JMenu options = new JMenu("options");
		bar.add(options);

		JMenu games = new JMenu("games");
		bar.add(games);
		JMenuItem playOneMove = new JMenuItem("play One Move");
		games.add(playOneMove);
		playOneMove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				playOneMove();
			}
		});
		JMenuItem answerChallenge = new JMenuItem("answer Challenge");
		games.add(answerChallenge);
		// TODO
		JMenuItem proposeChallenge = new JMenuItem("broadcast new challenge");
		games.add(proposeChallenge);
		// TODO
		
		setLayout(new FlowLayout());
		Box playerbox = Box.createVerticalBox();
		add(playerbox);
		playerbox.add(new JLabel("Players list"));
		JList playerList = new JList(players.toArray());
		JScrollPane playerPane = new JScrollPane(playerList);
		playerbox.add(playerPane);
	}

	public static void main(String args[]) {
		GUI m = new GUI();
	}
}
