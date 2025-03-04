
public abstract class PagedForm extends GHForm { // TODO
	
	int perPage = 30;
	int page = 1;

	public PagedForm(String title) {
		super(title);
	}
	
	void nextPage() {
		gotoPage(page + 1);
	}
	
	void prevPage() {
		gotoPage(page <= 1 ? 1 : page - 1);
	}
	
	void gotoPage(int n) {
		page = n;
		loaded = false;
		
		cancel();
		GH.midlet.start(GH.RUN_LOAD_FORM, this);
	}
	

}
