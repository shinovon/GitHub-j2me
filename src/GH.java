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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.ServerSocketConnection;
import javax.microedition.io.StreamConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import cc.nnproject.json.JSONStream;

public class GH extends MIDlet implements CommandListener, ItemCommandListener, ItemStateListener, Runnable, LangConstants {
	
	// threading tasks
	static final int RUN_LOAD_FORM = 1;
	static final int RUN_BOOKMARKS_SCREEN = 2;
	static final int RUN_OAUTH_SERVER = 3;
	static final int RUN_OAUTH_SERVER_CLIENT = 4;
	static final int RUN_VALIDATE_AUTH = 5;
	static final int RUN_CHECK_OAUTH_CODE = 6;
	
	// api modes
	static final int API_GITHUB = 0;
	static final int API_GITEA = 1;
	
	// constants
	private static final String SETTINGS_RECORDNAME = "ghsets";
	private static final String BOOKMARKS_RECORDNAME = "ghbm";
	private static final String GITHUB_AUTH_RECORDNAME = "ghauth";
	private static final String GITEA_AUTH_RECORDNAME = "giteaauth";
	
	private static final String GITHUB_API_URL = "https://api.github.com/";
	private static final String GITHUB_API_VERSION = "2022-11-28";
	
	private static final String GITHUB_OAUTH_CLIENT_ID = "Ov23liQSkThpLmVHIxsC";
	private static final String GITHUB_OAUTH_CLIENT_SECRET = "f41d31e17a8cd437e05f6423d5977615a9706505";
	private static final String GITHUB_OAUTH_REDIRECT_URI = "http://localhost:8082/oauth_github";
	private static final String GITHUB_OAUTH_SCOPE = "user, repo";
	
	private static final String GITEA_DEFAULT_API_URL = "https://gitea.com/api/v1/";
	private static final String GITEA_DEFAULT_CLIENT_ID = "4fce904c-9a87-4dfd-ab58-991cd496ac93";
	private static final String GITEA_DEFAULT_CLIENT_SECRET = "gto_qxo2fawh6dugu4u2rbkon6cwnywqfnp6od5ga3kw7opiwkg42xda";
	private static final String GITEA_OAUTH_REDIRECT_URI = "http://localhost:8082/oauth_gitea";
	private static final String GITEA_OAUTH_PORT = "8082";

	// fonts
	static final Font largefont = Font.getFont(0, 0, Font.SIZE_LARGE);
	static final Font medboldfont = Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
	static final Font medfont = Font.getFont(0, 0, Font.SIZE_MEDIUM);
	static final Font smallboldfont = Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_SMALL);
	static final Font smallfont = Font.getFont(0, 0, Font.SIZE_SMALL);

	static final IllegalStateException cancelException = new IllegalStateException("cancel");
	
	private static final byte[] CRLF = {(byte)'\r', (byte)'\n'};
	private static final String OAUTH_REDIRECT_BODY =
		"<html><head><script type=\"text/javascript\">window.close();</script></head><body></body></html>";

	// midp lifecycle
	static Display display;
	static GH midlet;
	static Displayable current;

	private static String version;

	// localization
	static String[] L;
	
	// settings
	private static String proxyUrl = "http://nnp.nnchan.ru/hproxy.php?";
	private static String browseProxyUrl = "http://nnp.nnchan.ru/glype/browse.php?u=";
	private static boolean useProxy = false;
	static int apiMode = API_GITHUB;
	private static String customApiUrl = GITEA_DEFAULT_API_URL;
	private static String lang = "en";

	// threading
	private static int run;
	private static Object runParam;
//	private static int running;
	
	// oauth
	private static Connection oauthSocket;
	private static boolean oauthStarted;
	private static Thread oauthThread;
	
	private static int oauthMode;
	private static String oauthUrl;
	
	private static String githubAccessToken;
	private static long githubAccessTokenTime;

	private static String giteaClientId = GITEA_DEFAULT_CLIENT_ID;
	private static String giteaClientSecret = GITEA_DEFAULT_CLIENT_SECRET;
	private static String giteaAccessToken;
	private static String giteaRefreshToken;
	private static long giteaAccessTokenTime;
	private static long giteaRefreshTokenTime;
	
	static String login;
	
	// bookmarks
	private static JSONArray bookmarks;
	private static int movingBookmark = -1;

	// ui commands
	private static Command exitCmd;
	static Command backCmd;
	private static Command settingsCmd;
	private static Command aboutCmd;
	private static Command bookmarksCmd;
	private static Command searchCmd;
	private static Command searchSubmitCmd;
	private static Command authCmd;
	private static Command yourProfileCmd;
	private static Command yourReposCmd;
	private static Command yourStarsCmd;
	
	private static Command goCmd;
	static Command downloadCmd;
	static Command openCmd;
	static Command linkCmd;
	static Command userCmd;
	static Command spoilerCmd;
	static Command branchItemCmd;
	static Command repoCmd;

	static Command followersCmd;
	static Command followingCmd;
	static Command reposCmd;
	
	static Command ownerCmd;
	static Command releasesCmd;
	static Command tagsCmd;
	static Command forksCmd;
	static Command contribsCmd;
	static Command stargazersCmd;
	static Command watchersCmd;
	static Command forkCmd;
	static Command issuesCmd;
	static Command pullsCmd;
	static Command commitsCmd;
	static Command selectBranchCmd;
	
	static Command nextPageCmd;
	static Command prevPageCmd;
	static Command gotoPageCmd;
	static Command gotoPageOkCmd;
//	static Command firstPageCmd;
//	static Command lastPageCmd;
	
	static Command showOpenCmd;
	static Command showClosedCmd;
	static Command showAllCmd;

	static Command saveBookmarkCmd;

	private static Command addBookmarkCmd;
	private static Command removeBookmarkCmd;
	private static Command moveBookmarkCmd;
	
	private static Command authGithubCmd;
	private static Command authGiteaCmd;
	private static Command logoutGithubCmd;
	private static Command logoutGiteaCmd;
	private static Command authBrowserCmd;
	private static Command authDoneCmd;
	private static Command authRegenCmd;

	static Command okCmd;
	static Command cancelCmd;

	// ui
	private static Form mainForm;
	private static Form settingsForm;
	private static Form searchForm;
	private static List bookmarksList;
	private static Vector formHistory = new Vector();

	// main form items
	private static TextField mainField;

	// settings items
	private static TextField proxyField;
	private static TextField browseProxyField;
	private static ChoiceGroup proxyChoice;
	private static ChoiceGroup modeChoice;
	private static TextField customApiField;
	
	// search items
	private static TextField searchField;
	private static ChoiceGroup searchChoice;
	
	private static TextField authField;

	protected void destroyApp(boolean unconditional) {}

	protected void pauseApp() {}

	protected void startApp() {
		if (midlet != null) return;
		midlet = this;
		
		version = getAppProperty("MIDlet-Version");
		(display = Display.getDisplay(this))
		.setCurrent(current = new Form("gh2me"));
		
		// load settings
		
		try {
			RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORDNAME, false);
			JSONObject j = JSONObject.parseObject(new String(r.getRecord(1), "UTF-8"));
			r.closeRecordStore();
			
			proxyUrl = j.getString("proxy", proxyUrl);
			useProxy = j.getBoolean("useProxy", useProxy);
			browseProxyUrl = j.getString("browseProxy", browseProxyUrl);
			apiMode = j.getInt("apiMode", apiMode);
			customApiUrl = j.getString("customApiUrl", customApiUrl);
			lang = j.getString("lang", lang);
		} catch (Exception ignored) {}

		try {
			RecordStore r = RecordStore.openRecordStore(GITHUB_AUTH_RECORDNAME, false);
			JSONObject j = JSONObject.parseObject(new String(r.getRecord(1), "UTF-8"));
			r.closeRecordStore();

			githubAccessToken = j.getString("accessToken", null);
			githubAccessTokenTime = j.getLong("accessTime", 0);
		} catch (Exception ignored) {}
		
		try {
			RecordStore r = RecordStore.openRecordStore(GITEA_AUTH_RECORDNAME, false);
			JSONObject j = JSONObject.parseObject(new String(r.getRecord(1), "UTF-8"));
			r.closeRecordStore();

			String apiUrl = j.getString("apiUrl", null);
			if (customApiUrl == apiUrl
					|| (apiUrl == null ? GITEA_DEFAULT_API_URL.equals(customApiUrl) : apiUrl.equals(customApiUrl))) {
				giteaAccessToken = j.getString("accessToken", null);
				giteaRefreshToken = j.getString("refreshToken", null);
				giteaClientId = j.getString("clientId", null);
				giteaClientSecret = j.getString("clientSecret", null);
				giteaAccessTokenTime = j.getLong("accessTime", 0);
				giteaRefreshTokenTime = j.getLong("refreshTime", 0);
			}
		} catch (Exception ignored) {}
		
		(L = new String[170])[0] = "gh2me";
		try {
			loadLocale(lang);
		} catch (Exception e) {
			try {
				loadLocale(lang = "en");
			} catch (Exception e2) {
				// crash on fail
				throw new RuntimeException(e2.toString());
			}
		}
		
		if ((apiMode == API_GITHUB && githubAccessToken != null)
				|| (apiMode == API_GITEA && giteaRefreshToken != null)) {
//			start(RUN_VALIDATE_AUTH, null);
			run = RUN_VALIDATE_AUTH;
			run();
		}
		
		// commands
		
		exitCmd = new Command(L[Exit], Command.EXIT, 2);
		backCmd = new Command(L[Back], Command.BACK, 2);
		settingsCmd = new Command(L[Settings], Command.SCREEN, 5);
		aboutCmd = new Command(L[About], Command.SCREEN, 7);
		bookmarksCmd = new Command(L[Bookmarks], Command.ITEM, 1);
		searchCmd = new Command(L[Search], Command.ITEM, 1);
		searchSubmitCmd = new Command(L[Search], Command.OK, 1);
		authCmd = new Command(L[Accounts], Command.SCREEN, 6);
		yourProfileCmd = new Command(L[YourProfile], Command.ITEM, 1);
		yourReposCmd = new Command(L[YourRepositories], Command.ITEM, 1);
		yourStarsCmd = new Command(L[YourStars], Command.ITEM, 1);
		
		goCmd = new Command(L[Go], Command.ITEM, 1);
		downloadCmd = new Command(L[Download], Command.ITEM, 1);
		openCmd = new Command(L[Open], Command.ITEM, 1);
		linkCmd = new Command(L[Open], Command.ITEM, 1);
		userCmd = new Command(L[ViewUser], Command.ITEM, 1);
		spoilerCmd = new Command(L[Show_Spoiler], Command.ITEM, 1);
		branchItemCmd = new Command(L[SelectBranch], Command.ITEM, 1);
		repoCmd = new Command(L[ViewRepository], Command.ITEM, 1);

		followersCmd = new Command(L[Followers], Command.ITEM, 1);
		followingCmd = new Command(L[Following], Command.ITEM, 1);
		reposCmd = new Command(L[Repositories], Command.ITEM, 1);
		
		ownerCmd = new Command(L[Owner], Command.SCREEN, 5);
		releasesCmd = new Command(L[Releases], Command.SCREEN, 3);
		tagsCmd = new Command(L[Tags], Command.SCREEN, 4);
		forksCmd = new Command(L[Forks], Command.ITEM, 1);
		contribsCmd = new Command(L[Contributors], Command.ITEM, 1);
		stargazersCmd = new Command(L[Stargazers], Command.ITEM, 1);
		watchersCmd = new Command(L[Watchers], Command.ITEM, 1);
		forkCmd = new Command(L[OpenParent], Command.ITEM, 1);
		issuesCmd = new Command(L[Issues], Command.ITEM, 1);
		pullsCmd = new Command(L[Pulls], Command.ITEM, 1);
		commitsCmd = new Command(L[Commits], Command.ITEM, 1);
		selectBranchCmd = new Command(L[SelectBranch], Command.ITEM, 1);
		
		showOpenCmd = new Command(L[ShowOpen], Command.SCREEN, 4);
		showClosedCmd = new Command(L[ShowClosed], Command.SCREEN, 5);
		showAllCmd = new Command(L[ShowAll], Command.SCREEN, 6);
		
		nextPageCmd = new Command(L[NextPage], Command.SCREEN, 7);
		prevPageCmd = new Command(L[PrevPage], Command.SCREEN, 8);
		gotoPageCmd = new Command(L[GoToPage_Cmd], Command.SCREEN, 9);
		gotoPageOkCmd = new Command(L[Go], Command.OK, 1);

		saveBookmarkCmd = new Command(L[SaveToBookmarks], Command.SCREEN, 10);

		addBookmarkCmd = new Command(L[New_Bookmark], Command.SCREEN, 5);
		removeBookmarkCmd = new Command(L[Delete], Command.ITEM, 3);
		moveBookmarkCmd = new Command(L[Move], Command.ITEM, 4);
		
		authGithubCmd = new Command("GitHub", Command.ITEM, 1);
		authGiteaCmd = new Command("Gitea", Command.ITEM, 1);
		logoutGithubCmd = new Command(L[Logout], Command.ITEM, 1);
		logoutGiteaCmd = new Command(L[Logout], Command.ITEM, 1);
		authBrowserCmd = new Command(L[OpenInBrowser], Command.ITEM, 1);
		authDoneCmd = new Command(L[Done], Command.ITEM, 1);
		authRegenCmd = new Command(L[Regenerate], Command.ITEM, 1);

		okCmd = new Command(L[Ok], Command.OK, 1);
		cancelCmd = new Command(L[Cancel], Command.CANCEL, 2);
		
		// init main form
		
		Form f = new Form(L[0]);
		f.addCommand(exitCmd);
		f.addCommand(settingsCmd);
		f.addCommand(aboutCmd);
//		f.addCommand(bookmarksCmd);
		f.addCommand(authCmd);
		f.setCommandListener(this);
		f.setItemStateListener(this);
		
		mainField = new TextField(L[URL_Main], "", 200, TextField.NON_PREDICTIVE);
		mainField.addCommand(goCmd);
		mainField.setItemCommandListener(this);
		mainField.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		f.append(mainField);
		
		StringItem s;
		
		s = new StringItem(null, L[Go], StringItem.BUTTON);
		s.setDefaultCommand(goCmd);
		s.setItemCommandListener(this);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		f.append(s);
		
		s = new StringItem(null, L[Search], StringItem.BUTTON);
		s.setDefaultCommand(searchCmd);
		s.setItemCommandListener(this);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		f.append(s);
		
		s = new StringItem(null, L[Bookmarks], StringItem.BUTTON);
		s.setDefaultCommand(bookmarksCmd);
		s.setItemCommandListener(this);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		f.append(s);
		
		if (login != null) { // authorized
			f.append("\n");
			
			s = new StringItem(null, L[YourProfile], StringItem.BUTTON);
			s.setDefaultCommand(yourProfileCmd);
			s.setItemCommandListener(this);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			f.append(s);

			s = new StringItem(null, L[YourRepositories], StringItem.BUTTON);
			s.setDefaultCommand(yourReposCmd);
			s.setItemCommandListener(this);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			f.append(s);

			s = new StringItem(null, L[YourStars], StringItem.BUTTON);
			s.setDefaultCommand(yourStarsCmd);
			s.setItemCommandListener(this);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			f.append(s);
		}
		
		display.setCurrent(current = mainForm = f);
	}

	public void commandAction(Command c, Displayable d) {
		// mainForm commands
		if (d == mainForm) {
			if (c == goCmd) {
				openUrl(mainField.getString().trim().toLowerCase());
				return;
			}
			if (c == settingsCmd) {
				if (settingsForm == null) {
					Form f = new Form(L[Settings]);
					f.addCommand(backCmd);
					f.setCommandListener(this);
					
					modeChoice = new ChoiceGroup(L[Mode_API], ChoiceGroup.POPUP, new String[] {
							"GitHub", "Gitea", /* "GitLab" */
							}, null);
					modeChoice.setSelectedIndex(apiMode, true);
					f.append(modeChoice);
					
					customApiField = new TextField(L[GiteaAPIURL],
							customApiUrl == null ? GITEA_DEFAULT_API_URL : customApiUrl, 200, TextField.URL);
					f.append(customApiField);
					
					proxyField = new TextField(L[APIProxyURL], proxyUrl, 200, TextField.NON_PREDICTIVE);
					f.append(proxyField);
					
					browseProxyField = new TextField(L[BrowserProxyURL], browseProxyUrl, 200, TextField.NON_PREDICTIVE);
					f.append(browseProxyField);
					
					proxyChoice = new ChoiceGroup("", ChoiceGroup.MULTIPLE, new String[] { L[UseProxy] }, null);
					proxyChoice.setSelectedIndex(0, useProxy);
					f.append(proxyChoice);
					
					settingsForm = f;
				}
				display(settingsForm);
				return;
			}
			if (c == aboutCmd) {
				Form f = new Form("About");
				f.addCommand(backCmd);
				f.setCommandListener(this);
				
				StringItem s;
				s = new StringItem(null, "GH2ME v" + version);
				s.setFont(largefont);
				s.setLayout(Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_VCENTER | Item.LAYOUT_LEFT);
				f.append(s);
				
				s = new StringItem(null, L[About_Text]);
				s.setFont(Font.getDefaultFont());
				s.setLayout(Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
				f.append(s);

				s = new StringItem(L[Developer], "shinovon");
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
				f.append(s);

				s = new StringItem("GitHub", "github.com/shinovon");
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
				s.setDefaultCommand(userCmd);
				s.setItemCommandListener(this);
				f.append(s);

				s = new StringItem("Web", "nnproject.cc");
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
				s.setDefaultCommand(linkCmd);
				s.setItemCommandListener(this);
				f.append(s);

				s = new StringItem("Donate", "boosty.to/nnproject/donate");
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
				f.append(s);

				s = new StringItem("Chat", "t.me/nnmidletschat");
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
				f.append(s);
				display(f);
				return;
			}
			if (c == bookmarksCmd) {
				if (bookmarksList != null) {
					display(bookmarksList);
					return;
				}
				display(loadingAlert(L[Loading]));
				start(RUN_BOOKMARKS_SCREEN, null);
				return;
			}
			if (c == searchCmd) {
				if (searchForm == null) {
					Form f = new Form(L[Search]);
					f.addCommand(backCmd);
					f.setCommandListener(this);
					f.setItemStateListener(this);
					
					searchField = new TextField("", "", 200, TextField.ANY);
					f.append(searchField);
					
					searchChoice = new ChoiceGroup(L[Type_Search], Choice.POPUP, new String[] {
							L[Repositories],
							L[Issues],
							L[Users],
							L[Commits],
					}, null);
					f.append(searchChoice);
					
					StringItem s = new StringItem(null, "Search", Item.BUTTON);
					s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.setDefaultCommand(searchCmd);
					s.setItemCommandListener(this);
					f.append(s);
					
					searchForm = f;
				}
				
				searchField.setString(mainField.getString());
				display(searchForm);
				return;
			}
			if (c == authCmd) {
				Form f = new Form(L[Accounts]);
				f.addCommand(backCmd);
				f.setCommandListener(this);
				
				StringItem s;
				
				s = new StringItem(null, "GitHub:");
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				f.append(s);
				
				if (githubAccessToken == null) {
					s = new StringItem(null, L[Authorize], Item.BUTTON);
					s.setDefaultCommand(authGithubCmd);
				} else {
					s = new StringItem(null, L[Logout], Item.BUTTON);
					s.setDefaultCommand(logoutGithubCmd);
				}
				s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.setItemCommandListener(this);
				f.append(s);
				
				s = new StringItem(null, "Gitea:");
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				f.append(s);
				
				if (giteaRefreshToken == null) {
					s = new StringItem(null, L[Authorize], Item.BUTTON);
					s.setDefaultCommand(authGiteaCmd);
				} else {
					s = new StringItem(null, L[Logout], Item.BUTTON);
					s.setDefaultCommand(logoutGiteaCmd);
				}
				s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.setItemCommandListener(this);
				f.append(s);
					
				display(f);
				return;
			}
			a: {
				GHForm f;
				if (c == yourProfileCmd) {
					f = new UserForm("user");
				} else if (c == yourReposCmd) {
					f = new ReposForm("user/repos?", L[YourRepositories], "pushed", false);
				} else if (c == yourStarsCmd) {
					f = new ReposForm("user/stars?", L[YourStars], null, false);
				} else break a;
				display(f);
				start(RUN_LOAD_FORM, f);
				return;
			}
		}
		// settingsForm commands
		if (d == settingsForm) {
			if (c == backCmd) {
				int prevApiMode = apiMode;
				
				// save settings
				proxyUrl = proxyField.getString();
				useProxy = proxyChoice.isSelected(0);
				apiMode = modeChoice.getSelectedIndex();
				if ((customApiUrl = customApiField.getString()).trim().length() == 0)
					customApiUrl = null;
				
				try {
					RecordStore.deleteRecordStore(SETTINGS_RECORDNAME);
				} catch (Exception e) {}
				try {
					JSONObject j = new JSONObject();
					j.put("proxy", proxyUrl);
					j.put("useProxy", useProxy);
					j.put("browseProxy", browseProxyUrl);
					j.put("apiMode", apiMode);
					j.put("customApiUrl", customApiUrl);
					
					byte[] b = j.toString().getBytes("UTF-8");
					RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORDNAME, true);
					r.addRecord(b, 0, b.length);
					r.closeRecordStore();
				} catch (Exception e) {}
				
				if (prevApiMode != apiMode) {
					start(RUN_VALIDATE_AUTH, null);
				} else display(mainForm, true);
				return;
			}
			return;
		}
		// bookmarksList commands
		if (d == bookmarksList && c != backCmd) {
			if (c == addBookmarkCmd) {
				TextBox t = new TextBox(L[URL_Main], "", 100, TextField.NON_PREDICTIVE);
				t.addCommand(okCmd);
				t.addCommand(cancelCmd);
				t.setCommandListener(this);
				display(t);
				return;
			}
			if (c == cancelCmd) {
				// cancel moving
				movingBookmark = -1;
				d.removeCommand(cancelCmd);
				d.addCommand(removeBookmarkCmd);
				d.addCommand(moveBookmarkCmd);
				d.addCommand(backCmd);
				d.addCommand(addBookmarkCmd);
				return;
			}
			int i = ((List) d).getSelectedIndex();
			if (i == -1 || bookmarks == null) return;
			if (c == removeBookmarkCmd) {
				if (movingBookmark != -1) return;
				((List) d).delete(i);
				bookmarks.remove(i);
				return;
			}
			if (c == moveBookmarkCmd) {
				// start moving
				movingBookmark = i;
				d.removeCommand(removeBookmarkCmd);
				d.removeCommand(moveBookmarkCmd);
				d.removeCommand(backCmd);
				d.removeCommand(addBookmarkCmd);
				d.addCommand(cancelCmd);
				return;
			}
			if (c == List.SELECT_COMMAND) {
				// finish moving
				if (movingBookmark != -1) {
					int j = movingBookmark;
					movingBookmark = -1;
					d.removeCommand(cancelCmd);
					d.addCommand(removeBookmarkCmd);
					d.addCommand(moveBookmarkCmd);
					d.addCommand(backCmd);
					d.addCommand(addBookmarkCmd);
					if (i != j) {
						Object bm = bookmarks.get(j);
						bookmarks.remove(i);
						bookmarks.put(i, (String) bm);
						String s = ((List) d).getString(j);
						((List) d).delete(j);
						((List) d).insert(i, s, null);
					}
					return;
				}
				// bookmark selected
				Object bm = bookmarks.get(i);
				if (bm instanceof String) {
					openUrl((String) bm);
				}
				return;
			}
		}
		// searchForm commands
		if (d == searchForm) {
			if (c == searchCmd || c == searchSubmitCmd) {
				int type = searchChoice.getSelectedIndex();
				String q = searchField.getString();
				
				Form f;
				switch (type) {
				case 0: // repositories
					f = new ReposForm((GH.apiMode == GH.API_GITEA ? "repos/search?q=" : "search/repositories?q=").concat(url(q)), 
							"Search", /*GH.apiMode == GH.API_GITEA ? "updated" : */null, true);
					break;
				case 1: // issues
					f = new IssuesForm(q, 2);
					break;
				case 2: // users
					f = new UsersForm((GH.apiMode == GH.API_GITEA ? "users/search?q=" : "search/users?q=").concat(url(q)),
							"Search");
					break;
				case 3: // commits
					if (GH.apiMode == GH.API_GITEA) return;
					f = new CommitsForm(q, null, true);
					break;
				default:
					return;
				}
				display(f);
				start(RUN_LOAD_FORM, f);
				return;
			}
		}
		// Auth commands
		if (c == authGithubCmd || c == authGiteaCmd) {
			String url = oauthUrl = getOauthUrl(oauthMode = c == authGithubCmd ? 0 : 1);
			
			Form f = new Form(L[Authorization]);
			f.addCommand(backCmd);
			f.setCommandListener(this);

			StringItem s;
			
			TextField t = new TextField("OAuth URL", url, 400, TextField.URL | TextField.UNEDITABLE);
			t.addCommand(authRegenCmd);
			f.append(t);


			s = new StringItem(null, "Copy this address to supported browser");
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setFont(smallfont);
			f.append(s);
			
			
			s = new StringItem(null, L[OpenInBrowser], Item.BUTTON);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(authBrowserCmd);
			s.setItemCommandListener(this);
			f.append(s);
			
			f.append("\n");
			
			authField = new TextField("Result URL", "", 400, TextField.URL);
			f.append(authField);

			s = new StringItem(null, "After authorization, copy resulted URL with code to this field");
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setFont(smallfont);
			f.append(s);
			
			f.append("\n");
			
			s = new StringItem(null, L[Done], Item.BUTTON);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(authDoneCmd);
			s.setItemCommandListener(this);
			f.append(s);
			
			display(f);
			return;
		}
		if (c == logoutGithubCmd) {
			githubAccessToken = null;
			writeGithubAuth();
			commandAction(backCmd, d);
			return;
		}
		if (c == logoutGiteaCmd) {
			giteaRefreshToken = giteaAccessToken = null;
			writeGiteaAuth();
			commandAction(backCmd, d);
			return;
		}
		if (c == authBrowserCmd) {
			if (!oauthStarted) {
				oauthThread = start(RUN_OAUTH_SERVER, null);
			}
			try {
				platformRequest(oauthUrl);
			} catch (Exception e) {
				display(errorAlert(e.toString()), d);
				e.printStackTrace();
				stopOauthServer();
			}
			return;
		}
		if (c == authDoneCmd) {
			if (oauthMode < 0) return; // TODO double click check
			start(RUN_CHECK_OAUTH_CODE, authField.getString().trim());
			return;
		}
		// RepoForm commands
		if (d instanceof RepoForm) {
			if (c == ownerCmd) {
				String url = ((RepoForm) d).url;
				openUser(url.substring(0, url.indexOf('/')));
				return;
			}
			if (c == downloadCmd) {
				String inst;
				if (apiMode == API_GITHUB) {
					inst = GITHUB_API_URL;
				} else if (customApiUrl != null) {
					inst = customApiUrl;
				} else if (apiMode == API_GITEA) {
					inst = GITEA_DEFAULT_API_URL;
				} else return;
				browse(inst.concat("repos/").concat(((RepoForm) d).url)
						.concat(apiMode == API_GITEA ? "/archive/" : "/zipball/")
						.concat(((RepoForm) d).selectedBranch).concat(apiMode == API_GITEA ? ".zip" : ""));
				return;
			}
			if (c == forkCmd) {
				openRepo(((RepoForm) d).parent);
				return;
			}
			a: {
				String url = ((RepoForm) d).url;
				Form f;
				if (c == releasesCmd) {
					f = new ReleasesForm(url, false);
				} else if (c == tagsCmd) {
					f = new ReleasesForm(url, true);
				} else if (c == forksCmd) {
					f = new ReposForm("repos/".concat(url).concat("/forks?"), L[Forks].concat(" - ").concat(url), null, true);
				} else if (c == contribsCmd) {
					f = new UsersForm("repos/".concat(url).concat("/contributors?"), L[Contributors].concat(" - ").concat(url));
				} else if (c == stargazersCmd) {
					 f = new UsersForm("repos/".concat(url).concat("/stargazers?"), L[Stargazers].concat(" - ").concat(url));
				} else if (c == watchersCmd) {
					f = new UsersForm("repos/".concat(url).concat("/subscribers?"), L[Watchers].concat(" - ").concat(url));
				} else if (c == issuesCmd) {
					f = new IssuesForm(url, 0);
				} else if (c == pullsCmd) {
					f = new IssuesForm(url, 1);
				} else if (c == commitsCmd) {
					f = new CommitsForm(url, ((RepoForm) d).selectedBranch, false);
				} else if (c == selectBranchCmd) {
					f = new BranchesForm((RepoForm) d);
				} else break a;
	
				display(f);
				start(RUN_LOAD_FORM, f);
				return;
			}
		}
		// UserForm commands
		if (d instanceof UserForm) {
			if (c == reposCmd) {
				String url = ((UserForm) d).url;
				ReposForm f = new ReposForm("users/".concat(url).concat("/repos?"),
						L[Repositories].concat(" - ").concat(url), GH.apiMode == GH.API_GITEA ? "updated" : "pushed", false);
				display(f);
				start(RUN_LOAD_FORM, f);
				return;
			}
			boolean b;
			if ((b = c == followersCmd) || c == followingCmd) {
				String url = ((UserForm) d).url;
				UsersForm f = new UsersForm(
						"users/".concat(url).concat(b ? "/followers?" : "/following?"),
						L[b ? Followers : Following].concat(" - ").concat(url)
						);
				display(f);
				start(RUN_LOAD_FORM, f);
				return;
			}
		}
		// ReleasesForm commands
		if (d instanceof ReleasesForm) {
			if (c == releasesCmd || c == tagsCmd) {
				((ReleasesForm) d).toggleMode();
				return;
			}
		}
		// PagedForm commands
		if (d instanceof PagedForm) {
			boolean b;
			if ((b = c == nextPageCmd) || c == prevPageCmd) {
				((PagedForm) d).changePage(b);
				return;
			}
			if (c == gotoPageCmd) {
				TextBox t = new TextBox(L[GoToPage].concat(((PagedForm) d).pageText), "", 10, TextField.NUMERIC);
				t.addCommand(gotoPageOkCmd);
				t.addCommand(cancelCmd);
				t.setCommandListener(this);
				display(t);
				return;
			}
//			if (c == firstPageCmd) {
//				((PagedForm) d).gotoPage(1);
//			}
		}
		// IssuesForm commands
		if (d instanceof IssuesForm) {
			if (c == showOpenCmd || c == showClosedCmd || c == showAllCmd) {
				((IssuesForm) d).state = c == showOpenCmd ? "open" : c == showClosedCmd ? "closed" : "all";
				((IssuesForm) d).cancel();
				GH.midlet.start(GH.RUN_LOAD_FORM, d);
				return;
			}
		}
		// TextBox commands
		if (d instanceof TextBox) {
			if (c == gotoPageOkCmd) { // go to page dialog confirm
				commandAction(backCmd, d);
				((PagedForm) current).gotoPage(Integer.parseInt(((TextBox) d).getString()));
				return;
			}
			if (c == okCmd) { // bookmark save confirm
				commandAction(backCmd, d);
				addBookmark(((TextBox) d).getString().trim().toLowerCase(), current);
				return;
			}
		}
		if (c == saveBookmarkCmd) {
			String s;
			if (d instanceof RepoForm) {
				s = ((RepoForm) d).url;
			} else if (d instanceof UserForm) {
				s = ((UserForm) d).url;
			} else if (d instanceof ReleasesForm) {
				s = ((ReleasesForm) d).url.concat("/releases");
			} else if (d instanceof IssuesForm) {
				s = ((IssuesForm) d).url.concat(((IssuesForm) d).mode == 1 ? "/pulls" : "/issues");
			} else if (d instanceof CommitsForm) {
				s = ((CommitsForm) d).url.concat("/commits");
			} else if (d instanceof IssueForm) {
				s = ((IssueForm) d).url;
			} else return;
			
			addBookmark(s, d);
			return;
		}
		if (c == backCmd || c == cancelCmd) {
			if (formHistory.size() == 0) {
				display(mainForm, true);
				return;
			}
			Displayable p = null;
			synchronized (formHistory) {
				int i = formHistory.size();
				while (i-- != 0) {
					if (formHistory.elementAt(i) == d) {
						break;
					}
				}
				if (i > 0) {
					p = (Displayable) formHistory.elementAt(i - 1);
					formHistory.removeElementAt(i);
				}
			}
			display(p, true);
			return;
		}
		if (c == exitCmd) {
			notifyDestroyed();
		}
	}

	public void commandAction(Command c, Item item) {
		if (c == linkCmd) {
			browse(((StringItem) item).getText());
			return;
		}
		if (c == userCmd) {
			String url = ((StringItem) item).getText();
			if (url.startsWith("github.com/")) {
				url = url.substring(11);
			}
			int i;
			if ((i = url.indexOf('/')) != -1) {
				url = url.substring(0, i);
			}
			
			openUser(url);
			return;
		}
		if (c == GH.branchItemCmd) {
			((BranchesForm) current).repoForm.branchItem
			.setText(((BranchesForm) current).repoForm.selectedBranch = ((StringItem) item).getText());
			
			commandAction(backCmd, current);
			return;
		}
		if (c == authRegenCmd) {
			((TextField) item).setString(oauthUrl = getOauthUrl(oauthMode));
			return;
		}
		commandAction(c, display.getCurrent());
	}

	public void itemStateChanged(Item item) {
		if (item == mainField) {
			String t;
			if ((t = ((TextField) item).getString()).endsWith("\n")) {
				commandAction(goCmd, item);
				((TextField) item).setString(t.trim());
			}
			return;
		}
		if (item == searchField) {
			String t;
			if ((t = ((TextField) item).getString()).endsWith("\n")) {
				commandAction(searchCmd, item);
				((TextField) item).setString(t.trim());
			}
			return;
		}
	}
	
	static void openUrl(String url) {
//		System.out.println("openUrl:".concat(url));
		if (url.startsWith("https://github.com/")) {
			url = url.substring(19);
		} else if (url.startsWith("https://api.github.com/repos/")
				|| url.startsWith("https://api.github.com/users/")) {
			url = url.substring(29);
		}
		if (url.indexOf('/') == -1) {
			// user
			openUser(url);
		} else {
			// repo
			String[] split = split(url, '/');
			if (split.length == 2 || split[2].length() == 0) {
				openRepo(url);
			} else {
				String repo = split[0].concat("/").concat(split[1]);
				GHForm f;
				char c;
				switch (c = split[2].charAt(0)) {
				case 'f': // forks
					f = new ReposForm("repos/".concat(repo).concat("/forks?"), L[Forks].concat(" - ").concat(repo), null, true);
					break;
				case 'r': // releases
					f = new ReleasesForm(repo, false);
					break;
				case 't': // tags
					f = new ReleasesForm(repo, true);
					break;
				case 's': // stargazers
					f = new UsersForm("repos/".concat(repo).concat("/stargazers?"), L[Stargazers].concat(" - ").concat(repo));
					break;
				case 'w': // watchers
					f = new UsersForm("repos/".concat(repo).concat("/subscribers?"), L[Watchers].concat(" - ").concat(repo));
					break;
				case 'i': // issues
				case 'p': // pulls
					if (split.length == 4) {
						f = new IssueForm(url);
						break;
					}
					f = new IssuesForm(repo, c == 'p' ? 1 : 0);
					break;
				case 'c': // commits
					if (split.length == 4) {
						f = new CommitsForm(repo, split[3], false);
						break;
					}
					f = new CommitsForm(repo, null, false);
					break;
				case 'b': // branches
					f = new BranchesForm(repo);
					break;
				default:
					return;
				}
				display(f);
				midlet.start(RUN_LOAD_FORM, f);
			}
		}
	}
	
	static void openRepo(String url) {
		RepoForm f = new RepoForm(url);
		display(f);
		midlet.start(RUN_LOAD_FORM, f);
	}
	
	static void openUser(String url) {
		url = "users/".concat(url);
		
		UserForm f = null;
		// search in previous screens
		synchronized (formHistory) {
			int l = formHistory.size();
			for (int i = 0; i < l; ++i) {
				Object o = formHistory.elementAt(i);
				if (!(o instanceof UserForm) || !url.equals(((UserForm) o).url)) {
					break;
				}
				f = (UserForm) o;
			}
		}
		if (f == null) {
			f = new UserForm(url);
		}
		display(f);
		midlet.start(RUN_LOAD_FORM, f);
	}
	
	static void addBookmark(String url, Displayable d) {
		if (bookmarks == null) {
			try {
				RecordStore r = RecordStore.openRecordStore(BOOKMARKS_RECORDNAME, false);
				bookmarks = JSONObject.parseArray(new String(r.getRecord(1), "UTF-8"));
				r.closeRecordStore();
			} catch (Exception e) {
				bookmarks = new JSONArray(10);
			}
		} else {
			// check if this bookmark already exists
			if (bookmarks.has(url)) return;
		}
		bookmarks.add(url);
		if (bookmarksList != null) {
			bookmarksList.append(url, null);
		}
		
		try {
			RecordStore.deleteRecordStore(BOOKMARKS_RECORDNAME);
		} catch (Exception ignored) {}
		try {
			RecordStore r = RecordStore.openRecordStore(BOOKMARKS_RECORDNAME, true);
			byte[] b = bookmarks.toString().getBytes("UTF-8");
			r.addRecord(b, 0, b.length);
			r.closeRecordStore();
		} catch (Exception ignored) {}
		
		display(infoAlert("Bookmark saved"), d);
	}
	
	// threading
	public void run() {
		int run;
		Object param;
		synchronized (this) {
			run = GH.run;
			param = GH.runParam;
			notify();
		}
		System.out.println("run " + run + " " + param);
//		running++;
		switch (run) {
		case RUN_LOAD_FORM: { // load GHForm contents
			((GHForm) param).load();
			break;
		}
		case RUN_BOOKMARKS_SCREEN: { // load bookmarks
			if (bookmarksList == null) {
				List list = new List("Bookmarks", List.IMPLICIT);
				list.setFitPolicy(Choice.TEXT_WRAP_ON);
				list.addCommand(backCmd);
				list.addCommand(List.SELECT_COMMAND);
				list.addCommand(removeBookmarkCmd);
				list.addCommand(moveBookmarkCmd);
				list.addCommand(addBookmarkCmd);
				list.setCommandListener(this);
				try {
					if (bookmarks == null) {
						RecordStore r = RecordStore.openRecordStore(BOOKMARKS_RECORDNAME, false);
						bookmarks = JSONObject.parseArray(new String(r.getRecord(1), "UTF-8"));
						r.closeRecordStore();
					}
					int l = bookmarks.size();
					for (int i = 0; i < l; i++) {
						Object bm = bookmarks.get(i);
						list.append(bm.toString(), null);
					}
				} catch (Exception e) {}
				bookmarksList = list;
			}
			display(bookmarksList);
			break;
		}
		case RUN_OAUTH_SERVER: { // start http server for oauth callback
			try {
				oauthSocket = Connector.open("socket://:".concat(GITEA_OAUTH_PORT));
				oauthStarted = true;
				try {
					while (oauthThread != null) {
						start(RUN_OAUTH_SERVER_CLIENT, ((ServerSocketConnection) oauthSocket).acceptAndOpen());
					}
				} finally {
					oauthSocket.close();
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
			oauthStarted = false;
			break;
		}
		case RUN_OAUTH_SERVER_CLIENT: { // handle http request
			StreamConnection s = (StreamConnection) param;
			try {
				InputStream in = s.openDataInputStream();
				OutputStream out = s.openDataOutputStream();
				
				Vector headers = new Vector();
				StringBuffer sb = new StringBuffer();
				
				boolean responded = false;
				try {
					for (int c, l = 0; (c = in.read()) != -1;) {
						if (c == '\n') {
//									sb.append((char) c);
							headers.addElement(sb.toString());
							sb.setLength(0);
							if (++l == 2) break;
							continue;
						}
						if (c != '\r') {
							l = 0;
							sb.append((char) c);
						}
					}
					
					boolean handled = false;
					try {
						res: {
							String req;
							if (headers.size() == 0 ||
									(req = (String) headers.elementAt(0)).length() == 0) {
								break res;
							}
							int i = req.indexOf(' ');
							if (i == -1 || req.indexOf(' ', i + 1) == -1)
								break res;
							
							String method = req.substring(0, i),
									path = req.substring(i + 1, req.indexOf(' ', i + 1)),
									protocol = req.substring(req.indexOf(' ', i + 1) + 1);
							if (!protocol.startsWith("HTTP/1.") ||
									!path.startsWith("/")) {
								break res;
							}
							
							if (!"GET".equals(method) || path.length() == 1) {
								handled = true;
								responded = true;
								writeHttpHeader(out, 404, "Not Found");
								out.write(CRLF);
								break res;
							}
							String query = null;
							if ((i = path.indexOf('?')) != -1) {
								query = path.substring(i + 1);
								path = path.substring(0, i);
							}
							
							boolean b;
							if ((b = "/oauth_github".equals(path)) || "/oauth_gitea".equals(path)) {
								handled = true;
								oauthMode = -oauthMode;
								display(current);
								b = acceptOauthToken(query, b ? 0 : 1);
								
								responded = true;
								writeHttpHeader(out, 200, "OK");
								out.write("Content-Type: text/html; charset=utf8".getBytes());
								out.write(CRLF);
								out.write(CRLF);
								out.write(OAUTH_REDIRECT_BODY.getBytes("UTF-8"));
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						if (!responded) {
							responded = true;
							writeHttpHeader(out, 500, "Internal Server Error");
							out.write(CRLF);
							out.write(e.toString().getBytes("UTF-8"));
						}
					}
					if (!handled && !responded) {
						writeHttpHeader(out, 400, "Bad Request");
						out.write(CRLF);
					}
					out.flush();
				} finally {
					in.close();
					out.close();
				}
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				try {
					s.close();
				} catch (Exception ignored) {}
			}
			break;
		}
		case RUN_VALIDATE_AUTH: { // validate and refresh auth
			display(loadingAlert(L[Authorizing]), null);
			login = null;
			
			if (apiMode == API_GITHUB && githubAccessToken != null) {
				try {
					login = ((JSONObject) api("user")).getString("login");
				} catch (Exception e) {
					// token revoked
					githubAccessToken = null;
					writeGithubAuth();
				}
				break;
			}
			if (apiMode == API_GITEA && giteaRefreshToken != null) {
				if (giteaAccessToken != null && System.currentTimeMillis() - giteaAccessTokenTime > 3600000L) {
					giteaAccessToken = null;
				}
				if (giteaAccessToken != null) {
					try {
						login = ((JSONObject) api("user")).getString("login");
						break;
					} catch (Exception e) {
						// token revoked
						giteaAccessToken = null;
					}
				}
				
				try {
					JSONObject j = new JSONObject();
					j.put("grant_type", "refresh_token");
					j.put("refresh_token", giteaRefreshToken);
					j.put("client_id", giteaClientId);
					j.put("client_secret", giteaClientSecret);

					String inst = customApiUrl != null ? customApiUrl : GITEA_DEFAULT_API_URL;
					inst = inst.substring(0, inst.indexOf("/api"));
					
					j = (JSONObject) apiPost(inst.concat("/login/oauth/access_token"),
							j.toString().getBytes(), "application/json");
					
					giteaAccessToken = j.getString("access_token");
					giteaRefreshToken = j.getString("refresh_token", giteaRefreshToken);
					
					giteaRefreshTokenTime = giteaAccessTokenTime = System.currentTimeMillis();
				} catch (Exception e) {
					// token revoked
					giteaRefreshToken = null;
				}
				display(/*current*/ mainForm, true);
				writeGiteaAuth();
				break;
			}
			break;
		}
		case RUN_CHECK_OAUTH_CODE: {
			acceptOauthToken((String) param, oauthMode);
			break;
		}
		}
//		running--;
	}

	// start task thread
	Thread start(int i, Object param) {
		Thread t = null;
		try {
			synchronized (this) {
				run = i;
				runParam = param;
				(t = new Thread(this)).start();
				wait();
			}
		} catch (Exception e) {}
		return t;
	}

	private static void writeHttpHeader(OutputStream out, int code, String message) throws IOException {
		out.write("HTTP/1.1 ".getBytes());
		out.write(Integer.toString(code).getBytes());
		out.write(' ');
		if (message != null) out.write(message.getBytes());
		out.write(CRLF);
		out.write("Connection: close".getBytes());
		out.write(CRLF);
	}
	
	private static String getOauthUrl(int mode) {
		StringBuffer sb = new StringBuffer();
		Random rng = new Random();
		for (int i = 0; i < 16; i++) {
			byte b = (byte) (rng.nextInt() & 0xff);
			sb.append(Integer.toHexString(b >> 4 & 0xf));
			sb.append(Integer.toHexString(b & 0xf));
		}
		
		String state = sb.toString();
		sb.setLength(0);
		
		if (mode == API_GITHUB) {
			sb.append("https://github.com/login/oauth/authorize?client_id=")
			.append(GITHUB_OAUTH_CLIENT_ID).append("&scope=").append(url(GITHUB_OAUTH_SCOPE))
			.append("&state=").append(state)
			.append("&redirect_uri=").append(url(GITHUB_OAUTH_REDIRECT_URI))
			;
		} else if (mode == API_GITEA) {
			String inst = customApiUrl != null ? customApiUrl : GITEA_DEFAULT_API_URL;
			inst = inst.substring(0, inst.indexOf("/api"));
			sb.append(inst)
			.append("/login/oauth/authorize?client_id=").append(giteaClientId)
			.append("&response_type=code&state=").append(state)
			.append("&redirect_uri=").append(url(GITEA_OAUTH_REDIRECT_URI))
			;
		}
		return sb.toString();
	}
	
	private static boolean acceptOauthToken(String query, int mode) {
		Displayable d = current;
		
		String code = null;
		int i = query.indexOf("code=");
		if (i == -1) {
			if (query.indexOf('=') != -1) {
				display(errorAlert(query.indexOf("error") != -1 ? query : "Invalid url"), d);
				return false;
			} else code = query;
		}
		
		display(loadingAlert(L[Authorizing]), d);
		
		try {
			if (code == null)
				code = query.substring(i + 5, (i = query.indexOf('&', i + 5)) != -1 ? i : query.length());
	
			JSONObject j = new JSONObject();
			j.put("code", code);
			j.put("grant_type", "authorization_code");
			
			if (mode == 0) { // github
				j.put("client_id", GITHUB_OAUTH_CLIENT_ID);
				j.put("redirect_uri", GITHUB_OAUTH_REDIRECT_URI);
				j.put("client_secret", GITHUB_OAUTH_CLIENT_SECRET);
				
				j = (JSONObject) apiPost("https://github.com/login/oauth/access_token",
						j.toString().getBytes(), "application/json");
				
				githubAccessToken = j.getString("access_token");
				
				githubAccessTokenTime = System.currentTimeMillis();
				
				writeGithubAuth();
			} else if (mode == 1) { // gitea
				j.put("client_id", giteaClientId);
				j.put("redirect_uri", GITEA_OAUTH_REDIRECT_URI);
				j.put("client_secret", giteaClientSecret);

				String inst = customApiUrl != null ? customApiUrl : GITEA_DEFAULT_API_URL;
				inst = inst.substring(0, inst.indexOf("/api"));
				
				j = (JSONObject) apiPost(inst.concat("/login/oauth/access_token"),
						j.toString().getBytes(), "application/json");
				
				giteaAccessToken = j.getString("access_token");
				giteaRefreshToken = j.getString("refresh_token");
				
				giteaRefreshTokenTime = giteaAccessTokenTime = System.currentTimeMillis();
				
				writeGiteaAuth();
			}
			display(mainForm, true);
			display(infoAlert("Authorized"), mainForm);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			display(errorAlert(e.toString()), d);
		}
		return false;
	}
	
	private static void stopOauthServer() {
		if (!oauthStarted || oauthThread == null) return;
		if (oauthSocket != null) {
			try {
				oauthSocket.close();
			} catch (Exception e) {}
		}
		oauthThread.interrupt();
		oauthThread = null;
	}
	
	private static void writeGithubAuth() {
		try {
			RecordStore.deleteRecordStore(GITHUB_AUTH_RECORDNAME);
		} catch (Exception e) {}
		try {
			JSONObject j = new JSONObject();
			
			j.put("accessToken", githubAccessToken);
			j.put("accessTime", githubAccessTokenTime);
			
			byte[] b = j.toString().getBytes("UTF-8");
			RecordStore r = RecordStore.openRecordStore(GITHUB_AUTH_RECORDNAME, true);
			r.addRecord(b, 0, b.length);
			r.closeRecordStore();
		} catch (Exception e) {}
	}
	
	private static void writeGiteaAuth() {
		try {
			RecordStore.deleteRecordStore(GITEA_AUTH_RECORDNAME);
		} catch (Exception e) {}
		try {
			JSONObject j = new JSONObject();

			j.put("apiUrl", customApiUrl);
			j.put("accessToken", giteaAccessToken);
			j.put("refreshToken", giteaRefreshToken);
			j.put("clientId", giteaClientId);
			j.put("clientSecret", giteaClientSecret);
			j.put("accessTime", giteaAccessTokenTime);
			j.put("refreshTime", giteaRefreshTokenTime);
			
			byte[] b = j.toString().getBytes("UTF-8");
			RecordStore r = RecordStore.openRecordStore(GITEA_AUTH_RECORDNAME, true);
			r.addRecord(b, 0, b.length);
			r.closeRecordStore();
		} catch (Exception e) {}
	}

	public static void parseMarkdown(Thread thread, GHForm form, String body, int i) {
		// TODO
		if (body.trim().length() == 0) return;
		
		StringItem s = new StringItem(null, body);
		s.setFont(GH.medfont);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		
		if (i == -1) form.safeAppend(thread, s);
		else form.safeInsert(thread, i, s);
	}
	
	private void loadLocale(String lang) throws IOException {
		InputStreamReader r = new InputStreamReader(getClass().getResourceAsStream("/l/" + lang), "UTF-8");
		StringBuffer s = new StringBuffer();
		int c;
		int i = 1;
		while ((c = r.read()) > 0) {
			if (c == '\r') continue;
			if (c == '\\') {
				s.append((c = r.read()) == 'n' ? '\n' : (char) c);
				continue;
			}
			if (c == '\n') {
				L[i++] = s.toString();
				s.setLength(0);
				continue;
			}
			s.append((char) c);
		}
		r.close();
	}
	
	static void display(Alert a, Displayable d) {
		if (d == null) {
			display.setCurrent(a);
			return;
		}
		if (display.getCurrent() != d) {
			display(d);
		}
		display.setCurrent(a, d);
	}
	
	static void display(Displayable d) {
		display(d, false);
	}

	static void display(Displayable d, boolean back) {
		if (d instanceof Alert) {
			display.setCurrent((Alert) d, mainForm);
			return;
		}
		if (d == null) {
			d = mainForm;
		}
		if (d == mainForm) {
			formHistory.removeAllElements();
			if (oauthStarted) {
				stopOauthServer();
			}
		}
		Displayable p = display.getCurrent();
		display.setCurrent(current = d);
		if (p == null || p == d) return;
		
		if (p instanceof GHForm) {
			((GHForm) p).closed(back);
		}
		// push to history
		if (!back && d != mainForm && (formHistory.isEmpty() || formHistory.lastElement() != d)) {
			formHistory.addElement(d);
		}
	}

	static Alert errorAlert(String text) {
		Alert a = new Alert("");
		a.setType(AlertType.ERROR);
		a.setString(text);
		a.setTimeout(3000);
		return a;
	}
	
	private static Alert infoAlert(String text) {
		Alert a = new Alert("");
		a.setType(AlertType.CONFIRMATION);
		a.setString(text);
		a.setTimeout(1500);
		return a;
	}
	
	private static Alert loadingAlert(String s) {
		Alert a = new Alert("", s, null, null);
		a.setCommandListener(midlet);
		a.addCommand(Alert.DISMISS_COMMAND);
		a.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
		a.setTimeout(Alert.FOREVER);
		return a;
	}

	void browse(String url) {
		try {
			if (url.indexOf(':') == -1) {
				url = "http://".concat(url);
			} else if (useProxy && (url.startsWith("https://github.com") || url.startsWith(GITHUB_API_URL))) {
				url = browseProxyUrl.concat(url(url));
			}
			if (platformRequest(url)) notifyDestroyed();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static Object api(String url) throws IOException {
		return api(url, null);
	}
	
	static Object api(String url, String[] linkPtr) throws IOException {
		Object res;

		HttpConnection hc = null;
		InputStream in = null;
		try {
			String inst;
			if (apiMode == API_GITHUB) {
				inst = GITHUB_API_URL;
			} else if (customApiUrl != null) {
				inst = customApiUrl;
			} else if (apiMode == API_GITEA) {
				inst = GITEA_DEFAULT_API_URL;
			} else {
				throw new IllegalStateException("Invalid API mode");
			}
			hc = openHttpConnection(proxyUrl(inst.concat(url)));
			hc.setRequestMethod("GET");
			if (apiMode == API_GITHUB) {
				hc.setRequestProperty("Accept", "application/vnd.github+json");
				hc.setRequestProperty("X-Github-Api-Version", GITHUB_API_VERSION);
				if (githubAccessToken != null)
					hc.setRequestProperty("Authorization", "Bearer ".concat(githubAccessToken));
			} else {
				hc.setRequestProperty("Accept", "application/json");
				if (giteaAccessToken != null)
					hc.setRequestProperty("Authorization", "Bearer ".concat(giteaAccessToken));
			}
			
			int c = hc.getResponseCode();
			try {
				res = JSONStream.getStream(in = hc.openInputStream()).nextValue();
//				res = JSONObject.parseJSON(readUtf(in = hc.openInputStream(), (int) hc.getLength()));
			} catch (RuntimeException e) {
				if (c >= 400) {
					throw new APIException(url, c, null);
				} else throw e;
			}
			if (c >= 400) {
				throw new APIException(url, c, res);
			}
			if (linkPtr != null) {
				linkPtr[0] = hc.getHeaderField("link");
			}
		} finally {
			if (in != null) try {
				in.close();
			} catch (IOException e) {}
			if (hc != null) try {
				hc.close();
			} catch (IOException e) {}
		}
		// FIXME debug
		System.out.println(res instanceof JSONObject ?
				((JSONObject) res).format(0) : res instanceof JSONArray ?
						((JSONArray) res).format(0) : res);
		return res;
	}
	
	private static Object apiPost(String url, byte[] body, String type) throws IOException {
		Object res;

		HttpConnection hc = null;
		InputStream in = null;
		try {
			String inst;
			if (apiMode == API_GITHUB) {
				inst = GITHUB_API_URL;
			} else if (customApiUrl != null) {
				inst = customApiUrl;
			} else if (apiMode == API_GITEA) {
				inst = GITEA_DEFAULT_API_URL;
			} else {
				throw new IllegalStateException("Invalid API mode");
			}
			hc = openHttpConnection(proxyUrl(url.startsWith("http") ? url : inst.concat(url)));
			hc.setRequestMethod("POST");
			hc.setRequestProperty("Content-Length", body == null ? "0" : Integer.toString(body.length));
			hc.setRequestProperty("Accept", "application/json");
			if (type != null) hc.setRequestProperty("Content-Type", type);
			if (body != null) {
				OutputStream out = hc.openOutputStream();
				out.write(body);
				out.flush();
				out.close();
			}

			int c = hc.getResponseCode();
			res = JSONStream.getStream(in = hc.openInputStream()).nextValue();
			if (c >= 400 || (res instanceof JSONObject && ((JSONObject) res).has("error"))) {
				throw new APIException(url, c, res);
			}
		} finally {
			if (in != null) try {
				in.close();
			} catch (IOException e) {}
			if (hc != null) try {
				hc.close();
			} catch (IOException e) {}
		}
		return res;
	}
	
	private static String proxyUrl(String url) {
		System.out.println(url);
		if (url == null
				|| (!useProxy && url.indexOf(";tw=") == -1)
				|| proxyUrl == null || proxyUrl.length() == 0 || "https://".equals(proxyUrl)) {
			return url;
		}
		return proxyUrl.concat(url(url));
	}
	
	private static HttpConnection openHttpConnection(String url) throws IOException {
		HttpConnection hc = (HttpConnection) Connector.open(url);
		hc.setRequestProperty("User-Agent", "j2me-client/" + version + " (https://github.com/shinovon)");
		return hc;
	}
	
	public static String url(String url) {
		StringBuffer sb = new StringBuffer();
		char[] chars = url.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			int c = chars[i];
			if (65 <= c && c <= 90) {
				sb.append((char) c);
			} else if (97 <= c && c <= 122) {
				sb.append((char) c);
			} else if (48 <= c && c <= 57) {
				sb.append((char) c);
			} else if (c == 32) {
				sb.append("%20");
			} else if (c == 45 || c == 95 || c == 46 || c == 33 || c == 126 || c == 42 || c == 39 || c == 40
					|| c == 41) {
				sb.append((char) c);
			} else if (c <= 127) {
				sb.append(hex(c));
			} else if (c <= 2047) {
				sb.append(hex(0xC0 | c >> 6));
				sb.append(hex(0x80 | c & 0x3F));
			} else {
				sb.append(hex(0xE0 | c >> 12));
				sb.append(hex(0x80 | c >> 6 & 0x3F));
				sb.append(hex(0x80 | c & 0x3F));
			}
		}
		return sb.toString();
	}

	private static String hex(int i) {
		String s = Integer.toHexString(i);
		return "%".concat(s.length() < 2 ? "0" : "").concat(s);
	}
	
	static String count(int n, int i) {
		boolean ru = "ru".equals(lang);
		return Integer.toString(n).concat(L[n == 1 || (ru && n % 10 == 1 && n % 100 != 11) ?
				i : (ru && (n % 10 > 4 || n % 10 < 2) ? (i + 2) : (i + 1))]);
	}
	
	// detailMode: 0 - date, 1 - offset or date, 2 - offset only
	static String localizeDate(String date, int detailMode) {
		long now = System.currentTimeMillis();
		long t = parseDateGMT(date);
		long d = (now - t) / 1000L;
		boolean ru = "ru".equals(lang);
		
		if (detailMode != 0) {
			if (d < 5) {
				return L[Now];
			}
			
			if (d < 60) {
				if (d == 1 || (ru && d % 10 == 1 && d % 100 != 11))
					return Integer.toString((int) d).concat(L[_secondAgo]);
				if (ru && (d % 10 > 4 || d % 10 < 2))
					return Integer.toString((int) d).concat(L[_secondsAgo2]);
				return Integer.toString((int) d).concat(L[_secondsAgo]);
			}
			
			if (d < 60 * 60) {
				d /= 60L;
				if (d == 1 || (ru && d % 10 == 1 && d % 100 != 11))
					return Integer.toString((int) d).concat(L[_minuteAgo]);
				if (ru && (d % 10 > 4 || d % 10 < 2))
					return Integer.toString((int) d).concat(L[_minutesAgo2]);
				return Integer.toString((int) d).concat(L[_minutesAgo]);
			}
			
			if (d < 24 * 60 * 60) {
				d /= 60 * 60L;
				if (d == 1 || (ru && d % 10 == 1 && d % 100 != 11))
					return Integer.toString((int) d).concat(L[_hourAgo]);
				if (ru && (d % 10 > 4 || d % 10 < 2))
					return Integer.toString((int) d).concat(L[_hoursAgo2]);
				return Integer.toString((int) d).concat(L[_hoursAgo]);
			}
			
			if (d < 7 * 24 * 60 * 60) {
				d /= 24 * 60 * 60L;
				if (d == 1)
					return L[Yesterday];
				if (ru && d % 10 == 1 && d % 100 != 11)
					return Integer.toString((int) d).concat(L[_dayAgo]);
				if (ru && (d % 10 > 4 || d % 10 < 2))
					return Integer.toString((int) d).concat(L[_daysAgo2]);
				return Integer.toString((int) d).concat(L[_daysAgo]);
			}

			if (d < 28 * 24 * 60 * 60) {
				d /= 7 * 24 * 60 * 60L;
				if (d == 1)
					return L[LastWeek];
				if (ru && d % 10 == 1 && d % 100 != 11)
					return Integer.toString((int) d).concat(L[_weekAgo]);
				if (ru && (d % 10 > 4 || d % 10 < 2))
					return Integer.toString((int) d).concat(L[_weeksAgo2]);
				return Integer.toString((int) d).concat(L[_weeksAgo]);
			}
			
			if (detailMode != 1) {
				if (d < 365 * 24 * 60 * 60) {
					d /= 30 * 24 * 60 * 60L;
					if (d == 1)
						return Integer.toString((int) d).concat(L[_monthAgo]);
					if (ru && (d % 10 > 4 || d % 10 < 2))
						return Integer.toString((int) d).concat(L[_monthsAgo2]);
					return Integer.toString((int) d).concat(L[_monthsAgo]);
				}
				
				d /= 365 * 24 * 60 * 60L;
				if (d == 1) return Integer.toString((int) d).concat(L[_yearAgo]);
				if (ru && (d % 10 > 4 || d % 10 < 2))
					return Integer.toString((int) d).concat(L[_yearsAgo2]);
				return Integer.toString((int) d).concat(L[_yearsAgo]);
			}
		}
		
		Calendar c = Calendar.getInstance();
		int currentYear = c.get(Calendar.YEAR);
		c.setTime(new Date(t));
		
		StringBuffer sb = new StringBuffer();
		if (detailMode != 0) sb.append(L[on_Date]);
		
		if (!ru) sb.append(L[Jan + c.get(Calendar.MONTH)]).append(' ');
		sb.append(c.get(Calendar.DAY_OF_MONTH));
		if (ru) sb.append(' ').append(L[Jan + c.get(Calendar.MONTH)]);
		
		int year = c.get(Calendar.YEAR);
		if (year != currentYear) {
			sb.append(", ").append(year);
		}
		
		return sb.toString();
	}
	
	static long parseDateGMT(String date) {
		Calendar c = parseDate(date);
		return c.getTime().getTime() + c.getTimeZone().getRawOffset() - parseTimeZone(date);
	}
	
	// ISO 8601 format date parser without timezone counted
	static Calendar parseDate(String date) {
		Calendar c = Calendar.getInstance();
		if (date.indexOf('T') != -1) {
			String[] timeSplit = split(date.substring(date.indexOf('T')+1), ':');
			if (timeSplit.length == 1) {
				c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeSplit[0]));
				c.set(Calendar.MINUTE, Integer.parseInt(timeSplit[0]));
			} else {
				if (timeSplit.length == 3) {
					String second = timeSplit[2];
					int i = second.indexOf('+');
					if (i == -1) {
						i = second.indexOf('-');
					}
					if (i == -1) {
						i = second.indexOf('Z');
					}
					if (i != -1) {
						second = second.substring(0, i);
					}
					i = second.indexOf('.');
					if (i != -1) {
						i = second.indexOf(',');
					}
					if (i != -1) {
						c.set(Calendar.MILLISECOND, Integer.parseInt(second.substring(i + 1)));
						second = second.substring(0, i);
					}
					c.set(Calendar.SECOND, Integer.parseInt(second));
				}
				
				c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeSplit[0]));
				c.set(Calendar.MINUTE, Integer.parseInt(timeSplit[1]));
			}
			date = date.substring(0, date.indexOf('T'));
		}
		String[] dateSplit = split(date, '-');
		if (dateSplit.length == 1) {
			c.set(Calendar.YEAR, Integer.parseInt(date.substring(0, 4)));
			c.set(Calendar.MONTH, Integer.parseInt(date.substring(4, 6))-1);
			c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(date.substring(6, 8)));
		} else {
			c.set(Calendar.YEAR, Integer.parseInt(dateSplit[0]));
			c.set(Calendar.MONTH, Integer.parseInt(dateSplit[1])-1);
			if (dateSplit.length == 3)
				c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateSplit[2]));
		}
		return c;
	}

	// get timezone offset of date in milliseconds
	static int parseTimeZone(String date) {
		int i = date.lastIndexOf('+');
		boolean m = false;
		if (i == -1 && date.lastIndexOf('Z') != -1)
			return 0;
		if (i == -1) {
			i = date.lastIndexOf('-');
			m = true;
		}
		if (i == -1)
			return 0;
		date = date.substring(i + 1);
		int offset = date.lastIndexOf(':');
		if (offset == -1) {
			if (date.length() == 4) {
				offset = (Integer.parseInt(date.substring(0, 2)) * 3600000) +
						(Integer.parseInt(date.substring(2, 4)) * 60000);
			} else {
				offset = (Integer.parseInt(date) * 3600000);
			}
		} else {
			offset = (Integer.parseInt(date.substring(0, offset)) * 3600000) +
					(Integer.parseInt(date.substring(offset + 1)) * 60000);
		}
		return m ? -offset : offset;
	}
	
	static String n(int n) {
		if (n < 10) {
			return "0".concat(Integer.toString(n));
		} else return Integer.toString(n);
	}
	
	static String[] split(String str, char d) {
		int i = str.indexOf(d);
		if (i == -1)
			return new String[] {str};
		Vector v = new Vector();
		v.addElement(str.substring(0, i));
		while (i != -1) {
			str = str.substring(i + 1);
			if ((i = str.indexOf(d)) != -1)
				v.addElement(str.substring(0, i));
			i = str.indexOf(d);
		}
		v.addElement(str);
		String[] r = new String[v.size()];
		v.copyInto(r);
		return r;
	}

}
