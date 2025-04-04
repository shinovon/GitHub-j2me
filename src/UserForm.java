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

public class UserForm extends GHForm {

	public UserForm(String user) {
		super(user.startsWith("/users/") ? user.substring(7) : user);
		this.url = user;
//		addCommand(GH.reposCmd);
		addCommand(GH.saveBookmarkCmd);
	}

	void loadInternal(Thread thread) throws Exception {
		JSONObject r = (JSONObject) GH.api(url);

		// cancel check
		if (thread != this.thread) return;
		
		StringItem s;
		String t;
		
		if ((t = r.getString(GH.apiMode == GH.API_GITEA ? "full_name" : "name")) != null) {
			s = new StringItem(null, t);
			s.setFont(GH.largePlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			append(s);
		}
		
		s = new StringItem(null, url = t = r.getString("login"));
		s.setFont(GH.medPlainFont);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		append(s);
		
		setTitle(t);

		if ((t = r.getString(GH.apiMode == GH.API_GITEA ? "description" : "bio")) != null && t.length() != 0) {
			s = new StringItem(null, t);
			s.setFont(GH.medPlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			append(s);
		}

		if ((t = r.getString(GH.apiMode == GH.API_GITEA ? "website" : "blog")) != null && t.length() != 0) {
			s = new StringItem(GH.L[Blog_User], t);
			s.setFont(GH.medPlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			s.setDefaultCommand(GH.linkCmd);
			s.setItemCommandListener(GH.midlet);
			append(s);
		}

		if ((t = r.getString("location", null)) != null && t.length() != 0) {
			s = new StringItem(GH.L[Location], t);
			s.setFont(GH.medPlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			append(s);
		}
		
		s = new StringItem(null, GH.count(r.getInt(GH.apiMode == GH.API_GITEA ? "followers_count" : "followers"), _follower), Item.BUTTON);
		s.setFont(GH.smallPlainFont);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
		s.setDefaultCommand(GH.followersCmd);
		s.setItemCommandListener(GH.midlet);
		append(s);
		
		s = new StringItem(null, GH.count(r.getInt(GH.apiMode == GH.API_GITEA ? "following_count" : "following"), _following), Item.BUTTON);
		s.setFont(GH.smallPlainFont);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
		s.setDefaultCommand(GH.followingCmd);
		s.setItemCommandListener(GH.midlet);
		append(s);
		
		s = new StringItem(null, GH.apiMode == GH.API_GITEA ? GH.L[Repositories] :
			GH.count(r.getInt("public_repos") + r.getInt("total_private_repos", 0), _repository),
			Item.BUTTON);
		s.setFont(GH.medPlainFont);
		s.setDefaultCommand(GH.reposCmd);
		s.setItemCommandListener(GH.midlet);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		append(s);
		
		s = new StringItem(null, GH.apiMode == GH.API_GITEA ? GH.count(r.getInt("starred_repos_count"), _star) : GH.L[Stars], Item.BUTTON);
		s.setFont(GH.medPlainFont);
		s.setDefaultCommand(GH.starsCmd);
		s.setItemCommandListener(GH.midlet);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		append(s);
	}

}
