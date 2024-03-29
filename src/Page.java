
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

public class Page implements Serializable {

	private final int intMaxRecords;
	private int intId; // id for the page
	private static int intSerial; // counting serial number to determine page id
	private String strClusteringKeyColumn;
	private Vector<Hashtable<String, Object>> vecData;
	private transient Hashtable<String, Object> firstRecord;
	private transient Hashtable<String, Object> lastRecord;

//	------------------------------------------------------(Constructor)------------------------------------------------------------------------------
	public int getMaxRowCountinPage() throws IOException {								// gettinng the max row count from properties file
		Properties prop=new Properties();
		FileInputStream propertiesFile= new FileInputStream("config.properties");
		prop.load(propertiesFile);
		return(Integer.parseInt(prop.getProperty("MaximumRowsCountinPage")));
	}

	
	public Page(String strClusteringKeyColumn) throws IOException {
		this.vecData = new Vector<Hashtable<String, Object>>();
		this.strClusteringKeyColumn = strClusteringKeyColumn;
		intId = intSerial++;
		intMaxRecords=getMaxRowCountinPage();

	}
//---------------------------------------------------------(Insert)-------------------------------------------------------------------
	
	/*
	 * inserting into a page by checking if it has a free space then insert the
	 * record into it then sort else remove the last element, insert the record in
	 * the hande and sort , then return the removed record to be inserted in another
	 * page
	 */
	

	public void sortPage() {

		vecData.sort(new compareHashtables(strClusteringKeyColumn));

	}

	public Hashtable<String, Object> insert (Hashtable<String, Object> htblQuery) {
		Hashtable<String, Object> htblLastRecord = null;
		if (isFull()) {
			htblLastRecord = vecData.lastElement();
			vecData.removeElement(htblLastRecord);
			Comparator comp = new compareHashtables(strClusteringKeyColumn);
			if (comp.compare(htblLastRecord, htblQuery) <= 0) {
				Hashtable<String, Object> temp = htblLastRecord;
				htblLastRecord = htblQuery;
				htblQuery = temp;
			}

		}
		vecData.add(htblQuery);
		sortPage();
		return htblLastRecord;

	}

//------------------------------------------------------(Update)------------------------------------------------------------------------

	// updating two records are equal /*before*/ /*After*/
		public static void update_helper(Hashtable<String, Object> htblRecord, Hashtable<String, Object> htblUpdate) {
			Set<String> colNames = htblUpdate.keySet();
			for (String s : colNames) {
				htblRecord.put(s, htblUpdate.get(s));
			}
		}
		
	public void update(String colType, String colName, String strKey, Hashtable<String, Object> htblUpdate)	throws DBAppException {
		int intCol;
		String strCol;
		double dblCol;
		boolean bolCol;
		Date datCol;
		try {
			switch (colType) {
				case "java.lang.Integer":
					intCol = (Integer.parseInt(strKey));
					for (int i = 0; i < vecData.size(); i++) {
						Hashtable<String, Object> htblRecord = vecData.get(i);
						if (htblRecord.get(colName).equals(intCol))
							update_helper(htblRecord, htblUpdate);
					}
					break;
				case "java.lang.String":
					strCol = strKey;
					for (int i = 0; i < vecData.size(); i++) {
						Hashtable<String, Object> htblRecord = vecData.get(i);
						if (htblRecord.get(colName).equals(strCol))
							update_helper(htblRecord, htblUpdate);
					}
					break;
				case "java.lang.Double":
					dblCol = (Double.parseDouble(strKey));
					for (int i = 0; i < vecData.size(); i++) {
						Hashtable<String, Object> htblRecord = vecData.get(i);
						if (htblRecord.get(colName).equals(dblCol))
							update_helper(htblRecord, htblUpdate);
					}
					break;
				case "java.lang.Boolean":
					bolCol = (Boolean.parseBoolean(strKey));
					for (int i = 0; i < vecData.size(); i++) {
						Hashtable<String, Object> htblRecord = vecData.get(i);
						if (htblRecord.get(colName).equals(bolCol))
							update_helper(htblRecord, htblUpdate);
					}
					break;
				case "java.lang.Date":
					try {
						datCol = new SimpleDateFormat("dd/MM/yyyy").parse(strKey);
						for (int i = 0; i < vecData.size(); i++) {
							Hashtable<String, Object> htblRecord = vecData.get(i);
							if (htblRecord.get(colName).equals(datCol))
								update_helper(htblRecord, htblUpdate);
						}
					} catch (Exception e) {
						throw new DBAppException("date is not in the correct format");
					}
					break;
					
			}
		} catch (Exception e) {
			throw new DBAppException("Key type mismatch!");
		}

	}
//	------------------------------------------------------(Delete)------------------------------------------------------------------------------
	
	
	/*
	 * to delete a record in the page, loop through all records and match it with
	 * the record in hand using the method
	 * "static boolean recordMatching(Hashtable<String,Object>htblQuery  ,  Hashtable<String,Object>htblRecord)"
	 * if the records match, delete and continue; (don't use for each)
	 */

	
	// checking if two records are equal
	public static boolean recordMatching(Hashtable<String, Object> htblQuery, Hashtable<String, Object> htblRecord) {
		Set<String> colNames = htblQuery.keySet();
		for (String s : colNames) {
			Comparable query = (Comparable) htblQuery.get(s);
			Comparable record = (Comparable) htblRecord.get(s);
			if (record==null || query.compareTo(record) != 0)
				return false;
		}
		return true;
	}
	public Vector<Integer> delete(Hashtable<String, Object> htblQuery) {
		Vector<Integer> vecDeletedInd = new Vector<>();
		for (int i = vecData.size() - 1; i >= 0; i--) {
			Hashtable<String, Object> htblRecord = vecData.get(i);
			boolean deleteRecord = recordMatching(htblQuery, htblRecord);
			if (deleteRecord) {
				vecData.remove(i);
				vecDeletedInd.add(i);
			}
		}
		return vecDeletedInd;
	}
	
//	-------------------------------------------------(Get special record)--------------------------------------------------------------------------------	

	public static boolean recordMatchingGet(Hashtable<String, Object> htblQuery, Hashtable<String, Object> htblRecord) {
		Set<String> colNames = htblQuery.keySet();
		
		for (String s : colNames) {
			String query = htblQuery.get(s).toString();
			String record = htblRecord.get(s).toString();
			if (record==null || !query.equals(record))
				return false;
		}
		return true;
	}

	public Hashtable<String, Object> get (Hashtable<String, Object> htblQuery) {

		for (int i = vecData.size() - 1; i >= 0; i--) {
			Hashtable<String, Object> htblRecord = vecData.get(i);
			boolean deleteRecord = recordMatchingGet(htblQuery, htblRecord);
			if (deleteRecord) {
				return(vecData.get(i));
			}
		}
		return null;
	}
//	-----------------------------------------------------------( Getters & setters )----------------------------------------------------------------------	

	public boolean isFull() {
		return vecData.size() == intMaxRecords;
	}

	public boolean isEmpty() {
		return vecData.size() == 0;
	}


	public Hashtable<String, Object> getRecord(int index) {
		return vecData.get(index);
	}

	public int size() {
		return vecData.size();
	}


	public int getIntId() {
		return intId;
	}

	public void setIntId(int intId) {
		this.intId = intId;
	}

	public static int getIntSerial() {
		return intSerial;
	}

	public static void setIntSerial(int intSerial) {
		Page.intSerial = intSerial;
	}

	public String getStrClusteringKeyColumn() {
		return strClusteringKeyColumn;
	}

	public void setStrClusteringKeyColumn(String strClusteringKeyColumn) {
		this.strClusteringKeyColumn = strClusteringKeyColumn;
	}

	public Vector<Hashtable<String, Object>> getVecData() {
		return vecData;
	}

	public void setVecData(Vector<Hashtable<String, Object>> vecData) {
		this.vecData = vecData;
	}

	public int getIntMaxRecords() {
		return intMaxRecords;
	}

	public Hashtable<String, Object> getFirstRecord() {
		return firstRecord;
	}

	public Hashtable<String, Object> getLastRecord() {
		return lastRecord;
	}
//	-----------------------------------------------( toString )-------------------------------------------------------------------------------------------
	public String toString() {
		return "\n page :" + intId + "\n records: " + vecData;
	}
	
//--------------------------------------------------------------------------------------------------------------------------------------------------
	
	static class compareHashtables implements Comparator<Hashtable<String, Object>> {
		String strClusteringKeyColumn = "";

		public compareHashtables(String s) {
			strClusteringKeyColumn = s;
		}

		@Override
		public int compare(Hashtable<String, Object> htblRecord1, Hashtable<String, Object> htblRecord2) {
			Comparable c1 = (Comparable) htblRecord1.get(strClusteringKeyColumn);
			Comparable c2 = (Comparable) htblRecord2.get(strClusteringKeyColumn);

			return c1.compareTo(c2);
		}

	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public void deleteByIndex(int ind) {
		vecData.remove(ind);
	}
	
	public void updateByIndex(int ind, Hashtable<String, Object> htblUpdate)	throws DBAppException {
		Hashtable<String, Object> htblRecord = vecData.get(ind);
			update_helper(htblRecord, htblUpdate);

	}

	public Hashtable getByIndex(int ind){
		return vecData.get(ind);
	}
//	-------------------------------------------------------------(New Methods)-------------------------------------------------------------------------------------
	public int getRecordOrder(Hashtable<String, Object> htblRecord) {
		Comparable c1 = (Comparable) htblRecord.get(strClusteringKeyColumn);		
		int count=0;
		Comparable htblTragetRecord=(Comparable)(htblRecord.get(strClusteringKeyColumn));
		for(Hashtable<String, Object> htbl:vecData) {
			if(((Comparable)(htbl.get(strClusteringKeyColumn))).compareTo(htblTragetRecord)==0) {
				break;
			}
			count++;
		}
		return count;
}
	

























	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public String generateBitStream (SQLTerm term) {
		String bitStream="";
		for (Hashtable htbl : vecData){
			if(satisfy(htbl,term))bitStream+="1";
			else bitStream+="0";
		}
		return bitStream;
	}
	public static boolean satisfy(Hashtable<String,Object>htbl,SQLTerm term)
	{
		String operator = term._strOperator;
		Comparable value = (Comparable) htbl.get(term._strColumnName);
		if(operator.equals("<")){
			return (value.compareTo(term._objValue)<0);
		}
		else if(operator.equals(">")){
			return (value.compareTo(term._objValue)>0);
		}
		else if(operator.equals("=")){
			return (value.compareTo(term._objValue)==0);
		}
		else if(operator.equals("<=")){
			return (value.compareTo(term._objValue)<=0);
		}
		else if(operator.equals(">=")){
			return (value.compareTo(term._objValue)>=0);
		}
		else{
			return (value.compareTo(term._objValue)!=0);
		}

	}
}
