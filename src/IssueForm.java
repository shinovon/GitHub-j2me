import java.util.Hashtable;

import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.Spacer;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

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

public class IssueForm extends PagedForm {

	String url;
	JSONObject issue;
	boolean pull;
	
	StringItem commitItem;
	String commitAuthor, commitDate;
	int commitCount;
	
	public IssueForm(String url) {
		super(url);
		this.url = url;
		addCommand(GH.saveBookmarkCmd);
	}

	void loadInternal(Thread thread) throws Exception {
		StringBuffer sb = new StringBuffer("repos/");
		String url = this.url;
		int i;
		if (pull = (i = url.indexOf("/pull/")) != -1) {
			url = url.substring(0, i)
				.concat("/issues/".concat(url.substring(i + 6)));
		}
		sb.append(url);
		
		if (issue == null) {
			issue = (JSONObject) GH.api(sb.toString());
			
			sb.setLength(0);
			sb.append(issue.getString("title")).append(" - ")
			.append(pull ? GH.L[PullRequest] : GH.L[Issue]).append(" #").append(issue.getString("number"));
			setTitle(sb.toString());
		}
		
		if (urls == null) {
			urls = new Hashtable();
		} else {
			urls.clear();
		}
		
		int insert = 0;
		
		if (page == 1) {
			insert = event(thread, issue, sb, 0);
		}

		sb.setLength(0);
		sb.append("repos/").append(url).append("/timeline?");
		
		JSONArray r = pagedApi(thread, sb);
		int l = r.size();

		for (i = 0; i < l && thread == this.thread; ++i) {
			JSONObject j = r.getObject(i);
			insert = event(thread, j, sb, insert);
		}
	}
	
	int event(Thread thread, JSONObject j, StringBuffer sb, int insert) {
		String type = j.getString("event", null);
		
		if (commitItem != null && !"committed".equals(type)) {
			commitItem = null;
		}
		
		StringItem s;
		Spacer sp;
		String t;
		int k;
		if (type == null || "commented".equals(type)) {
			s = new StringItem(null, j.getObject("user").getString("login"));
			s.setFont(GH.smallPlainFont);
			s.setDefaultCommand(GH.userCmd);
			s.setItemCommandListener(GH.midlet);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
			safeInsert(thread, insert++, s);
			
			sb.setLength(0);
			sb.append(GH.L[!pull && type == null ? _opened2 : _commented])
			.append(GH.localizeDate(j.getString("created_at"), 1));
			
			s = new StringItem(null, sb.toString());
			s.setFont(GH.smallPlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
			safeInsert(thread, insert++, s);
			
			sp = new Spacer(10, 4);
			sp.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			safeInsert(thread, insert++, sp);
			
			insert = GH.parseMarkdown(thread, this, j.getString("body"), insert, urls);
			
		} else if ("committed".equals(type)) {
			sb.setLength(0);
			
			JSONObject m;
			t = null;
			if ((m = j.getObject("author")) != null) {
				t = m.getString("name");
			}
			
			if (commitItem != null && commitAuthor != null && commitAuthor.equals(t)) {
				sb.append(t).append(GH.L[_added]).append(GH.count(++commitCount, _commit));
				commitItem.setText(sb.toString());
				commitItem.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
			} else {
				commitAuthor = t;
				commitDate = GH.localizeDate(j.getObject("committer").getString("date"), 1);
				s = new StringItem(null, "");
				s.setFont(GH.smallPlainFont);
				s.setLayout(Item.LAYOUT_LEFT);
				safeInsert(thread, insert++, s);
				
				commitItem = s;
				commitCount = 1;
			}
			
			t = j.getString("message");
			if ((k = t.indexOf('\n')) != -1) t = t.substring(0, k);
			
			s = new StringItem(null, " - ".concat(t));
			s.setFont(GH.smallPlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			safeInsert(thread, insert++, s);
		} else if ("closed".equals(type)) {
			s = new StringItem(null, j.getObject("actor").getString("login"));
			s.setFont(GH.smallPlainFont);
			s.setDefaultCommand(GH.userCmd);
			s.setItemCommandListener(GH.midlet);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
			safeInsert(thread, insert++, s);

			sb.setLength(0);
			sb.append(GH.L[_closedThis])
			.append(GH.localizeDate(j.getString("created_at"), 1));
			
			s = new StringItem(null, sb.toString());
			s.setFont(GH.smallPlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
			safeInsert(thread, insert++, s);
		} else if ("merged".equals(type)) {
			s = new StringItem(null, j.getObject("actor").getString("login"));
			s.setFont(GH.smallPlainFont);
			s.setDefaultCommand(GH.userCmd);
			s.setItemCommandListener(GH.midlet);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
			safeInsert(thread, insert++, s);

			
			sb.setLength(0);
			sb.append(GH.L[_mergedCommit]).append(j.getString("commit_id").substring(0, 7))
			.append(' ').append(GH.localizeDate(j.getString("created_at"), 1));
			
			s = new StringItem(null, sb.toString());
			s.setFont(GH.smallPlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
			safeInsert(thread, insert++, s);
		} else {
			// TODO
			sb.setLength(0);
			if (j.has("actor")) {
				sb.append(j.getObject("actor").getString("login"));
				
				if ((t = j.getString("created_at", null)) != null) {
					sb.append(' ').append(GH.localizeDate(t, 1));
				}
				sb.append('\n');
			}
			sb.append("Undefined event: ").append(type);
			
			s = new StringItem(null, sb.toString());
			s.setFont(GH.smallPlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			safeInsert(thread, insert++, s);
		}
		
		sp = new Spacer(10, 8);
		sp.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		safeInsert(thread, insert++, sp);
		
		return insert;
	}

}
