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
import javax.microedition.lcdui.Spacer;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

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
		if (GH.login != null) addCommand(GH.commentCmd);
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
			
			pull = issue.has("pull_request");
			
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
			StringItem s;
			
			s = new StringItem(null, issue.getString("title"));
			s.setFont(GH.largePlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
			safeInsert(thread, insert++, s);
			
			sb.setLength(0);
			sb.append(" #").append(issue.getString("number"));
			
			s = new StringItem(null, sb.toString());
			s.setFont(GH.medPlainFont);
			s.setLayout(Item.LAYOUT_LEFT);
			safeInsert(thread, insert++, s);
			
			s = new StringItem(null, GH.L[pull && !issue.getObject("pull_request").isNull("merged_at") ? _merged_ :
				(!issue.isNull("closed_at") ? _closed_ : (issue.getBoolean("draft", false) ? _draft_ : _open_))]);
			s.setFont(GH.medPlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
			safeInsert(thread, insert++, s);
			
			insert = event(thread, issue, sb, insert);
		}

		// TODO gitea api
		if (GH.apiMode == GH.API_GITEA) return;
		
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
		
		StringItem s;
		Spacer sp;
		String t;
		int i;
		
		if (commitItem != null && !"committed".equals(type)) {
			sp = new Spacer(10, 8);
			sp.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			safeInsert(thread, insert++, sp);
			
			commitItem = null;
		}
		
		if ("mentioned".equals(type) || "subscribed".equals(type)) {
			return insert;
		}

		sb.setLength(0);
		
		if ("committed".equals(type)) {
			JSONObject m;
			t = null;
			if ((m = j.getObject("author")) != null) {
				t = m.getString("name");
			}
			
			if (commitItem != null && commitAuthor != null && commitAuthor.equals(t)) {
				sb.append(t).append(GH.L[_added]).append(GH.localizePlural(++commitCount, _commit));
				commitItem.setText(sb.toString());
				commitItem.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
			} else {
				commitAuthor = t;
				commitDate = GH.localizeDate(j.getObject("committer").getString("date"), 1);

				if (commitItem != null) {
					sb.append(t).append(GH.L[_added]).append(GH.localizePlural(1, _commit));
				}
				s = new StringItem(null, sb.toString());
				s.setFont(GH.smallPlainFont);
				s.setLayout(Item.LAYOUT_LEFT);
				safeInsert(thread, insert++, s);
				
				commitItem = s;
				commitCount = 1;
			}
			
			t = j.getString("message");
			if ((i = t.indexOf('\n')) != -1) t = t.substring(0, i);
			
			s = new StringItem(null, " - ".concat(t));
			s.setFont(GH.smallPlainFont);
			s.setDefaultCommand(GH.mdLinkCmd);
			s.setItemCommandListener(GH.midlet);
			s.setLayout(Item.LAYOUT_LEFT | Item.
					LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			
			urls.put(s, j.getString("html_url"));
			safeInsert(thread, insert++, s);
			
			return insert;
		}
		
		if (j.has("actor") || j.has("user")) {
			s = new StringItem(null, j.getObject(j.has("actor") ? "actor" : "user").getString("login"));
			s.setFont(GH.smallPlainFont);
			s.setDefaultCommand(GH.userCmd);
			s.setItemCommandListener(GH.midlet);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
			safeInsert(thread, insert++, s);
			sb.setLength(0);
		}
		
		if (type == null || "commented".equals(type)) {
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
			sb.setLength(0);
		} else {
			if ("closed".equals(type)) {
				if ((t = j.getString("commit_id", null)) != null) { 
					sb.append(_closedThisCompleted).append(t.substring(0, 7));
				} else {
					sb.append(GH.L["not_planned".equals(j.getString("state_reason", null)) ?
							_closedThisAsNotPlanned : _closedThis]);
				}
			} else if ("merged".equals(type)) {
				sb.append(GH.L[_mergedCommit]).append(j.getString("commit_id").substring(0, 7));
			} else if ("head_ref_deleted".equals(type)) {
				sb.append(GH.L[_deletedBranch]);
			} else if ("cross-referenced".equals(type)) {
				sb.append(GH.L[_mentionedThis]);
			} else if ("labeled".equals(type) || "unlabeled".equals(type)) {
				sb.append(GH.L[type.charAt(0) == 'u' ? _removed : _added]);
			} else if ("referenced".equals(type)) {
				sb.append(GH.L[_mentionedThisIn]).append(j.getString("commit_id").substring(0, 7));
			} else if ("renamed".equals(type)) {
				sb.append(GH.L[_renamedThisTo]).append(j.getObject("renamed").getString("to"));
			} else if ("reopened".equals(type)) {
				sb.append(GH.L[_reopenedThis]);
			} else if ("subscribed".equals(type) || "unsubscribed".equals(type)) {
				sb.append(GH.L[type.charAt(0) == 'u' ? _unsubscribed : _subscribed]);
			} else if ("locked".equals(type) || "unlocked".equals(type)) {
				sb.append(GH.L[type.charAt(0) == 'u' ? _unlockedThis : _lockedThis]);
			} else if ("pinned".equals(type) || "unpinned".equals(type)) {
				sb.append(GH.L[type.charAt(0) == 'u' ? _unpinnedThis : _pinnedThis]);
			} else if ("convert_to_draft".equals(type) || "ready_for_review".equals(type)) {
				sb.append(GH.L[type.charAt(0) == 'c' ? _convertedThisToDraft : _markedThisAsReady]);
			} else if ("assigned".equals(type) || "unassigned".equals(type) ||  "review_requested".equals(type)) {
				boolean r = type.charAt(0) == 'r';
				t = j.getObject(r ? "requested_reviewer" : "assignee").getString("login");
				if (j.getObject("actor").getString("login").equals(t)) {
					sb.append(GH.L[r ? _selfRequestedReview : type.charAt(0) == 'u' ? _unassignedSelf : _selfAssigned]);
				} else {
					s = new StringItem(null, GH.L[r ? _requestedReview : type.charAt(0) == 'u' ? _unassigned : _assigned]);
					s.setFont(GH.smallPlainFont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
					safeInsert(thread, insert++, s);
					
					safeInsert(thread, insert++,
							new Spacer(GH.smallPlainFont.charWidth(' '), GH.smallPlainFont.getBaselinePosition()));
					
					s = new StringItem(null, t);
					s.setFont(GH.smallPlainFont);
					s.setDefaultCommand(GH.userCmd);
					s.setItemCommandListener(GH.midlet);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
					safeInsert(thread, insert++, s);
					
					sb.setLength(0);
				}
			} else if ("reviewed".equals(type)) {
				t = j.getString("state", null);
				sb.append(GH.L["changes_requested".equals(t) ? _requestedChanges : _reviewed])
				.append(GH.localizeDate(j.getString("submitted_at"), 1));
				
				s = new StringItem(null, sb.toString());
				s.setFont(GH.smallPlainFont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
				safeInsert(thread, insert++, s);
				
				sp = new Spacer(10, 4);
				sp.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				safeInsert(thread, insert++, sp);
				
				insert = GH.parseMarkdown(thread, this, j.getString("body"), insert, urls);
				sb.setLength(0);
			} else {
				// TODO https://docs.github.com/en/rest/using-the-rest-api/issue-event-types
				
				// (un)marked_as_duplicate, transferred, review_dismissed,
				// base_ref_changed. head_ref_force_pushed, head_ref_restored.
				// (de)milestoned
				
				sb.append("\nUndefined event: ").append(type);
			}
			if (j.has("created_at")) {
				sb.append(' ').append(GH.localizeDate(j.getString("created_at"), 1));
			}
		}
		
		if (sb.length() != 0) {
			s = new StringItem(null, sb.toString());
			s.setFont(GH.smallPlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
			safeInsert(thread, insert++, s);
		}
		
		if (!"committed".equals(type)) {
			sp = new Spacer(10, 8);
			sp.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			safeInsert(thread, insert++, sp);
		}
			
		return insert;
	}

}
