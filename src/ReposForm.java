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
import java.util.Hashtable;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

// list of repositories (forks, etc)
public class ReposForm extends PagedForm implements ItemCommandListener  {

	String url;
	Hashtable urls;
	boolean users;
	String sort;
	boolean mini;

	public ReposForm(String url, String title, String sort, boolean users) {
		super(title);
		this.url = url;
		this.users = users;
		this.sort = sort;
	}

	void loadInternal(Thread thread) throws Exception {
		deleteAll();
		
		JSONArray r = pagedApi(thread, sort != null ? url.concat("?sort=").concat(sort) : url.concat("?"));
		int l = r.size();
		
		if (urls == null) {
			urls = new Hashtable();
		} else {
			urls.clear();
		}
		StringItem s;
		String t;
		for (int i = 0; i < l && thread == this.thread; ++i) {
			JSONObject j = r.getObject(i);

			if (users) {
				s = new StringItem(null, j.getObject("owner").getString("login"));
				s.setFont(GH.medfont);
				s.setDefaultCommand(GH.userCmd);
				s.setItemCommandListener(GH.midlet);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
				safeAppend(thread, s);
				
				s = new StringItem(null, "/");
				s.setFont(GH.medfont);
				s.setLayout(Item.LAYOUT_LEFT);
				safeAppend(thread, s);
			}
			
			s = new StringItem(null, j.getString("name"));
			s.setFont(GH.medfont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
			s.addCommand(GH.openCmd);
			if (!users) s.addCommand(GH.userCmd);
			s.setDefaultCommand(GH.openCmd);
			s.setItemCommandListener(this);
			urls.put(s, j.getString("full_name"));
			safeAppend(thread, s);
			
			if (!mini) {
				if (j.getBoolean("fork", false)) {
					s.setLayout(Item.LAYOUT_LEFT);
					
					s = new StringItem(null, " (Fork)"/*"Forked from " + j.getObject("parent").getString("full_name")*/);
					s.setFont(GH.smallfont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
					safeAppend(thread, s);
				}
				
				if ((t = j.getString("description")) != null) {
					s = new StringItem(null, t);
					s.setFont(GH.smallfont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
					safeAppend(thread, s);
				}
				
			
				if ((t = j.getString("language")) != null) {
					s = new StringItem(null, t);
					s.setFont(GH.smallfont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
					safeAppend(thread, s);
				}
				
				s = new StringItem(null, " Updated ".concat(GH.localizeDate(j.getString("pushed_at"), 1)));
				s.setFont(GH.smallfont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
				safeAppend(thread, s);
				safeAppend(thread, "\n");
			}
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
