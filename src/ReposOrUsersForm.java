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

import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

// list of repositories (forks, etc) or users (stargazers, followers, etc.)
public class ReposOrUsersForm extends PagedForm {

	String sort;
	boolean mini;
	int mode;

	// mode: 0, 1 - repos, 2 - users
	public ReposOrUsersForm(String url, String title, String sort, int mode) {
		super(title);
		this.url = url;
		this.sort = sort;
		this.mode = mode;
	}

	void loadInternal(Thread thread) throws Exception {
		StringBuffer sb = new StringBuffer(url);
		JSONArray r = pagedApi(thread, sort != null ? sb.append("sort=").append(sort) : sb);
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
			
			// user
			if (mode == 2) {
				s = new StringItem(null, j.getString("login"));
				s.setFont(GH.medPlainFont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
				s.setDefaultCommand(GH.userCmd);
				s.setItemCommandListener(GH.midlet);
				safeAppend(thread, s);
				
				if (j.has("contributions")) {
					s = new StringItem(null, j.getString("contributions").concat(GH.L[L_contributions]));
					s.setFont(GH.medPlainFont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					safeAppend(thread, s);
					
					safeAppend(thread, "\n");
				}
				continue;
			}
			
			// repo

			if (mode == 1) {
				s = new StringItem(null, j.getObject("owner").getString("login"));
				s.setFont(GH.medPlainFont);
				s.setDefaultCommand(GH.userCmd);
				s.setItemCommandListener(GH.midlet);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
				safeAppend(thread, s);
				
				s = new StringItem(null, "/");
				s.setFont(GH.medPlainFont);
				s.setLayout(Item.LAYOUT_LEFT);
				safeAppend(thread, s);
			}
			
			s = new StringItem(null, j.getString("name"));
			s.setFont(GH.medPlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
			s.addCommand(GH.openCmd);
			s.setDefaultCommand(GH.mdLinkCmd);
			s.setItemCommandListener(GH.midlet);
			urls.put(s, j.getString("full_name"));
			safeAppend(thread, s);
			
			if (!mini) {
				boolean b, fork = j.getBoolean("fork", false), arch = j.getBoolean("archived", false);
				if ((b = j.getBoolean("private", false)) || fork || arch) {
					s.setLayout(Item.LAYOUT_LEFT);
					
					s = new StringItem(null, GH.L[b ?
							(arch ? L_privateArchive_ : (fork ? L_privateFork_ : L_private_)) :
								(arch ? L_archive_ : L_fork_)]);
					s.setFont(GH.smallPlainFont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
					safeAppend(thread, s);
				}
				
				if ((t = j.getString("description")) != null && t.length() != 0) {
					s = new StringItem(null, t);
					s.setFont(GH.smallPlainFont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					safeAppend(thread, s);
				}
				
				if (b = ((t = j.getString("language")) != null && t.length() != 0)) {
					s = new StringItem(null, t);
					s.setFont(GH.smallPlainFont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
					safeAppend(thread, s);
				}
				
				s = new StringItem(null, (b ? " " : "").concat(GH.L[LUpdated])
						.concat(GH.localizeDate((t = j.getString("pushed_at", null)) != null ? t : j.getString("updated_at"), 1)));
				s.setFont(GH.smallPlainFont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
				safeAppend(thread, s);
				safeAppend(thread, "\n");
			}
		}
	}

}
