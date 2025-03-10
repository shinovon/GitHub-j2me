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
import java.util.Hashtable;
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
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.Spacer;
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
	static final int RUN_TOGGLE_STAR = 7;
	static final int RUN_THUMBNAILS = 8;
	static final int RUN_OPEN_PATH = 9;
	
	// api modes
	static final int API_GITHUB = 0;
	static final int API_GITEA = 1;
	
	// constants
	private static final String SETTINGS_RECORDNAME = "ghsets";
	private static final String BOOKMARKS_RECORDNAME = "ghbm";
	private static final String GITHUB_AUTH_RECORDNAME = "ghauth";
	private static final String GITEA_AUTH_RECORDNAME = "giteaauth";

	private static final String GITHUB_URL = "https://github.com/";
	static final String GITHUB_RAW_URL = "https://raw.githubusercontent.com/";
	private static final String GITHUB_API_URL = "https://api.github.com/";
	private static final String GITHUB_API_VERSION = "2022-11-28";
	
	private static final String GITHUB_OAUTH_CLIENT_ID = "Ov23liQSkThpLmVHIxsC";
	private static final String GITHUB_OAUTH_CLIENT_SECRET = "f41d31e17a8cd437e05f6423d5977615a9706505";
	private static final String GITHUB_OAUTH_REDIRECT_URI = "http://localhost:8082/oauth_github";
	private static final String GITHUB_OAUTH_SCOPE = "user, repo";
	
	static final String GITEA_DEFAULT_API_URL = "https://gitea.com/api/v1/";
	private static final String GITEA_DEFAULT_CLIENT_ID = "4fce904c-9a87-4dfd-ab58-991cd496ac93";
	private static final String GITEA_DEFAULT_CLIENT_SECRET = "gto_qxo2fawh6dugu4u2rbkon6cwnywqfnp6od5ga3kw7opiwkg42xda";
	private static final String GITEA_OAUTH_REDIRECT_URI = "http://localhost:8082/oauth_gitea";
	private static final String GITEA_OAUTH_PORT = "8082";

	// fonts
	static final Font largePlainFont = Font.getFont(0, 0, Font.SIZE_LARGE);
	static final Font medPlainFont = Font.getFont(0, 0, Font.SIZE_MEDIUM);
	static final Font medBoldFont = Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
	static final Font medItalicFont = Font.getFont(0, Font.STYLE_ITALIC, Font.SIZE_MEDIUM);
	static final Font medItalicBoldFont = Font.getFont(0, Font.STYLE_BOLD | Font.STYLE_ITALIC, Font.SIZE_MEDIUM);
	static final Font smallPlainFont = Font.getFont(0, 0, Font.SIZE_SMALL);
	static final Font smallBoldFont = Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_SMALL);
	static final Font smallItalicFont = Font.getFont(0, Font.STYLE_ITALIC, Font.SIZE_SMALL);

	static final IllegalStateException cancelException = new IllegalStateException("cancel");
	
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
	static String customApiUrl = GITEA_DEFAULT_API_URL;
	private static String lang = "en";
	private static boolean noFormat;
	private static boolean onlineResize = false;
	private static boolean loadImages = false;
	static boolean previewFiles;

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
	
	private static Object thumbLoadLock = new Object();
	private static Vector thumbsToLoad = new Vector();
	
	// source browser
	private static String repo;
	private static String ref;
	
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
	static Command mdLinkCmd;

	static Command followersCmd;
	static Command followingCmd;
	static Command reposCmd;
	static Command starsCmd;
	
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
	static Command starCmd;
	static Command readmeCmd;
	static Command filesCmd;
	
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
	private static Command authManualCmd;
	private static Command authPersonalTokenCmd;

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
	private static TextField clientIdField;
	private static TextField clientSecretField;
	
	private static Image fileImg;
	private static Image folderImg;

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
			noFormat = j.getBoolean("noFormat", noFormat);
			onlineResize = j.getBoolean("onlineResize", onlineResize);
			loadImages = j.getBoolean("loadImages", loadImages);
			previewFiles = j.getBoolean("previewFiles", previewFiles);
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
		
		(L = new String[180])[0] = "gh2me";
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
				|| (apiMode == API_GITEA && (giteaRefreshToken != null || giteaAccessToken != null))) {
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
		mdLinkCmd = new Command(L[Open], Command.ITEM, 1);

		followersCmd = new Command(L[Followers], Command.ITEM, 1);
		followingCmd = new Command(L[Following], Command.ITEM, 1);
		reposCmd = new Command(L[Repositories], Command.ITEM, 1);
		starsCmd = new Command(L[Stars], Command.ITEM, 1);
		
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
		starCmd = new Command(L[Star], Command.ITEM, 1);
		readmeCmd = new Command(L[Readme], Command.ITEM, 1);
		filesCmd = new Command(L[BrowseSource], Command.ITEM, 1);
		
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
		authManualCmd = new Command(L[ManualOAuth], Command.ITEM, 1);
		authPersonalTokenCmd = new Command(L[PersonalToken], Command.ITEM, 1);

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
		
		start(RUN_THUMBNAILS, null);
		
//		if (loadImages) {
//			start(RUN_THUMBNAILS, null);
//			start(RUN_THUMBNAILS, null);
//		}
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
					
					proxyChoice = new ChoiceGroup("", ChoiceGroup.MULTIPLE, new String[] {
							L[LoadImages], L[UseProxy], L[OnlineResize]
					}, null);
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
				
				try {
					f.append(new ImageItem(null, Image.createImage("/g.png"), Item.LAYOUT_LEFT, null));
				} catch (Exception ignored) {}
				
				StringItem s;
				s = new StringItem(null, "GH2ME v".concat(version));
				s.setFont(largePlainFont);
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
				
				if (giteaRefreshToken == null && giteaAccessToken == null) {
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
					f = new ReposForm("user/repos?", L[YourRepositories], "pushed", true);
				} else if (c == yourStarsCmd) {
					f = new ReposForm("user/starred?", L[YourStars], null, true);
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
				loadImages = proxyChoice.isSelected(0);
				useProxy = proxyChoice.isSelected(1);
				onlineResize = proxyChoice.isSelected(2);
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
					j.put("noFormat", noFormat);
					j.put("onlineResize", onlineResize);
					j.put("loadImages", loadImages);
					j.put("previewFiles", previewFiles);
					
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
			oauthMode = c == authGithubCmd ? 0 : 1;
			
			Form f = new Form(L[Authorization]);
			f.addCommand(backCmd);
			f.setCommandListener(this);
			
			StringItem s;

			s = new StringItem(null, L[ChooseAuthMethod]);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setFont(medPlainFont);
			f.append(s);
			
			if (!useProxy) {
				s = new StringItem(null, L[AutoOAuth], Item.BUTTON);
				s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.setDefaultCommand(authBrowserCmd);
				s.setItemCommandListener(this);
				f.append(s);
			}
			
			s = new StringItem(null, L[ManualOAuth], Item.BUTTON);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(authManualCmd);
			s.setItemCommandListener(this);
			f.append(s);
			
			s = new StringItem(null, L[PersonalToken], Item.BUTTON);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setDefaultCommand(authPersonalTokenCmd);
			s.setItemCommandListener(this);
			f.append(s);
			
			display(f);
			return;
		}
		if (c == authPersonalTokenCmd) {
			TextBox t = new TextBox(L[PersonalToken], "", 200, TextField.NON_PREDICTIVE);
			t.addCommand(cancelCmd);
			t.addCommand(authDoneCmd);
			t.setCommandListener(this);
			display(t);
			
			return;
		}
		if (c == authManualCmd) {
			String url = oauthUrl = getOauthUrl(oauthMode);
			
			Form f = new Form(L[Authorization]);
			f.addCommand(backCmd);
			f.setCommandListener(this);

			StringItem s;
			TextField t;
			
			if (oauthMode == API_GITEA) {
				f.append(clientIdField = new TextField("Client ID", giteaClientId, 200, TextField.NON_PREDICTIVE));
				f.append(clientSecretField = new TextField("Client Secret", giteaClientSecret, 200, TextField.NON_PREDICTIVE));
			}
			
			t = new TextField("OAuth URL", url, 400, TextField.URL | TextField.UNEDITABLE);
			t.addCommand(authRegenCmd);
			t.setItemCommandListener(this);
			f.append(t);


			s = new StringItem(null, "Copy this URL to supported browser");
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setFont(smallPlainFont);
			f.append(s);
			
			
			if (!useProxy) {
				s = new StringItem(null, L[OpenInBrowser], Item.BUTTON);
				s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.setDefaultCommand(authBrowserCmd);
				s.setItemCommandListener(this);
				f.append(s);
			}
			
			f.append("\n");
			
			authField = new TextField("Result URL", "", 400, TextField.URL);
			f.append(authField);

			s = new StringItem(null, "After authorization, copy resulted URL from address bar and paste here");
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			s.setFont(smallPlainFont);
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
			
			if (oauthUrl == null || clientIdField != null) {
				if (clientIdField != null) {
					giteaClientId = clientIdField.getString().trim();
					giteaClientSecret = clientSecretField.getString().trim();
					
				}
				oauthUrl = getOauthUrl(oauthMode);
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
			if (d instanceof TextBox) {
				String s = ((TextBox) d).getString().trim();
				long l = System.currentTimeMillis();
				if (oauthMode == API_GITHUB) {
					githubAccessToken = s;
					githubAccessTokenTime = l;
				} else {
					giteaAccessToken = s;
					giteaAccessTokenTime = l;
					giteaRefreshToken = null;
					giteaRefreshTokenTime = 0;
				}
				start(RUN_VALIDATE_AUTH, this);
				return;
			}
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
				browse(getApi().concat("repos/").concat(((RepoForm) d).url)
						.concat(apiMode == API_GITEA ? "/archive/" : "/zipball/")
						.concat(((RepoForm) d).selectedBranch).concat(apiMode == API_GITEA ? ".zip" : ""));
				return;
			}
			if (c == forkCmd) {
				openRepo(((RepoForm) d).parent);
				return;
			}
			if (c == starCmd) {
				if (!((GHForm) d).finished) return;
				start(RUN_TOGGLE_STAR, d);
				return;
			}
			if (c == filesCmd) {
				repo = ((RepoForm) d).url;
				ref = ((RepoForm) d).selectedBranch;
				start(RUN_OPEN_PATH, "");
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
				} else if (c == readmeCmd) {
					f = new FileForm("repos/".concat(url).concat("/readme?"), null, null, url, ((RepoForm) d).selectedBranch);
					f.setTitle("Readme");
				} else break a;
	
				display(f);
				start(RUN_LOAD_FORM, f);
				return;
			}
		}
		// UserForm commands
		if (d instanceof UserForm) {
			a: {
				String url = ((UserForm) d).url;
				GHForm f;
				if (c == reposCmd) {
					f = new ReposForm("users/".concat(url).concat("/repos?"),
							L[Repositories].concat(" - ").concat(url), GH.apiMode == GH.API_GITEA ? "updated" : "pushed", false);
				} else if (c == starsCmd) {
					f = new ReposForm("users/".concat(url).concat("/starred?"),
							L[Stars].concat(" - ").concat(url), null, true);
				} else if (c == followersCmd) {
					f = new UsersForm("users/".concat(url).concat("/followers?"),
							L[Followers].concat(" - ").concat(url));
				} else if (c == followingCmd) {
					f = new UsersForm("users/".concat(url).concat("/following?"),
							L[Following].concat(" - ").concat(url));
				} else break a;
	
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
		if (d instanceof FileForm) {
			if (c == downloadCmd) {
				browse(((FileForm) d).downloadUrl);
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
		if (c == List.SELECT_COMMAND) {
			int i = ((List) d).getSelectedIndex();
			if (i == -1) return;
			boolean dir = ((List) d).getImage(i) == folderImg;
			String name = ((List) d).getString(i);
			String path = d.getTitle();
			if (path != null && path.length() != 0)
				path = path.concat("/");
			path = path.concat(url(name));
			if (dir) {
				start(RUN_OPEN_PATH, path);
			} else {
				Form f = new FileForm(null, null, path, repo, ref);
				display(f);
				start(RUN_LOAD_FORM, f);
			}
			return;
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
			giteaClientId = clientIdField.getString().trim();
			giteaClientSecret = clientSecretField.getString().trim();
			((TextField) item).setString(oauthUrl = getOauthUrl(oauthMode));
			return;
		}
		if (c == mdLinkCmd) {
			String url = (String) ((GHForm) current).urls.get(item);
			if (url == null) return;
			if (url.startsWith("!")) {
				url = url.substring(1);
				if (url.indexOf("http") == -1) {
					if (!(current instanceof FileForm)) return;
					FileForm f = new FileForm(null, null, ((FileForm) current).resolveUrl(url),
							((FileForm) current).repo, ((FileForm) current).ref);
					display(f);
					start(RUN_LOAD_FORM, f);
					return;
				}
			}
			if (!openUrl(url)) {
				browse(url);
			}
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
			final byte[] CRLF = {(byte)'\r', (byte)'\n'};
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
			
			
			int mode;
			int prevMode = mode = apiMode;
			if (param != null) {
				mode = apiMode = oauthMode;
			}
			
			if (mode == API_GITHUB && githubAccessToken != null) {
				try {
					login = ((JSONObject) api("user")).getString("login");
				} catch (Exception e) {
					// token revoked
					githubAccessToken = null;
				}
				writeGithubAuth();
			} else if (mode == API_GITEA && (giteaRefreshToken != null || giteaAccessToken != null)) {
				if (giteaRefreshToken != null && giteaAccessToken != null
						&& System.currentTimeMillis() - giteaAccessTokenTime > 3600000L) {
					giteaAccessToken = null;
				}
				if (giteaAccessToken != null) {
					try {
						login = ((JSONObject) api("user")).getString("login");
					} catch (Exception e) {
						// token expired
						giteaAccessToken = null;
					}
				} else if (giteaRefreshToken != null) {
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
						try {
							login = ((JSONObject) api("user")).getString("login");
						} catch (Exception e) {
							// ???
						}
					} catch (Exception e) {
						// token revoked
						giteaRefreshToken = null;
					}
				}
				writeGiteaAuth();
			}
			display(/*current*/ mainForm, true);
			if (param != null) {
				apiMode = prevMode;
				if (login != null) {
					display(infoAlert(L[Authorized]), mainForm);
				} else {
					display(errorAlert(L[AuthFailed]), mainForm);
				}
			}
			break;
		}
		case RUN_CHECK_OAUTH_CODE: { // finish oauth manually
			acceptOauthToken((String) param, oauthMode);
			break;
		}
		case RUN_TOGGLE_STAR: { // toggle repo starred state
			if (!useProxy || login == null) break;
			try {
				apiPost("user/starred/".concat(((RepoForm) param).url)
						.concat(";method=").concat(((RepoForm) param).starred ? "DELETE" : "PUT"), null, null);
				((RepoForm) param).starBtn.setText(L[((RepoForm) param).starred ? Star : Starred]);
			} catch (Exception ignored) {
				ignored.printStackTrace();
			}
			break;
		}
		case RUN_THUMBNAILS: { // background thumbnails loader thread
			try {
				while (true) {
					synchronized (thumbLoadLock) {
						thumbLoadLock.wait();
					}
					Thread.sleep(200);
					while (thumbsToLoad.size() > 0) {
						int i = 0;
						Object[] o = null;
						
						try {
							synchronized (thumbLoadLock) {
								o = (Object[]) thumbsToLoad.elementAt(i);
								thumbsToLoad.removeElementAt(i);
							}
						} catch (Exception e) {
							continue;
						}
						
						if (o == null) continue;
						
						String url = (String) o[0];
						ImageItem item = (ImageItem) o[1];
						
						if (url == null) continue;
						
						try {
							if (url.startsWith("!")) {
								url = url.substring(1);
								if (!url.startsWith("http")) {
									if (!(current instanceof FileForm)) continue;
									
									url = ((FileForm) current).fetchBlobUrl(url);
								}
							}

							int sw = getWidth();
							int sh = getHeight() / 3;
							Image img;
							if (onlineResize) {
								img = getImage(proxyUrl(url + ";wh=" + (getWidth()) + ";th=" + (getHeight() / 3)));
							} else {
								img = getImage(proxyUrl(url));
								
								int ow = img.getWidth(), oh = img.getHeight();
								if (ow > sw || oh > sh) {
									int h = sh;
									int w = (int) (((float) h / oh) * ow);
									if (w > sw) {
										w = sw;
										h = (int) (((float) w / ow) * oh);
									}
									img = resize(img, w, h);
								}
							}
							
							item.setImage(img);
						} catch (Exception e) {
							e.printStackTrace();
						} 
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		case RUN_OPEN_PATH: {
			String path = (String) param;
			display(loadingAlert(L[Loading]), current);
			try {
				if (fileImg == null) {
					fileImg = Image.createImage("/file.png");
					folderImg = Image.createImage("/folder.png");
				}
				Object r = api("repos/".concat(repo).concat("/contents").concat(path).concat("?ref=").concat(ref));
				
				if (r instanceof JSONArray) {
					List list = new List(path, List.IMPLICIT);
					list.addCommand(backCmd);
					list.addCommand(List.SELECT_COMMAND);
					list.setCommandListener(this);
					
					int l = ((JSONArray) r).size();
					for (int i = 0; i < l; ++i) {
						JSONObject j = ((JSONArray) r).getObject(i);
						// TODO pass download_url
						list.append(j.getString("name"), "dir".equals(j.getString("type")) ? folderImg : fileImg);
					}
					
					display(list);
				} else {
					FileForm f = new FileForm(null, null, path, repo, ref);
					display(f);
					start(RUN_LOAD_FORM, f);
				}
				
			} catch (Exception e) {
				display(errorAlert(e.toString()), current);
				e.printStackTrace();
			}
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
	
	private static void scheduleThumb(ImageItem item, String url) {
		if (!loadImages || item == null || url == null) return;
//		if (url.startsWith("!")) {
//			url = url.substring(1);
//			if (url.indexOf("http") == -1) {
//				if (!(current instanceof FileForm)) return;
//				url = ((FileForm) current).blobUrl(url);
//			}
//		}
		synchronized (thumbLoadLock) {
			thumbsToLoad.addElement(new Object[] { url, item });
			thumbLoadLock.notifyAll();
		}
	}
	
	static boolean openUrl(String url) {
//		System.out.println("openUrl:".concat(url));
		if (url.startsWith(GITHUB_URL)) {
			url = url.substring(19);
		} else if (url.startsWith("https://api.github.com/repos/")
				|| url.startsWith("https://api.github.com/users/")) {
			url = url.substring(29);
		}
		if (url.indexOf('/') == -1) {
			// user
			openUser(url);
			return true;
		} else {
			// repo
			String[] split = split(url, '/');
			if (split.length == 2 || split[2].length() == 0) {
				openRepo(url);
				return true;
			}
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
			case 'b': // braches or blob
				if ("branches".equals(split[2])) {
					f = new BranchesForm(repo);
				} else if ("blob".equals(split[2])) {
					url = url.substring(url.indexOf('/', url.indexOf('/', url.indexOf('/') + 1) + 1) + 1);
					f = new FileForm(null, null, url, repo, split[3]);
				} else {
					return false;
				}
				break;
			default:
				return false;
			}
			display(f);
			midlet.start(RUN_LOAD_FORM, f);
			return true;
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
		
		display(infoAlert(L[BookmarkSaved]), d);
	}

	private static void writeHttpHeader(OutputStream out, int code, String message) throws IOException {
		out.write("HTTP/1.1 ".getBytes());
		out.write(Integer.toString(code).getBytes());
		out.write(' ');
		if (message != null) out.write(message.getBytes());
		out.write("\r\n".getBytes());
		out.write("Connection: close".getBytes());
		out.write("\r\n".getBytes());
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
			sb.append(GITHUB_URL + "login/oauth/authorize?client_id=")
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
				
				j = (JSONObject) apiPost(GITHUB_URL + "login/oauth/access_token",
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
			display(infoAlert(L[Authorized]), mainForm);
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
	
	private static int getWidth() {
		return mainForm.getWidth();
	}
	
	private static int getHeight() {
		return mainForm.getHeight();
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
			
			clientIdField = null;
			clientSecretField = null;
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
	
	static String getApi() {
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
		return inst;
	}

	void browse(String url) {
		try {
			if (url.indexOf(':') == -1) {
				url = "http://".concat(url);
			} else if (useProxy && (url.startsWith(GITHUB_URL) || url.startsWith(GITHUB_API_URL))) {
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
			hc = openHttpConnection(proxyUrl(getApi().concat(url)));
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
//		System.out.println(res instanceof JSONObject ?
//				((JSONObject) res).format(0) : res instanceof JSONArray ?
//						((JSONArray) res).format(0) : res);
		return res;
	}
	
	private static Object apiPost(String url, byte[] body, String type) throws IOException {
		Object res;

		HttpConnection hc = null;
		InputStream in = null;
		try {
			hc = openHttpConnection(proxyUrl(url.startsWith("http") ? url : getApi().concat(url)));
			hc.setRequestMethod("POST");
			hc.setRequestProperty("Accept", "application/json");
			if (!url.startsWith("http")) {
				if (apiMode == API_GITHUB) {
					hc.setRequestProperty("X-Github-Api-Version", GITHUB_API_VERSION);
					if (githubAccessToken != null)
						hc.setRequestProperty("Authorization", "Bearer ".concat(githubAccessToken));
				} else {
					if (giteaAccessToken != null)
						hc.setRequestProperty("Authorization", "Bearer ".concat(giteaAccessToken));
				}
			}
			hc.setRequestProperty("Content-Length", body == null ? "0" : Integer.toString(body.length));
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
//		System.out.println(res instanceof JSONObject ?
//				((JSONObject) res).format(0) : res instanceof JSONArray ?
//						((JSONArray) res).format(0) : res);
		return res;
	}
	
	static JSONStream apiStream(String url) throws IOException {
		JSONStream res = null;

		HttpConnection hc = null;
		InputStream in = null;
		try {
			hc = openHttpConnection(proxyUrl(getApi().concat(url)));
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
			if (c >= 400) {
				throw new APIException(url, c, null);
			}
			res = JSONStream.getStream(hc);
		} finally {
			if (res == null) {
				if (in != null) try {
					in.close();
				} catch (IOException e) {}
				if (hc != null) try {
					hc.close();
				} catch (IOException e) {}
			}
		}
//		System.out.println(res instanceof JSONObject ?
//				((JSONObject) res).format(0) : res instanceof JSONArray ?
//						((JSONArray) res).format(0) : res);
		return res;
	}

	private static Image getImage(String url) throws IOException {
		byte[] b = get(url);
		return Image.createImage(b, 0, b.length);
	}
	
	private static byte[] readBytes(InputStream inputStream, int initialSize, int bufferSize, int expandSize) throws IOException {
		if (initialSize <= 0) initialSize = bufferSize;
		byte[] buf = new byte[initialSize];
		int count = 0;
		byte[] readBuf = new byte[bufferSize];
		int readLen;
		while ((readLen = inputStream.read(readBuf)) != -1) {
			if (count + readLen > buf.length) {
				byte[] newbuf = new byte[count + expandSize];
				System.arraycopy(buf, 0, newbuf, 0, count);
				buf = newbuf;
			}
			System.arraycopy(readBuf, 0, buf, count, readLen);
			count += readLen;
		}
		if (buf.length == count) {
			return buf;
		}
		byte[] res = new byte[count];
		System.arraycopy(buf, 0, res, 0, count);
		return res;
	}
	
	private static byte[] get(String url) throws IOException {
		HttpConnection hc = null;
		InputStream in = null;
		try {
			hc = openHttpConnection(url);
			hc.setRequestMethod("GET");
			int r;
			if ((r = hc.getResponseCode()) >= 400) {
				throw new IOException("HTTP " + r);
			}
			in = hc.openInputStream();
			return readBytes(in, (int) hc.getLength(), 8*1024, 16*1024);
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException e) {
			}
			try {
				if (hc != null) hc.close();
			} catch (IOException e) {
			}
		}
	}
	
	private static String proxyUrl(String url) {
		System.out.println(url);
		if (url == null
				|| (!useProxy && (url.indexOf(";tw=") == -1 && url.indexOf(";th=") == -1))
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
	
	// tube42 imagelib

	static Image resize(Image src_i, int size_w, int size_h) {
		// set source size
		int w = src_i.getWidth();
		int h = src_i.getHeight();

		// no change??
		if (size_w == w && size_h == h)
			return src_i;
		
//		if (MangaApp.mipmap) {
//			while (w > size_w * 3 && h > size_h * 3) {
//				src_i = halve(src_i);
//				w /= 2;
//				h /= 2;
//			}
//		}

		int[] dst = new int[size_w * size_h];

		resize_rgb_filtered(src_i, dst, w, h, size_w, size_h);

		// not needed anymore
		src_i = null;

		return Image.createRGBImage(dst, size_w, size_h, true);
	}
	
	private static final void resize_rgb_filtered(Image src_i, int[] dst, int w0, int h0, int w1, int h1) {
		int[] buffer1 = new int[w0];
		int[] buffer2 = new int[w0];

		// UNOPTIMIZED bilinear filtering:
		//
		// The pixel position is defined by y_a and y_b,
		// which are 24.8 fixed point numbers
		// 
		// for bilinear interpolation, we use y_a1 <= y_a <= y_b1
		// and x_a1 <= x_a <= x_b1, with y_d and x_d defining how long
		// from x/y_b1 we are.
		//
		// since we are resizing one line at a time, we will at most 
		// need two lines from the source image (y_a1 and y_b1).
		// this will save us some memory but will make the algorithm 
		// noticeably slower

		for (int index1 = 0, y = 0; y < h1; y++) {

			final int y_a = ((y * h0) << 8) / h1;
			final int y_a1 = y_a >> 8;
			int y_d = y_a & 0xFF;

			int y_b1 = y_a1 + 1;
			if (y_b1 >= h0) {
				y_b1 = h0 - 1;
				y_d = 0;
			}

			// get the two affected lines:
			src_i.getRGB(buffer1, 0, w0, 0, y_a1, w0, 1);
			if (y_d != 0)
				src_i.getRGB(buffer2, 0, w0, 0, y_b1, w0, 1);

			for (int x = 0; x < w1; x++) {
				// get this and the next point
				int x_a = ((x * w0) << 8) / w1;
				int x_a1 = x_a >> 8;
				int x_d = x_a & 0xFF;

				int x_b1 = x_a1 + 1;
				if (x_b1 >= w0) {
					x_b1 = w0 - 1;
					x_d = 0;
				}

				// interpolate in x
				int c12, c34;
				int c1 = buffer1[x_a1];
				int c3 = buffer1[x_b1];

				// interpolate in y:
				if (y_d == 0) {
					c12 = c1;
					c34 = c3;
				} else {
					int c2 = buffer2[x_a1];
					int c4 = buffer2[x_b1];

					final int v1 = y_d & 0xFF;
					final int a_c2_RB = c1 & 0x00FF00FF;
					final int a_c2_AG_org = c1 & 0xFF00FF00;

					final int b_c2_RB = c3 & 0x00FF00FF;
					final int b_c2_AG_org = c3 & 0xFF00FF00;

					c12 = (a_c2_AG_org + ((((c2 >>> 8) & 0x00FF00FF) - (a_c2_AG_org >>> 8)) * v1)) & 0xFF00FF00
							| (a_c2_RB + ((((c2 & 0x00FF00FF) - a_c2_RB) * v1) >> 8)) & 0x00FF00FF;
					c34 = (b_c2_AG_org + ((((c4 >>> 8) & 0x00FF00FF) - (b_c2_AG_org >>> 8)) * v1)) & 0xFF00FF00
							| (b_c2_RB + ((((c4 & 0x00FF00FF) - b_c2_RB) * v1) >> 8)) & 0x00FF00FF;
				}

				// final result

				final int v1 = x_d & 0xFF;
				final int c2_RB = c12 & 0x00FF00FF;

				final int c2_AG_org = c12 & 0xFF00FF00;
				dst[index1++] = (c2_AG_org + ((((c34 >>> 8) & 0x00FF00FF) - (c2_AG_org >>> 8)) * v1)) & 0xFF00FF00
						| (c2_RB + ((((c34 & 0x00FF00FF) - c2_RB) * v1) >> 8)) & 0x00FF00FF;
			}
		}
	}
	
	// base64
	
	private final static byte[] DECODE_ALPHABET = {
			-9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 0 - 8
			-5, -5, // Whitespace: Tab and Linefeed
			-9, -9, // Decimal 11 - 12
			-5, // Whitespace: Carriage Return
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 14 - 26
			-9, -9, -9, -9, -9, // Decimal 27 - 31
			-5, // Whitespace: Space
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 33 - 42
			62, // Plus sign at decimal 43
			-9, -9, -9, // Decimal 44 - 46
			63, // Slash at decimal 47
			52, 53, 54, 55, 56, 57, 58, 59, 60, 61, // Numbers zero through nine
			-9, -9, -9, // Decimal 58 - 60
			-1, // Equals sign at decimal 61
			-9, -9, -9, // Decimal 62 - 64
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, // Letters 'A' through 'N'
			14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // Letters 'O' through 'Z'
			-9, -9, -9, -9, -9, -9, // Decimal 91 - 96
			26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, // Letters 'a' through 'm'
			39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, // Letters 'n' through 'z'
			-9, -9, -9, -9 // Decimal 123 - 126
	};

	public static byte[] decodeBase64(byte[] source, int[] lenPtr) {
		if (source == null) {
			return null;
		}
		int len34 = source.length * 3 / 4;
		byte[] outBuff = new byte[len34];
		int outBuffPosn = 0;

		byte[] b4 = new byte[4];
		int b4Posn = 0;
		int i = 0;
		byte sbiCrop = 0;
		byte sbiDecode = 0;
		for (i = 0; i < source.length; i++) {
			sbiCrop = (byte) (source[i] & 0x7f);
			sbiDecode = DECODE_ALPHABET[sbiCrop];

			if (sbiDecode >= -5) {
				if (sbiDecode >= -1) {
					b4[b4Posn++] = sbiCrop;
					if (b4Posn > 3) {
						outBuffPosn += decode4to3(b4, 0, outBuff, outBuffPosn);
						b4Posn = 0;

						if (sbiCrop == '=')
							break;
					}
				}
			} else {
				return null;
			}
		}
		if (outBuffPosn == 0) {
			return null;
		}
		if (lenPtr == null) {
			byte[] out = new byte[outBuffPosn];
			System.arraycopy(outBuff, 0, out, 0, outBuffPosn);
			return out;
		}
		lenPtr[0] = outBuffPosn;
		return outBuff;
	}

	private static int decode4to3(byte[] source, int srcOffset, byte[] destination, int destOffset) {
		if (source[srcOffset + 2] == '=') {
			int outBuff = ((DECODE_ALPHABET[source[srcOffset]] & 0xFF) << 18)
					| ((DECODE_ALPHABET[source[srcOffset + 1]] & 0xFF) << 12);
			destination[destOffset] = (byte) (outBuff >>> 16);
			return 1;
		} else if (source[srcOffset + 3] == '=') {
			int outBuff = ((DECODE_ALPHABET[source[srcOffset]] & 0xFF) << 18)
					| ((DECODE_ALPHABET[source[srcOffset + 1]] & 0xFF) << 12)
					| ((DECODE_ALPHABET[source[srcOffset + 2]] & 0xFF) << 6);
			destination[destOffset] = (byte) (outBuff >>> 16);
			destination[destOffset + 1] = (byte) (outBuff >>> 8);
			return 2;
		} else {
			try {
				int outBuff = ((DECODE_ALPHABET[source[srcOffset]] & 0xFF) << 18)
						| ((DECODE_ALPHABET[source[srcOffset + 1]] & 0xFF) << 12)
						| ((DECODE_ALPHABET[source[srcOffset + 2]] & 0xFF) << 6)
						| ((DECODE_ALPHABET[source[srcOffset + 3]] & 0xFF));
				destination[destOffset] = (byte) (outBuff >> 16);
				destination[destOffset + 1] = (byte) (outBuff >> 8);
				destination[destOffset + 2] = (byte) (outBuff);
				return 3;
			} catch (Exception e) {
				return -1;
			}
		}
	}
	
	// Markdown parser

	private static final int
			MD_FONT_FACE = 0,
			MD_FONT_STYLE = 1,
			MD_FONT_SIZE = 2,
			MD_TAB = 3,
			MD_SPACES = 4,
			MD_LAST_TAB = 5,
			MD_ESCAPE = 6,
			MD_QUOTE = 7,
			MD_HEADER = 8,
			MD_LENGTH = 9,
			MD_ITALIC = 10,
			MD_BOLD = 11,
			MD_HTML_BOLD = 12,
			MD_HTML_ITALIC = 13,
			MD_LINE = 14,
			MD_HTML_PARAGRAPH = 15,
			MD_HTML_UNDERLINE = 16,
			MD_HTML_HEADER = 17,
			MD_UNDERSCORE = 18,
			MD_HASH = 19,
			MD_GRAVE = 20,
			MD_STRIKE = 21,
			MD_ASTERISK = 22,
			MD_HTML_BIG = 23,
			MD_HTML_LINK = 24,
			MD_BRACKET = 25,
			MD_LINK = 26,
			MD_IMAGE = 27,
			MD_PARENTHESIS = 28,
			MD_PARAGRAPH = 29,
			MD_BREAKS = 30;
	
	public static void parseMarkdown(Thread thread, GHForm form, String body, int insert, Hashtable urls) {
		if (body == null) return;
		if (insert == -1) insert = form.size();
		if (noFormat) {
			if (body.trim().length() == 0) return;
			form.safeInsert(thread, insert, new StringItem(null, body));
			return;
		}
		
//		System.out.println("Init: " + body);
		
		StringBuffer sb = new StringBuffer();
//		boolean noFormat = GH.noFormat;
		int d = body.indexOf('<');
		int len = body.length();
		if (len == 0) return;
		int o = 0;
		int[] state = new int[32];
		state[MD_FONT_SIZE] = Font.SIZE_SMALL;
		
		Item item = null;
		
		char[] chars = body.toCharArray();
		while (d != -1 || o < len) {
			a: {
				if (o != d) {
					if (d == -1) d = len;
					char l = 0;
					int i;
					for (i = o; i < d; ++i) {
						char c = chars[i];
//						System.out.print("> " + c + ": ");
						if (state[MD_HASH] != 0 && ((c != '#' && c != ' ') || state[MD_HASH] > 6)) {
							for (int k = 0; k < state[MD_HASH]; ++k) sb.append('#');
							state[MD_LENGTH] += state[MD_HASH];
							state[MD_HASH] = 0;
						}
						if (c == '\r' || (c == '\n' && l != '\r')) {
							l = c;
							boolean b;
							if ((b = state[MD_HEADER] != 0) || state[MD_LENGTH] != 0 || state[MD_ESCAPE] == 1) {
								if (b) {
									sb.append('\n');
									insert = flush(thread, form, sb, insert, state);
									state[MD_PARAGRAPH] = 1;
								}
								state[MD_LAST_TAB] = state[MD_TAB];
								state[MD_UNDERSCORE] = state[MD_HASH] = state[MD_BOLD] = state[MD_ITALIC]
										= state[MD_HEADER] = state[MD_ESCAPE] = state[MD_LENGTH] 
										= state[MD_QUOTE] = state[MD_SPACES] = state[MD_TAB]
										= state[MD_GRAVE] = state[MD_STRIKE] = state[MD_ASTERISK]
										= state[MD_BRACKET] = state[MD_LINK]
										= state[MD_IMAGE] = state[MD_PARENTHESIS] = 0;
								state[MD_BREAKS] ++;
	
								if (!b) {
									sb.append('\n');
									state[MD_PARAGRAPH] = 0;
								}
							} else if (state[MD_LENGTH] == 0 && state[MD_PARAGRAPH] == 0) {
								insert = flush(thread, form, sb, insert, state);
								Spacer spacer = new Spacer(10, 10);
								spacer.setLayout(Item.LAYOUT_NEWLINE_AFTER);
								form.safeInsert(thread, insert++, spacer);
								state[MD_PARAGRAPH] = 1;
							}
							continue;
						} else if (c <= ' ' && l <= ' ') {
							if ((l == c) && (state[MD_SPACES]++ == 0 || state[MD_SPACES] == 4)) {
								state[MD_TAB] ++;
								state[MD_SPACES] = 0;
							}
							l = c;
							continue;
						} else if (state[MD_PARENTHESIS] != 0 && c != ')') {
							l = c;
							sb.append(c);
							continue;
						} else {
							state[MD_SPACES] = 0;
							if (c == '&' && i + 1 != len) { // entity
								switch (chars[i + 1]) {
								case 'n': // nsbp;
									if (i + 5 >= len) {
										break;
									}
									if (chars[i += 5] == ';') {
										c = ' ';
									} else i -= 5;
									break;
								case 'l': // lt
									if (i + 3 >= len) break;
									if (chars[i += 3] == ';') {
										c = '<';
									} else i -= 3;
									break;
								case 'g': // gt
									if (i + 3 >= len) break;
									if (chars[i += 3] == ';') {
										c = '>';
									} else i -= 3;
									break;
								case 'a': // amp
									if (i + 4 >= len) break;
									if (chars[i += 4] == ';') {
										c = '&';
									} else i -= 4;
									break;
								case '#':
									int k = i + 2;
									try {
										while (chars[++k] != ';' && k - i < 10);
										if (k - i == 10) break;
										c = (char) Integer.parseInt(new String(chars, i + 2, k - i - 2));
										i = k;
									} catch (Exception e) {
										e.printStackTrace();
									}
									break;
								}
							} else if (state[MD_ESCAPE] == 0) {
								switch (c) {
								case '\t':
									c = ' ';
									break;
								case '\\': // escape
									state[MD_ESCAPE] = 1;
									l = c;
									continue;
								case ' ':
									if (state[MD_HASH] != 0) {
										insert = flush(thread, form, sb, insert, state);
										l = c;
										state[MD_HEADER] = state[MD_HASH];
										state[MD_HASH] = 0;
										continue;
									}
									if (state[MD_LENGTH] == 0) {
										l = c;
										continue;
									}
									break;
								case '>':
									if (state[MD_LENGTH] == 0 && state[MD_QUOTE] == 0) {
										state[MD_QUOTE] ++;
										continue;
									}
									break;
								case '#':
									if (state[MD_LENGTH] == 0 && state[MD_HEADER] == 0) {
										state[MD_HASH] ++;
										l = c;
										continue;
									}
									break;
								case '-': {
									if (state[MD_LENGTH] == 0 && i + 2 < len
											&& chars[i + 1] == c && chars[i + 2] == c) {
										int k = i;
										while (++k < len && chars[k] != '\n' && chars[k] != '\r');
										if (chars[k - 1] == c) {
											i = k - 1;
											state[MD_LINE] ++;
											sb.append("\n");
											insert = flush(thread, form, sb, insert, state);
											continue;
										}
									}
									break;
								}
								case '*':
								case '_': {
									int t = c == '*' ? MD_ASTERISK : MD_UNDERSCORE;
									if (state[t] == 0 && state[MD_LENGTH] == 0 && i + 2 < len
											&& chars[i + 1] == c && chars[i + 2] == c) {
										int k = i;
										while (++k < len && chars[k] != '\n' && chars[k] != '\r');
										if (chars[k - 1] == c) {
											i = k - 1;
											state[MD_LINE] ++;
											sb.append("\n");
											insert = flush(thread, form, sb, insert, state);
											continue;
										}
									}
									
									if (state[t] == 0 && ((c == '_' && l > ' ') || i + 1 >= len)) {
										break;
									}
									int k = i; // line length
									while (++k < len && chars[k] != '\n' && chars[k] != '\r');
									
									if (state[t] == 0) {
										if (i + 2 >= k) break;
										int j = body.indexOf(c, i + 1);
										if (j == -1 || j >= k) break;
									}
									
									l = c;
									if (i + 1 < k && chars[i + 1] == c) {
										String s;
										if (i + 2 < k && chars[i + 2] == c) {
											s = c == '*' ? "***" : "___";
											if (state[t] == 3) {
												insert = flush(thread, form, sb, insert, state);
												state[t] = 0;
												state[MD_BOLD] --;
												state[MD_ITALIC] --;
												i += 2;
												continue;
											} else if (state[t] == 0) {
												int j = body.indexOf(s, i + 1);
												if (i + 6 >= k || j == -1 || j >= k || chars[i + 3] <= ' '
														|| (j + 3 != k && chars[j + 3] > ' ')) {
													sb.append(s);
													i += 2;
													continue;
												}
												insert = flush(thread, form, sb, insert, state);
												state[t] = 3;
												state[MD_BOLD] ++;
												state[MD_ITALIC] ++;
												i += 2;
												continue;
											}
											sb.append(c).append(c);
											i+=3;
											break;
										}
										s = c == '*' ? "**" : "__";
										if (state[t] == 2) {
											insert = flush(thread, form, sb, insert, state);
											state[t] = 0;
											state[MD_BOLD] --;
											i++;
											continue;
										} else if (state[t] == 0) {
											int j = body.indexOf(s, i + 1);
											if (i + 4 >= k || j == -1 || j >= k
													|| chars[i + 2] <= ' ' || chars[j - 1] <= ' '
													|| (c == '_' && j + 2 != k && chars[j + 2] > ' ')) {
												sb.append(s);
												i++;
												continue;
											}
											insert = flush(thread, form, sb, insert, state);
											state[t] = 2;
											state[MD_BOLD] ++;
											i++;
											continue;
										}
										sb.append(c);
										i++;
										break;
									}
									
									if (state[t] == 1) {
										insert = flush(thread, form, sb, insert, state);
										state[t] = 0;
										state[MD_ITALIC] = 0;
										continue;
									}
									
									if (state[t] == 0) {
										int j = body.indexOf(c, i + 1);
										if (j == -1 || j >= k || chars[i] <= ' '
												|| (c == '_' && j + 1 != k && chars[j + 1] > ' ')) {
											break;
										}
										insert = flush(thread, form, sb, insert, state);
										state[t] = 1;
										state[MD_ITALIC] = 1;
										state[MD_LENGTH] ++;
										continue;
									}
									break;
								}
								case '~': {
									if (i + 1 >= len || chars[i + 1] != c) {
										break;
									}
									int k = i; // line length
									while (++k < len && chars[k] != '\n' && chars[k] != '\r');
									if (state[MD_STRIKE] == 0) {
										if (i + 4 >= k) break;
										int j = body.indexOf("~~", i + 1);
										if (j == -1 || j >= k || chars[i + 2] <= ' ' || chars[j - 1] <= ' ') {
											break;
										}

										insert = flush(thread, form, sb, insert, state);
										state[MD_STRIKE] = 1;
										state[MD_LENGTH] ++;
										i++;
										continue;
									}
									if (state[MD_STRIKE] == 1) {
										insert = flush(thread, form, sb, insert, state);
										state[MD_STRIKE] = 0;
										i++;
										continue;
									}
									continue;
								}
								case '`': {
									if (i + 1 >= len) {
										break;
									}
									if (i + 2 < len && chars[i + 1] == c && chars[i + 2] == c) {
										insert = flush(thread, form, sb, insert, state);
										i += state[MD_GRAVE] = 3;
									} else {
										int j = body.indexOf('`', i + 1);
										if (j == -1) break;
										if (i + 1 < len && chars[i + 1] == c) {
											insert = flush(thread, form, sb, insert, state);
											i += state[MD_GRAVE] = 2;
										} else {
											insert = flush(thread, form, sb, insert, state);
											i += state[MD_GRAVE] = 1;
										}
									}
									if (state[MD_GRAVE] != 0) {
//										i += state[MD_GRAVE];
										while (i < len) {
											if ((c = chars[i++]) == '`') {
												if (state[MD_GRAVE] == 1) {
													state[MD_GRAVE] = 0;
													break;
												} else if (state[MD_GRAVE] == 2) {
													if (i + 1 < len && chars[i] == c) {
														state[MD_GRAVE] = 0;
														i++;
														break;
													}
												} else if (state[MD_GRAVE] == 3) {
													if (i + 2 < len && chars[i] == c && chars[i + 1] == c) {
														state[MD_GRAVE] = 0;
														i += 2;
														break;
													}
												}
											} else if (c <= ' ' && state[MD_GRAVE] != 3) {
												if (l == ' ') continue;
												c = ' ';
											} else if (c == '\t') {
												l = c;
												sb.append("    ");
												continue;
											}
											l = c;
											sb.append(c);
										}
										insert = flush(thread, form, sb, insert, state);
										d = body.indexOf('<', o = i);
										state[MD_LENGTH] ++;
										break a;
									}
									continue;
								}
								case '!': {
									if (i + 1 == len || chars[i + 1] != '[') {
										break;
									}
									state[MD_IMAGE] ++;
									continue;
								}
								case '[': {
//									if (state[MD_BRACKET] != 0) {
//										break;
//									}
									
									int k = i; // line length
									while (++k < len && chars[k] != '\n' && chars[k] != '\r');
									
									int n, m;
									if ((n = body.indexOf(']', i)) == -1 || n >= k
											|| (m = body.indexOf('(', n)) == -1
											|| m != n + 1 || m >= k
											|| (m = body.indexOf(')', m)) == -1 || m >= k) {
										break;
									}
									
									insert = flush(thread, form, sb, insert, state);
									
									if (state[MD_IMAGE] != 0) {
										item = new ImageItem("Image", null, 0, null);
										item.setDefaultCommand(mdLinkCmd);
										item.setItemCommandListener(midlet);
									} else {
										item = new StringItem(null, "");
										((StringItem) item).setFont(getFont(state));
										item.setDefaultCommand(mdLinkCmd);
										item.setItemCommandListener(midlet);
									}
									
									l = c;
									state[MD_BRACKET] ++;
									state[MD_LINK] ++;
									state[MD_LENGTH] ++;
									continue;
								}
								case ']': {
									if (state[MD_BRACKET] == 0 || i + 1 == len || chars[i + 1] != '(') {
										break;
									}

									String s = sb.toString();
									if (item instanceof StringItem) {
										((StringItem) item).setText(s);
									} else if (item instanceof ImageItem) {
										((ImageItem) item).setLabel(s);
									}
									sb.setLength(0);
									
									l = c;
									state[MD_BRACKET] --;
//									i++;
									continue;
								}
								case '(': {
									if (state[MD_LINK] == 0 || chars[i - 1] != ']') {
										break;
									}
									
									l = '(';
									state[MD_PARENTHESIS] = 1;
									continue;
								}
								case ')': {
									if (state[MD_LINK] == 0) {
										break;
									}

									sb.insert(0, '!');
									String s = sb.toString();
									if (item instanceof ImageItem && loadImages) {
										if (((ImageItem) item).getAltText() == null) {
											((ImageItem) item).setAltText(s);
											scheduleThumb((ImageItem) item, s);
										}
									}
									if (urls != null) urls.put(item, s);
									try {
										System.out.println("link " + item);
										form.safeInsert(thread, insert, item);
										insert++;
									} catch (RuntimeException e) {
										// nested links
										if (e == cancelException) throw e;
									}
									sb.setLength(0);
									
									l = c;
									state[MD_PARENTHESIS] --;
									if (state[MD_IMAGE] != 0) state[MD_IMAGE] --;
									if (-- state[MD_LINK] == 0) {
										item = null;
									}
									continue;
								}
								default:
									if (c < ' ' && state[MD_LENGTH] == 0) {
										l = c;
										continue;
									}
									break;
								}
							} else {
								state[MD_ESCAPE] = 0;
								if (c == '\t') {
									sb.append("    ");
									l = c;
									state[MD_LENGTH] ++;
									continue;
								}
							}
							
						}
						l = c;
						sb.append(c);
						state[MD_LENGTH] ++;
					}
					
					if (/*!noFormat && */sb.length() != 0) {
						insert = flush(thread, form, sb, insert, state);
					}
					if (d == len) break;
				}
				int e = body.indexOf('>', d);
	//			if (noFormat) {
	//				if (chars[d + 1] == 'p'
	//						|| (chars[d + 1] == '/' && chars[d + 2] == 'p')
	//						|| (chars[d + 1] == 'b' && chars[d + 2] == 'r')) {
	//					sb.append('\n');
	//				}
	//			} else
				{ // format by tags
					if (chars[d + 1] == '/') {
						if ((chars[d + 2] == 'b' && chars[d + 3] == '>')
								|| (chars[d + 2] == 's' && chars[d + 3] == 't')) {
							// </b> or </strong>
							state[MD_HTML_BOLD] --;
	//						state[1] &= ~Font.STYLE_BOLD;
						} else if (chars[d + 2] == 'h' && chars[d + 4] == '>') { // </h
							state[MD_HTML_HEADER] = 0;
							// </h1>
							if (chars[d + 3] == '1') state[1] &= ~Font.STYLE_BOLD;
						} else if ((chars[d + 2] == 'e' && chars[d + 3] == 'm')
							|| (chars[d + 2] == 'i' && chars[d + 3] == '>')) {
							// </em> or </i>
							state[MD_HTML_ITALIC] --;
//							state[1] &= ~Font.STYLE_ITALIC;
						} else if (chars[d + 2] == 'b' && chars[d + 3] == 'i' && chars[d + 4] == 'g') {
							// </small>
							state[MD_HTML_BIG] --;
						} else if ((chars[d + 2] == 's' && chars[d + 3] == 'u' && chars[d + 4] == 'b')
								|| (chars[d + 2] == 'i' && chars[d + 3] == 'n' && chars[d + 4] == 's')
								|| (chars[d + 2] == 'u' && chars[d + 3] == '>')) {
							// </sub>, </ins>, </u>
							state[MD_HTML_UNDERLINE] --;
							state[1] &= ~Font.STYLE_UNDERLINED;
						} else if (chars[d + 2] == 'p' && chars[d + 3] == '>') {
							// </p>
							state[MD_HTML_PARAGRAPH] --;
							sb.append('\n');
						} else if (chars[d + 2] == 's' && chars[d + 3] == 'm') {
							// </small>
						} else if (chars[d + 2] == 'a' && chars[d + 3] == '>') {
							// </a>
							state[MD_HTML_LINK] --;
							
							((StringItem) item).setText(sb.toString());
							
							form.safeInsert(thread, insert++, item);
							sb.setLength(0);
						} else {
							sb.append('<');
							e = d;
						}
					} else {
						if ((chars[d + 1] == 'b' && chars[d + 2] == '>')
								|| (chars[d + 1] == 's' && chars[d + 2] == 't')) {
							// <b> or <strong>
							state[MD_HTML_BOLD] ++;
//							state[1] |= Font.STYLE_BOLD;
						} else if (chars[d + 1] == 'h' && chars[d + 3] == '>') {
							// <h
							// <h1>
							state[MD_HTML_HEADER] = chars[d + 2] - '0';
							if (chars[d + 2] == '1') state[1] |= Font.STYLE_BOLD;
						} else if ((chars[d + 1] == 'e' && chars[d + 2] == 'm')
								|| (chars[d + 1] == 'i' && chars[d + 2] == '>')) {
							// <em> or <i>
							state[MD_HTML_ITALIC] ++;
//							state[1] |= Font.STYLE_ITALIC;
						} else if (chars[d + 1] == 'b' && chars[d + 2] == 'i' && chars[d + 3] == 'g') {
							// <big>
							state[MD_HTML_BIG] ++;
						} else if ((chars[d + 1] == 's' && chars[d + 2] == 'u' && chars[d + 3] == 'b')
								|| (chars[d + 1] == 'i' && chars[d + 2] == 'n' && chars[d + 3] == 's')
								|| (chars[d + 1] == 'u')) {
							// <sub>, <ins>, <u>
							state[MD_HTML_UNDERLINE] ++;
							state[1] |= Font.STYLE_UNDERLINED;
						} else if (chars[d + 1] == 'b' && chars[d + 2] == 'r') {
							// <br>
							sb.append('\n');
	//						lineBreak(state);
						} else if (chars[d + 1] == 'p' && (chars[d + 2] == ' ' || chars[d + 2] == '>')) {
							// <p>
							state[MD_HTML_PARAGRAPH] ++;
							if (state[MD_BREAKS] != 0) sb.append('\n');
						} else if (chars[d + 1] == 'i' && chars[d + 2] == 'm' && chars[d + 3] == 'g') {
							// <img>
							
							ImageItem img = new ImageItem("Image", null, 0, null);
							if (loadImages) {
								img.setLabel("");
								String url = body.substring(d + 4, e);
								int i;
								if ((i = url.indexOf("src=")) != -1) {
									url = url.substring(i + 4);
									if ((i = url.indexOf(' ')) != -1) {
										url = url.substring(0, i);
									}
									
									if (url.charAt(0) == '"')
										url = url.substring(1, url.length() - 1);
									
									url = "!".concat(url);
									
									img.setAltText(url);
									scheduleThumb(img, url);
								}
							}
							
							if (sb.length() != 0) {
								insert = flush(thread, form, sb, insert, state);
							}
	
							form.safeInsert(thread, insert++, img);
						} else if (chars[d + 1] == 's' && chars[d + 2] == 'm') {
							// <small>
						} else if (chars[d + 1] == 'a'
								&& (chars[d + 2] == ' ' || chars[d + 2] == '>')) {
							// <a>
							insert = flush(thread, form, sb, insert, state);
							state[MD_HTML_LINK] ++;

							item = new StringItem(null, "");
							((StringItem) item).setFont(getFont(state));
							item.setDefaultCommand(mdLinkCmd);
							item.setItemCommandListener(midlet);
							
							String url = body.substring(d + 2, e);
							int i;
							if ((i = url.indexOf("href=")) != -1) {
								url = url.substring(i + 5);
								if ((i = url.indexOf(' ')) != -1) {
									url = url.substring(0, i);
								}
								
								if (url.charAt(0) == '"')
									url = url.substring(1, url.length() - 1);
							}
							if (urls != null) urls.put(item, "!".concat(url));
						} else {
							sb.append('<');
							e = d;
						}
						
						// <li> ?
					}
				}
				d = body.indexOf('<', o = e + 1);
			}
		}
	}
	
	private static int flush(Thread thread, GHForm form, StringBuffer sb, int insert, int[] state) {
//		System.out.println("Flush: " + sb);
//		for (int i = 0; i < state.length; ++i) System.out.print(i + ": " + state[i] + ", ");
//		System.out.println(System.getProperty("kemulator.threadtrace"));
//		System.out.println();
		
		if (sb.length() == 0) return insert;

		if (state[MD_HEADER] != 0) {
			Spacer spacer = new Spacer(10, 4);
			spacer.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			form.safeInsert(thread, insert++, spacer);
		}
		boolean b = false;
		while (sb.charAt(sb.length() - 1) == ' ') {
			sb.setLength(sb.length() - 1);
			b = true;
		}
		StringItem s = new StringItem(null, sb.toString());
		Font f;
		s.setFont(f = getFont(state));
		form.safeInsert(thread, insert++, s);
		if (state[MD_HEADER] != 0 || state[MD_LINE] != 0) {
			Spacer spacer = new Spacer(10, 10);
			spacer.setLayout(Item.LAYOUT_NEWLINE_AFTER);
			form.safeInsert(thread, insert++, spacer);
			state[MD_LINE] = 0;
			state[MD_PARAGRAPH] = 1;
		} else if (b) {
			form.safeInsert(thread, insert++, new Spacer(f.charWidth(' '), f.getBaselinePosition()));
		}
		sb.setLength(0);
		return insert;
	}
	
	private static Font getFont(int[] state) {
		int face = 0, style = 0, size = 0;
		if (state[MD_GRAVE] != 0) {
			face = Font.FACE_MONOSPACE;
			style = Font.STYLE_BOLD;
			size = Font.SIZE_SMALL;
		} else {
			face = state[MD_FONT_FACE];
			style = state[MD_FONT_STYLE];
			size = state[MD_FONT_SIZE];
			if (state[MD_BOLD] != 0 || state[MD_HTML_BOLD] != 0) {
				style |= Font.STYLE_BOLD;
			}
			if (state[MD_ITALIC] != 0 || state[MD_HTML_ITALIC] != 0) {
				style |= Font.STYLE_ITALIC;
			}
			if (state[MD_HTML_BIG] != 0) {
				size = state[MD_HTML_BIG] == 1 ? Font.SIZE_MEDIUM : Font.SIZE_LARGE;
			}
//			if (state[MD_STRIKE] != 0) {
//				style |= Font.STYLE_UNDERLINED;
//			}
			int header = state[MD_HEADER];
			switch (header != 0 ? header : state[MD_HTML_HEADER]) {
			case 1:
				size = Font.SIZE_LARGE;
				style |= Font.STYLE_BOLD;
				break;
			case 2:
				size = Font.SIZE_MEDIUM;
				style |= Font.STYLE_BOLD;
				break;
			case 3:
				size = Font.SIZE_SMALL;
				style |= Font.STYLE_BOLD;
				break;
			case 4:
			case 5:
			case 6:
				size = Font.SIZE_SMALL;
				break;
			}
		}
		return getFont(face, style, size);
	}

	private static Font getFont(int face, int style, int size) {
		if (face == 0) {
//			int setSize = fontSize;
//			if (setSize == 0) {
//				size = size == Font.SIZE_LARGE ? Font.SIZE_MEDIUM : Font.SIZE_SMALL;
//			} else if (setSize == 2) {
//				size = size == Font.SIZE_SMALL ? Font.SIZE_MEDIUM : Font.SIZE_LARGE;
//			}
			
			if (size == Font.SIZE_SMALL) {
				if (style == Font.STYLE_BOLD) {
					return smallBoldFont;
				}
				if (style == Font.STYLE_ITALIC) {
					return smallItalicFont;
				}
				if (style == Font.STYLE_PLAIN) {
					return smallPlainFont;
				}
			}
			if (size == Font.SIZE_MEDIUM) {
				if (style == Font.STYLE_BOLD) {
					return medBoldFont;
				}
				if (style == Font.STYLE_ITALIC) {
					return medItalicFont;
				}
				if (style == (Font.STYLE_BOLD | Font.STYLE_ITALIC)) {
					return medItalicBoldFont;
				}
				if (style == Font.STYLE_PLAIN) {
					return medPlainFont;
				}
			}
			if (size == Font.SIZE_LARGE) {
				return largePlainFont;
			}
		}
		return Font.getFont(face, style, size);
	}
	
	// Markdown parser end

}
