import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONObject;

public class RepoForm extends GHForm {

	String url;

	public RepoForm(String url) {
		super(url);
		this.url = url;
		addCommand(GH.releasesCmd);
		addCommand(GH.ownerCmd);
		addCommand(GH.forksCmd);
	}

	void loadInternal() throws Exception {
		// TODO
		JSONObject r = (JSONObject) GH.api("repos/".concat(url));
		
		StringItem s;
		String t;
		
		s = new StringItem(null, r.getString("full_name"));
		s.setFont(GH.medfont);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		append(s);

		if ((t = r.getString("description")) != null) {
			s = new StringItem("About", t);
			s.setFont(GH.medfont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			append(s);
		}

		if ((t = r.getString("homepage")) != null) {
			s = new StringItem(null, t);
			s.setFont(GH.smallfont);
			s.setDefaultCommand(GH.linkCmd);
			s.setItemCommandListener(GH.midlet);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			append(s);
		}

		if (r.getBoolean("fork")) {
			s = new StringItem(null, "Forked from " + r.getObject("parent").getString("full_name"));
			s.setFont(GH.smallfont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			append(s);
		}
	}

}
