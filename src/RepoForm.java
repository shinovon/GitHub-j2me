/*
Copyright (c) 2025 Arman Jussupgaliyev

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
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

	void loadInternal(Thread thread) throws Exception {
		// TODO
		JSONObject r = (JSONObject) GH.api("repos/".concat(url));
		
		// cancel check
		if (thread != this.thread) return;
		
		StringItem s;
		String t;
		
		s = new StringItem(null, r.getObject("owner").getString("login"));
		s.setFont(GH.medfont);
		s.setDefaultCommand(GH.userCmd);
		s.setItemCommandListener(GH.midlet);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
		append(s);
		
		s = new StringItem(null, "/");
		s.setFont(GH.medfont);
		s.setLayout(Item.LAYOUT_LEFT);
		append(s);
		
		s = new StringItem(null, r.getString("name"));
		s.setFont(GH.medfont);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
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
		
		s = new StringItem(null, r.getString("forks") + " forks");
		s.setFont(GH.smallfont);
		s.setDefaultCommand(GH.forksCmd);
		s.setItemCommandListener(GH.midlet);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		append(s);
		
		s = new StringItem(null, "Releases", StringItem.BUTTON);
		s.setDefaultCommand(GH.releasesCmd);
		s.setItemCommandListener(GH.midlet);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		append(s);
	}

}
