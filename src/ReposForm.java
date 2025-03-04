import java.util.Hashtable;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

// список репозиториев (форки, т.д)
public class ReposForm extends PagedForm implements ItemCommandListener  {

	String url;
	Hashtable urls;
	boolean users;

	public ReposForm(String url, boolean users) {
		super("Repos " + url);
		this.url = url;
		this.users = users;
	}

	void loadInternal() throws Exception {
		// TODO
		JSONArray r = (JSONArray) GH.api(url);
//		append(r.toString());

		int l = r.size();
		
		urls = new Hashtable();
		
		StringItem s;
		for (int i = 0; i < l; ++i) {
			JSONObject j = r.getObject(i);

			if (users) {
				s = new StringItem(null, j.getObject("owner").getString("login"));
				s.setFont(GH.medfont);
				s.setDefaultCommand(GH.userCmd);
				s.setItemCommandListener(GH.midlet);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
				append(s);
				
				s = new StringItem(null, "/");
				s.setFont(GH.medfont);
				s.setLayout(Item.LAYOUT_LEFT);
				append(s);
			}
			
			s = new StringItem(null, users ? j.getString("name") : j.getString("full_name"));
			s.setFont(GH.medfont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
			s.addCommand(GH.openCmd);
			if (!users) s.addCommand(GH.userCmd);
			s.setDefaultCommand(GH.openCmd);
			s.setItemCommandListener(this);
			urls.put(s, j.getString("full_name"));
			append(s);
		}
	}

	public void commandAction(Command c, Item item) {
		if (urls == null) return;
		String url = (String) urls.get(item);
		
		RepoForm f = new RepoForm(url);
		GH.display(f);
		GH.midlet.start(GH.RUN_LOAD_FORM, f);
	}

}
