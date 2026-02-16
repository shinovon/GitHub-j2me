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

public class BranchesForm extends PagedForm {

	RepoForm repoForm;
	
	public BranchesForm(String repo) {
		super(GH.L[LBranches].concat(" - ").concat(repo));
		this.url = repo;
	}

	public BranchesForm(RepoForm d) {
		this(d.url);
		this.repoForm = d;
	}

	void loadInternal(Thread thread) throws Exception {
		JSONArray r = pagedApi(thread, new StringBuffer("repos/").append(url).append("/branches?"));
		int l = r.size();
		
		StringItem s;
		String t;
		for (int i = 0; i < l && thread == this.thread; ++i) {
			JSONObject j = r.getObject(i);
			
			t = j.getString("name");
			if (repoForm != null) {
				s = new StringItem(null, t, Item.BUTTON);
				s.setDefaultCommand(GH.branchItemCmd);
				s.setItemCommandListener(GH.midlet);
				s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			} else {
				s = new StringItem(null, t);
				s.setFont(GH.medPlainFont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			}
			safeAppend(thread, s);
		}
	}

}
