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

	String parent;
	String selectedBranch;
	StringItem branchItem;
	boolean starred;
	StringItem starBtn;

	public RepoForm(String name) {
		super(name);
		this.url = name;
//		addCommand(GH.releasesCmd);
		addCommand(GH.ownerCmd);
//		addCommand(GH.forksCmd);
		addCommand(GH.saveBookmarkCmd);
	}

	void loadInternal(Thread thread) throws Exception {
		JSONObject r = (JSONObject) GH.api("repos/".concat(url));
		
		// cancel check
		if (thread != this.thread) return;
		
		selectedBranch = r.getString("default_branch");
		
		StringItem s;
		String t;
		
		s = new StringItem(null, r.getObject("owner").getString("login"));
		s.setFont(GH.medPlainFont);
		s.setDefaultCommand(GH.userCmd);
		s.setItemCommandListener(GH.midlet);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_BOTTOM | Item.LAYOUT_NEWLINE_BEFORE);
		append(s);
		
		s = new StringItem(null, "/");
		s.setFont(GH.medPlainFont);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_BOTTOM);
		append(s);
		
		s = new StringItem(null, r.getString("name"));
		s.setFont(GH.largePlainFont);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_BOTTOM);
		append(s);
		
		if (r.getBoolean("private", false)) {
			s = new StringItem(null, GH.L[L_private_]);
			s.setFont(GH.smallPlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_BOTTOM);
			append(s);
		}

		if ((t = r.getString("description")) != null && t.length() != 0) {
			s = new StringItem(GH.L[LAbout_Repo], t);
			s.setFont(GH.medPlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			append(s);
		}

		if ((t = r.getString(GH.apiMode == GH.API_GITEA ? "website" : "homepage")) != null && t.length() != 0) {
			s = new StringItem(GH.L[LHomepage_Repo], t);
			s.setFont(GH.smallPlainFont);
			s.setDefaultCommand(GH.linkCmd);
			s.setItemCommandListener(GH.midlet);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			append(s);
		}

		if (GH.apiMode != GH.API_GITEA && !r.isNull("license")
				&& (t = r.getObject("license").getString("name")) != null && t.length() != 0) {
			s = new StringItem(GH.L[LLicense], t);
			s.setFont(GH.smallPlainFont);
			s.setItemCommandListener(GH.midlet);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			append(s);
		}

		if (r.getBoolean("fork", false)) {
			s = new StringItem(null, GH.L[LForkedFrom].concat(parent = r.getObject("parent").getString("full_name")));
			s.setFont(GH.smallPlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			s.setDefaultCommand(GH.forkCmd);
			s.setItemCommandListener(GH.midlet);
			append(s);
		}
		
		s = new StringItem(null, GH.localizePlural(r.getInt(GH.apiMode == GH.API_GITEA ? "stars_count" : "stargazers_count"), L_star),
				Item.BUTTON);
		s.setFont(GH.smallPlainFont);
		s.setDefaultCommand(GH.stargazersCmd);
		s.setItemCommandListener(GH.midlet);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
		append(s);
		
		s = new StringItem(null, GH.localizePlural(r.getInt(GH.apiMode == GH.API_GITEA ? "watchers_count" : "subscribers_count"),
				L_watching), Item.BUTTON);
		s.setFont(GH.smallPlainFont);
		s.setDefaultCommand(GH.watchersCmd);
		s.setItemCommandListener(GH.midlet);
		s.setLayout(Item.LAYOUT_LEFT);
		append(s);
		
		s = new StringItem(null, GH.localizePlural(r.getInt(GH.apiMode == GH.API_GITEA ? "forks_count" : "forks"), L_fork), Item.BUTTON);
		s.setFont(GH.smallPlainFont);
		s.setDefaultCommand(GH.forksCmd);
		s.setItemCommandListener(GH.midlet);
		s.setLayout(Item.LAYOUT_LEFT);
		append(s);

		s = branchItem = new StringItem(GH.L[LBranch], selectedBranch, Item.BUTTON);
		s.setItemCommandListener(GH.midlet);
		s.setDefaultCommand(GH.selectBranchCmd);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		append(s);
		
		s = new StringItem(null, GH.L[LDownloadZIP], Item.BUTTON);
		s.setDefaultCommand(GH.downloadCmd);
		s.setItemCommandListener(GH.midlet);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		append(s);

		s = new StringItem(null, GH.L[LCommits], Item.BUTTON);
		s.setDefaultCommand(GH.commitsCmd);
		s.setItemCommandListener(GH.midlet);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		append(s);
		
		s = new StringItem(null, GH.L[LBrowseSource], Item.BUTTON);
		s.setDefaultCommand(GH.filesCmd);
		s.setItemCommandListener(GH.midlet);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		append(s);

		if (GH.apiMode != GH.API_GITEA) {
			s = new StringItem(null, GH.L[LReadme], Item.BUTTON);
			s.setDefaultCommand(GH.readmeCmd);
			s.setItemCommandListener(GH.midlet);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			append(s);
		}
		
		append("\n");
		
		if (r.getBoolean("has_issues")) {
			s = new StringItem(null, GH.apiMode == GH.API_GITEA ? GH.L[LIssues] : GH.L[LIssues].concat(" (")
				.concat(GH.localizePlural(r.getInt("open_issues"), L_open).concat(")")), Item.BUTTON);
			s.setDefaultCommand(GH.issuesCmd);
			s.setItemCommandListener(GH.midlet);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			append(s);
		}

		if (r.getBoolean("has_pull_requests", true)) {
			s = new StringItem(null, GH.L[LPullRequests], Item.BUTTON);
			s.setDefaultCommand(GH.pullsCmd);
			s.setItemCommandListener(GH.midlet);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			append(s);
		}

		if (r.getBoolean("has_releases", true)) {
			s = new StringItem(null, GH.L[LReleases], Item.BUTTON);
			s.setDefaultCommand(GH.releasesCmd);
			s.setItemCommandListener(GH.midlet);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			append(s);
		}
		
		if (GH.apiMode != GH.API_GITEA) {
			s = new StringItem(null, GH.L[LContributors], Item.BUTTON);
			s.setDefaultCommand(GH.contribsCmd);
			s.setItemCommandListener(GH.midlet);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			append(s);
		}
		
		if (GH.login != null) { // logged in
			s = starBtn = new StringItem(null, GH.L[LStar], Item.BUTTON);
			s.setDefaultCommand(GH.starCmd);
			s.setItemCommandListener(GH.midlet);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			append(s);
			
			starred = false;
			try {
				GH.api("user/starred/".concat(url));
				starred = true;
				s.setText(GH.L[LStarred]);
			} catch (Exception ignored) {}
		}
		
		
	}

}
