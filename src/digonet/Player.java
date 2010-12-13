package digonet;

import java.util.ArrayList;
import java.util.List;

public class Player implements Comparable {
	String login;
	String rank;
	String website;
	List<Game> games = null;
	
	public Player(String s) {
		String[] ss = s.split(";");
		if (ss.length>=1)
			login = ss[0];
		else login = "unknown";
		if (ss.length>=2)
			rank = ss[1];
		else rank = "20k";
		if (ss.length>=3)
			website = ss[2];
		else website = "http://nosite.com/digonet.cor";
		// load his games
		games = new ArrayList<Game>();
		for (int i=3;i<ss.length;i++) {
			Game g = new Game(ss[i]);
			games.add(g);
		}
	}
	
	public boolean equals(Object o) {
		Player p = (Player)o;
		return (p.website.equals(website));
	}
	
	public int hashCode() {
		return website.hashCode();
	}

	@Override
	public int compareTo(Object o) {
		Player p = (Player)o;
		return website.compareTo(p.website);
	}
	
	public String toString() {
		return login+" "+rank;
	}
	
	public String string4save() {
		String s = login+";"+rank+";"+website;
		for (Game g : games) {
			s+=";"+g.string4save();
		}
		return s;
	}
}
