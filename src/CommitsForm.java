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

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class CommitsForm extends PagedForm {

	String url;
	String sha;
	
	public CommitsForm(String repo, String sha) {
		super(repo.concat(" - Commits"));
		this.url = repo;
		this.sha = sha;
	}

	void loadInternal(Thread thread) throws Exception {
		deleteAll();
		
		StringBuffer sb = new StringBuffer("repos/");
		sb.append(url).append("/commits?");
		if (sha != null) sb.append("sha=").append(sha);
		
		JSONArray r = pagedApi(thread, sb.toString());
		int l = r.size();

		StringItem s;
		String t;
		int k;
		for (int i = 0; i < l && thread == this.thread; ++i) {
			JSONObject j = r.getObject(i);
			JSONObject commit = j.getObject("commit");

			t = commit.getString("message");
			if ((k = t.indexOf('\n')) != -1) t = t.substring(0, k);
			
			s = new StringItem(null, t);
			s.setFont(GH.medboldfont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			safeAppend(thread, s);
			
			sb.setLength(0);
			
			if (!"invalid-email-address".equals(t = j.getObject("author").getString("login"))) {
				sb.append(t);
				sb.append(!t.equals(j.getObject("committer").getString("login")) ? " authored " : " commited ");
			} else {
				sb.append(t = commit.getObject("author").getString("name"));
				sb.append(!t.equals(commit.getObject("committer").getString("name")) ? " authored " : " commited ");
			}
			
			sb.append(GH.localizeDate(commit.getObject("committer").getString("date"), 1));
			
			s = new StringItem(null, sb.toString());
			s.setFont(GH.smallfont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
			safeAppend(thread, s);
		}
	}

}
