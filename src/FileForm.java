import java.util.Hashtable;

import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONObject;
import cc.nnproject.json.JSONStream;

public class FileForm extends GHForm {

	String repo, ref, path, downloadUrl;
	boolean privateAccess;
	long size;
	
	public FileForm(String url, String downloadUrl, String path, String repo, String ref) {
		super(path != null ? path : "");
		this.url = url;
		this.path = path;
		this.repo = repo;
		this.ref = ref;
		this.downloadUrl = downloadUrl;
		addCommand(GH.downloadCmd);
	}

	void loadInternal(Thread thread) throws Exception {
		StringBuffer sb = new StringBuffer();
		if (downloadUrl != null) {
			StringItem s;
			if (path != null) {
				String name = path;
				int i = name.lastIndexOf('/');
				if (i != -1) {
					name = name.substring(i + 1);
				}
				
				s = new StringItem(null, name);
				s.setFont(GH.largePlainFont);
				s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				append(s);
			}

			s = new StringItem(null, GH.L[Download], Item.BUTTON);
			s.setDefaultCommand(GH.downloadCmd);
			s.setItemCommandListener(GH.midlet);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			append(s);
		} else if (url != null) {
			sb.append(url);
		} else if (path != null) {
			sb.append("repos/").append(repo).append("/contents/").append(path).append('?');
		}
		if (ref != null) sb.append("ref=").append(ref);
		
		JSONStream r = GH.apiStream(sb.toString());
		try {
			r.expectNextTrim('{');
			if (!r.jumpToKey("name")) throw new Exception();
			String name = r.nextString();
			setTitle(name);
			
			if (!r.jumpToKey("path")) throw new Exception();
			path = r.nextString();
			
			if (!r.jumpToKey("size")) throw new Exception();
			size = JSONObject.getLong(r.nextNumber());

			if (!r.jumpToKey("download_url")) throw new Exception();
			downloadUrl = r.nextString();
			privateAccess = downloadUrl.indexOf("token") != -1;
			
			String ext = name.toLowerCase();
			int i;
			if ((i = ext.lastIndexOf('.')) != -1) {
				ext = ext.substring(i + 1);
			} else ext = null;

			if (size > 10240
					|| (!"md".equals(ext) && !"txt".equals(ext) && !GH.previewFiles)
					|| !r.jumpToKey("content")) {
				StringItem s;
				
				s = new StringItem(null, name);
				s.setFont(GH.largePlainFont);
				s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				append(s);
				
				s = new StringItem(null, GH.L[Download], Item.BUTTON);
				s.setDefaultCommand(GH.downloadCmd);
				s.setItemCommandListener(GH.midlet);
				s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				append(s);
				return;
			}
			
			String content = r.nextString();

			if (!r.jumpToKey("encoding")) throw new Exception();
			if ("base64".equals(r.nextString())) {
				int[] len = new int[1];
				content = new String(GH.decodeBase64(content.getBytes(), len), 0, len[0], "UTF-8");
			}
			
			if (!"md".equals(ext)) {
				StringItem item = new StringItem(null, content);
				item.setFont(GH.smallPlainFont);
				safeAppend(thread, item);
				return;
			}
			
			if (urls == null) {
				urls = new Hashtable();
			} else {
				urls.clear();
			}
			GH.parseMarkdown(thread, this, content, 0, urls);
		} finally {
			r.close();
		}
	}
	
	public String resolveUrl(String url) {
		if (!url.startsWith("/")) {
			int i = path.lastIndexOf('/');
			if (i != -1) {
				while (url.startsWith("../")) {
					url = url.substring(3);
					i = path.lastIndexOf('/', i - 1);
				}
				url = (i == -1 ? "" : path.substring(0, i + 1)).concat(url);
			}
		} else url = url.substring(1);
		
		return url;
	}

	public String fetchBlobUrl(String url) throws Exception {
		if (url != null) {
			url = resolveUrl(url);
			StringBuffer sb = new StringBuffer(repo);
			
			if (!privateAccess) {
				if (GH.apiMode == GH.API_GITEA) {
					String inst = GH.customApiUrl != null ? GH.customApiUrl : GH.GITEA_DEFAULT_API_URL;
					inst = inst.substring(0, inst.indexOf("/api"));
				
					sb.insert(0, inst).append("/raw/").append(ref)
					.append('/').append(url);
				} else {
					sb.insert(0, GH.GITHUB_RAW_URL).append('/').append(ref)
					.append('/').append(url);
				}
				return sb.toString();
			}
			
			sb.insert(0, "repos/").append("/contents/").append(url).append('?');
			if (ref != null) sb.append("ref=").append(ref);
			
			JSONStream r = GH.apiStream(sb.toString());
			try {
				r.expectNextTrim('{');
				if (!r.jumpToKey("download_url")) throw new Exception();
				return r.nextString();
			} finally {
				r.close();
			}
		}
		return null;
	}


}
