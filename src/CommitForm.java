import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.Spacer;
import javax.microedition.lcdui.StringItem;

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
public class CommitForm extends GHForm {

	String sha;

	public CommitForm(String repo, String sha) {
		super(sha);
		this.url = repo;
		this.sha = sha;
	}

	void loadInternal(Thread thread) throws Exception {
		// TODO
		StringBuffer sb = new StringBuffer("repos/");
		sb.append(url).append("/commits?sha=").append(sha).append("&per_page=1");

		if (external) addCommand(GH.showRepoCmd);

		JSONObject j = ((JSONArray) GH.api(sb.toString())).getObject(0);
		sha = j.getString("sha");
		JSONObject commit = j.getObject("commit");

		StringItem s;
		String t;

		sb.setLength(0);

		s = new StringItem(null, sb.append("Commit ").append(sha.substring(0, 7)).toString());
		s.setFont(GH.largePlainFont);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		safeAppend(thread, s);

		sb.setLength(0);

		JSONObject m;
		if ((m = j.getObject("author")) != null && !"invalid-email-address".equals(t = m.getString("login"))) {
			sb.append(t);
			sb.append(GH.L[!t.equals(j.getObject("committer").getString("login")) ? L_authored : L_commited]);
		} else if ((m = commit.getObject("author")) != null) {
			sb.append(t = m.getString("name"));
			sb.append(GH.L[!t.equals(commit.getObject("committer").getString("name")) ? L_authored : L_commited]);
		}

		sb.append(GH.localizeDate(commit.getObject("committer").getString("date"), 1));

		s = new StringItem(null, sb.toString());
		s.setFont(GH.smallPlainFont);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
		safeAppend(thread, s);
		safeAppend(thread, new Spacer(10, 8));

		s = new StringItem(null, commit.getString("message"));
		s.setFont(GH.medPlainFont);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		safeAppend(thread, s);
	}

}
