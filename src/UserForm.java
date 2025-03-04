import java.io.IOException;

import cc.nnproject.json.JSONObject;

public class UserForm extends GHForm {

	private String user;

	public UserForm(String user) {
		super(user);
		this.user = user;
	}

	void load() throws IOException {
		JSONObject r = (JSONObject) GH.api("users/".concat(user));
		append(r.toString());
	}

}
