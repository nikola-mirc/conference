package alfatec.dao.conference;

import java.sql.ResultSet;
import java.sql.SQLException;

import alfatec.dao.research.ResearchDAO;
import alfatec.dao.utils.Logging;
import alfatec.dao.utils.TableUtility;
import alfatec.model.conference.Collection;
import alfatec.model.research.Research;
import database.DatabaseTable;
import database.Getter;
import javafx.collections.ObservableList;
import util.BooleanUtil;

/**
 * DAO for table "collection".
 * 
 * Double-checked locking in singleton.
 * 
 * @author jelena
 *
 */
public class CollectionDAO {

	private static CollectionDAO instance;

	public static CollectionDAO getInstance() {
		if (instance == null)
			synchronized (CollectionDAO.class) {
				if (instance == null)
					instance = new CollectionDAO();
			}
		return instance;
	}

	private final TableUtility table;

	private Getter<Collection> getCollection;

	private CollectionDAO() {
		table = new TableUtility(new DatabaseTable("collection", "collection_id",
				new String[] { "from_conference", "for_special_issue", "research_id" }));
		getCollection = (ResultSet rs) -> {
			Collection collection = new Collection();
			try {
				collection.setCollectionID(rs.getLong(table.getTable().getPrimaryKey()));
				collection.setConferenceID(rs.getInt(table.getTable().getColumnName(1)));
				collection.setForSpecialIssue(BooleanUtil.parse(rs.getBoolean(table.getTable().getColumnName(2))));
				collection.setResearchID(rs.getLong(table.getTable().getColumnName(3)));
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return collection;
		};
	}

	public Collection createEntry(long researchID, int conferenceID, boolean forSpecialIssue) {
		Collection collection = table.create(new String[] {},
				new int[] { conferenceID, BooleanUtil.parse(forSpecialIssue) }, new long[] { researchID },
				getCollection);
		Logging.getInstance().change("add", "Marked\n\t"
				+ ResearchDAO.getInstance().getResearch(researchID).getResearchTitle() + "\nfor collection.");
		return collection;
	}

	public void deleteEntry(Collection collection) {
		table.delete(collection.getCollectionID());
		Logging.getInstance().change("remove",
				"Remove\n\t" + ResearchDAO.getInstance().getResearch(collection.getResearchID()).getResearchTitle()
						+ "\nfrom collection.");
	}

	/**
	 * Search table by primary key - id
	 */
	public Collection get(long collectionID) {
		return table.findBy(collectionID, getCollection);
	}

	/**
	 * @return all data from table (from all conferences)
	 */
	public ObservableList<Collection> getAll() {
		return table.getAll(getCollection);
	}

	/**
	 * @param conferenceID
	 * @return all data for specified conference
	 */
	public ObservableList<Collection> getAllForConference(int conferenceID) {
		return table.findBy(conferenceID, 1, getCollection);
	}

	/**
	 * @param conferenceID
	 * @return all data marked for special issue from specified conference
	 */
	public ObservableList<Collection> getAllForSpecialIssue(int conferenceID) {
		String[] columnNames = new String[] { table.getTable().getColumnName(1), table.getTable().getColumnName(2) };
		return table.findWhere(columnNames, new long[] { conferenceID, 1 }, getCollection);
	}

	public Collection getCollection(Research research) {
		try {
			return table.findBy(research.getResearchID(), 3, getCollection).get(0);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	public void updateConferenceID(Collection collection, int conferenceID) {
		table.update(collection.getCollectionID(), 1, conferenceID);
		collection.setConferenceID(conferenceID);
	}

	public void updateForSpecialIssue(Collection collection, boolean bool) {
		int special = collection.getForSpecialIssue();
		String type = special == 0 ? "add" : "remove";
		String description = special == 0
				? "Mark\n\t" + ResearchDAO.getInstance().getResearch(collection.getResearchID()).getResearchTitle()
						+ "\nfor "
				: "Remove\n\t" + ResearchDAO.getInstance().getResearch(collection.getResearchID()).getResearchTitle()
						+ "\nfrom ";
		table.update(collection.getCollectionID(), 2, BooleanUtil.parse(bool));
		collection.setIsForSpecialIssue(bool);
		Logging.getInstance().change(type, description + "special issue.");
	}

	public void updateResearchID(Collection collection, long researchID) {
		table.update(collection.getCollectionID(), 3, researchID);
		collection.setResearchID(researchID);
	}

}
