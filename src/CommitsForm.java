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
import javax.microedition.lcdui.Spacer;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class CommitsForm extends PagedForm implements ItemCommandListener {

	String sha;
	boolean search;
	
	public CommitsForm(String repo, String sha, boolean search) {
		super(search ? GH.L[Search] : GH.L[Commits].concat(" - ").concat(repo));
		this.url = repo;
		this.sha = sha;
		this.search = search;
	}

	void loadInternal(Thread thread) throws Exception {
		StringBuffer sb = new StringBuffer(search ? "search/commits?" : "repos/");
		if (search) {
			GH.appendUrl(sb.append("q="), url);
		} else {
			sb.append(url).append("/commits?");
			if (sha != null) sb.append("sha=").append(sha);
		}
		
		JSONArray r = pagedApi(thread, sb);
		int l = r.size();
		
		if (urls == null) {
			urls = new Hashtable();
		} else {
			urls.clear();
		}
		StringItem s;
		String t;
		int k;
		for (int i = 0; i < l && thread == this.thread; ++i) {
			JSONObject j = r.getObject(i);
			JSONObject commit = j.getObject("commit");

			t = commit.getString("message");
			if ((k = t.indexOf('\n')) != -1) t = t.substring(0, k);
			
			s = new StringItem(null, t);
			s.setFont(GH.medBoldFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			if (j.has("repository")) {
				urls.put(s, j.getObject("repository").getString("full_name")
						.concat("/commit/".concat(j.getString("sha"))));
			}
			if (search) {
				s.addCommand(GH.repoCmd);
			}
			s.setDefaultCommand(GH.mdLinkCmd);
			s.setItemCommandListener(this);
			safeAppend(thread, s);
			
			sb.setLength(0);
			
			JSONObject m;
			if ((m = j.getObject("author")) != null && !"invalid-email-address".equals(t = m.getString("login"))) {
				sb.append(t);
				sb.append(GH.L[!t.equals(j.getObject("committer").getString("login")) ? _authored : _commited]);
			} else if ((m = commit.getObject("author")) != null) {
				sb.append(t = m.getString("name"));
				sb.append(GH.L[!t.equals(commit.getObject("committer").getString("name"))  ? _authored : _commited]);
			}
			
			sb.append(GH.localizeDate(commit.getObject("committer").getString("date"), 1));
			
			s = new StringItem(null, sb.toString());
			s.setFont(GH.smallPlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
			safeAppend(thread, s);
			safeAppend(thread, new Spacer(10, 8));
		}
	}

	public void commandAction(Command c, Item item) {
		String url = (String) urls.get(item);
		if (url == null) return;
		if (c == GH.repoCmd) {
			GH.openRepo(url.substring(0, url.indexOf('/', url.indexOf('/') + 1)));
			return;
		}
//		GH.openUrl(url);
		GH.midlet.browse(GH.GITHUB_URL.concat(url));
	}

}
