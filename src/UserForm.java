import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONObject;

public class UserForm extends GHForm {

	String user;

	public UserForm(String user) {
		super(user);
		this.user = user;
		addCommand(GH.reposCmd);
	}

	void loadInternal() throws Exception {
		JSONObject r = (JSONObject) GH.api("users/".concat(user));
		
		StringItem s;
		String t;
		
		if ((t = r.getString("name")) != null) {
			s = new StringItem(null, t);
			s.setFont(GH.largefont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			append(s);
		}
		
		s = new StringItem(null, r.getString("login"));
		s.setFont(GH.medfont);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		append(s);

		if ((t = r.getString("bio")) != null) {
			s = new StringItem(null, t);
			s.setFont(GH.medfont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			append(s);
		}

		if ((t = r.getString("blog")) != null) {
			s = new StringItem("Blog", t);
			s.setFont(GH.medfont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			s.setDefaultCommand(GH.linkCmd);
			s.setItemCommandListener(GH.midlet);
			append(s);
		}
		
		s = new StringItem(null, r.getString("followers") + " followers, " + r.getString("following") + " following");
		s.setFont(GH.smallfont);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		append(s);
		
		s = new StringItem(null, r.getString("public_repos") + " repositories");
		s.setFont(GH.smallfont);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		append(s);
	}

}
