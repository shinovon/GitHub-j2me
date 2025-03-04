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

	public ReposForm(String url) {
		super("Repos " + url);
		this.url = url;
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
			
			s = new StringItem(null, j.getString("full_name"));
			s.setFont(GH.medfont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			s.addCommand(GH.openCmd);
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
