/*
Copyright (c) 2026 Arman Jussupgaliyev

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
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.Spacer;
import javax.microedition.lcdui.StringItem;
import java.util.Hashtable;

public class EventsForm extends PagedForm {
	public EventsForm(String title, String url) {
		super(title);
		this.url = url;
	}

	void loadInternal(Thread thread) throws Exception {
		JSONArray r = pagedApi(thread, new StringBuffer(this.url));
		int l = r.size();

		if (urls == null) {
			urls = new Hashtable();
		} else {
			urls.clear();
		}

		StringItem s;
		Spacer sp;
		for (int i = 0; i < l && thread == this.thread; ++i) {
			JSONObject j = r.getObject(i);

			System.out.println(j.format(0));
			System.out.println();

			String type = j.getString("type");
			if ("WatchEvent".equals(type)) {
				// user starred a repo

				JSONObject actor = j.getObject("actor");
				String repo = j.getObject("repo").getString("name");

				s = new StringItem(null, actor.getString("login"));
				s.setFont(GH.smallPlainFont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
				s.setDefaultCommand(GH.userCmd);
				s.setItemCommandListener(GH.midlet);
				safeAppend(thread, s);

				boolean your = repo.startsWith(GH.login.concat("/"));

				s = new StringItem(null, your ? " starred your repository" : " starred a repository");
				s.setFont(GH.smallPlainFont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
				safeAppend(thread, s);

				s = new StringItem(null, GH.localizeDate(j.getString("created_at"), 1));
				s.setFont(GH.smallPlainFont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				safeAppend(thread, s);

				if (your) {
					s = new StringItem(null, repo);
					s.setFont(GH.medPlainFont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.setDefaultCommand(GH.repoCmd);
					s.setItemCommandListener(GH.midlet);
					safeAppend(thread, s);
				} else {
					s = new StringItem(null, repo.substring(0, repo.indexOf('/')));
					s.setFont(GH.medPlainFont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
					s.setDefaultCommand(GH.userCmd);
					s.setItemCommandListener(GH.midlet);
					safeAppend(thread, s);

					s = new StringItem(null, "/");
					s.setFont(GH.medPlainFont);
					s.setLayout(Item.LAYOUT_LEFT);
					safeAppend(thread, s);

					s = new StringItem(null, repo.substring(repo.indexOf('/') + 1));
					s.setFont(GH.medPlainFont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
					s.setDefaultCommand(GH.mdLinkCmd);
					s.setItemCommandListener(GH.midlet);
					urls.put(s, repo);
					safeAppend(thread, s);
				}
			} else if ("ForkEvent".equals(type)) {
				// user forked a repo

				JSONObject actor = j.getObject("actor");

				s = new StringItem(null, actor.getString("login"));
				s.setFont(GH.smallPlainFont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
				s.setDefaultCommand(GH.userCmd);
				s.setItemCommandListener(GH.midlet);
				safeAppend(thread, s);

				JSONObject repo = j.getObject("repo");
				boolean your = repo.has("name") && repo.getString("name").startsWith(GH.login.concat("/"));

				s = new StringItem(null, your ? " forked your repository" : " forked a repository");
				s.setFont(GH.smallPlainFont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
				safeAppend(thread, s);

				s = new StringItem(null, GH.localizeDate(j.getString("created_at"), 1));
				s.setFont(GH.smallPlainFont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				safeAppend(thread, s);

				s = new StringItem(null, j.getObject("payload").getObject("forkee").getString("full_name"));
				s.setFont(GH.medPlainFont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
				s.setDefaultCommand(GH.repoCmd);
				s.setItemCommandListener(GH.midlet);
				safeAppend(thread, s);
			} else if ("PublicEvent".equals(type)) {
				// user made a repo public

				JSONObject actor = j.getObject("actor");

				s = new StringItem(null, actor.getString("login"));
				s.setFont(GH.smallPlainFont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
				s.setDefaultCommand(GH.userCmd);
				s.setItemCommandListener(GH.midlet);
				safeAppend(thread, s);

				s = new StringItem(null, " made this repository public");
				s.setFont(GH.smallPlainFont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
				safeAppend(thread, s);

				// date in this event is always not correct for some reason
//				s = new StringItem(null, GH.localizeDate(j.getString("created_at"), 1));
//				s.setFont(GH.smallPlainFont);
//				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
//				safeAppend(thread, s);

				s = new StringItem(null, j.getObject("repo").getString("name"));
				s.setFont(GH.medPlainFont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
				s.setDefaultCommand(GH.repoCmd);
				s.setItemCommandListener(GH.midlet);
				safeAppend(thread, s);
			} else if ("ReleaseEvent".equals(type)) {
				// release

				JSONObject actor = j.getObject("actor");

				s = new StringItem(null, actor.getString("login"));
				s.setFont(GH.smallPlainFont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
				s.setDefaultCommand(GH.userCmd);
				s.setItemCommandListener(GH.midlet);
				safeAppend(thread, s);

				s = new StringItem(null, " released");
				s.setFont(GH.smallPlainFont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
				safeAppend(thread, s);

				s = new StringItem(null, GH.localizeDate(j.getString("created_at"), 1));
				s.setFont(GH.smallPlainFont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				safeAppend(thread, s);

				// TODO
			} else {
				append("\nUndefined event: " + type + "\n");
//				continue;
			}

			sp = new Spacer(10, 8);
			sp.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			safeAppend(thread, sp);
		}
	}
}
