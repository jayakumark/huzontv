package tv.huzon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.mail.MessagingException;


import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class User {

	/**
	 * @param args
	 */
	
	// values directly from "people" table
	private String designation;
	private String display_name;
	private double homogeneity_studio;
	private double homogeneity_natural;
	private double homogeneity_artificial;
	private int twitter_alert_waiting_period;
	private int facebook_alert_waiting_period;
	private int twitter_delete_after;
	private int facebook_delete_after;
	private long twitter_last_alert;
	private long facebook_last_alert;
	private String twitter_handle;
	private String twitter_access_token;
	private String twitter_access_token_secret;
	private long facebook_uid;
	private String facebook_access_token;
	private long facebook_access_token_expires;
	private long facebook_page_id;
	private String facebook_page_name;
	private String facebook_page_access_token;
	private String weekend_expected_begin_string;
	private String weekend_expected_end_string;
	private String weekday_expected_begin_string;
	private String weekday_expected_end_string;
	private boolean anchor;
	private boolean weather;
	private boolean sports;
	private boolean reporter;
	
	// values collected from "stations" table in constructor
	private TreeSet<String> stations_appearing;
	private TreeSet<String> stations_as_admin;
		
	// additional values
	boolean valid;
		
	public User(String inc_twitter_handle, String constructor_type)
	{
		/*
		private TreeSet<String> facebook_page_ids;
		*/
		valid = false;
		ResultSet rs = null;  		Connection con = null; 		Statement stmt = null;  		ResultSet rs2 = null;  		Statement stmt2 = null;  		ResultSet rs3 = null; 		Statement stmt3 = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement();
			String query_to_exec = "";
			if(constructor_type.equals("twitter_handle"))
				query_to_exec = "SELECT * FROM people WHERE twitter_handle=' " + inc_twitter_handle + "'";
			else if (constructor_type.equals("designation"))
				query_to_exec = "SELECT * FROM people WHERE designation='" + designation + "'";
			
			rs = stmt.executeQuery(query_to_exec);
			if(rs.next())
			{
				designation = rs.getString("designation");
				display_name = rs.getString("display_name");
				homogeneity_studio = rs.getDouble("homogeneity_studio");
				homogeneity_natural = rs.getDouble("homogeneity_natural");
				homogeneity_artificial = rs.getDouble("homogeneity_artificial");
				twitter_alert_waiting_period = rs.getInt("twitter_alert_waiting_period");
				facebook_alert_waiting_period = rs.getInt("facebook_alert_waiting_period");
				twitter_delete_after = rs.getInt("twitter_delete_after");
				facebook_delete_after = rs.getInt("facebook_delete_after");
				twitter_last_alert = rs.getLong("twitter_last_alert");
				facebook_last_alert = rs.getLong("facebook_last_alert");
				twitter_handle = rs.getString("twitter_handle");
				twitter_access_token = rs.getString("twitter_access_token");
				twitter_access_token_secret = rs.getString("twitter_access_token_secret");
				facebook_uid = rs.getLong("facebook_uid");
				facebook_access_token = rs.getString("facebook_access_token");
				facebook_access_token_expires = rs.getLong("facebook_access_token_expires");
				facebook_page_id = rs.getLong("facebook_page_id");
				facebook_page_name = rs.getString("facebook_page_name");
				facebook_page_access_token = rs.getString("facebook_page_access_token");
				weekday_expected_begin_string = rs.getString("weekday_expected_begin");
				weekday_expected_end_string = rs.getString("weekday_expected_end");
				weekend_expected_begin_string = rs.getString("weekend_expected_begin");
				weekend_expected_end_string = rs.getString("weekend_expected_end");
				anchor = rs.getBoolean("anchor");
				weather = rs.getBoolean("weather");
				sports = rs.getBoolean("sports");
				reporter = rs.getBoolean("reporter");
				
				rs2 = null;
				stmt2 = con.createStatement();
				rs2 = stmt2.executeQuery("SELECT * FROM stations WHERE reporters like '% " + designation + " %'");
				stations_appearing = new TreeSet<String>();
				while(rs2.next())
				{
					stations_appearing.add(rs2.getString("call_letters"));
				}
				
				rs3 = null;
				stmt3 = con.createStatement();
				rs3 = stmt3.executeQuery("SELECT * FROM stations WHERE administrators like '% " + designation + " %'");
				stations_as_admin = new TreeSet<String>();
				while(rs3.next())
				{
					stations_as_admin.add(rs3.getString("call_letters"));
				}
				valid = true;
			}
			else
			{
				valid = false;
			}
		}
		catch(SQLException sqle)
		{
			System.out.println("SQLException in User constructor: " + sqle.getMessage());
			sqle.printStackTrace();
			SimpleEmailer se = new SimpleEmailer();
			try {
				se.sendMail("SQLException in User constructor", "valid set to false. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } 
				if (rs2  != null){ rs2.close(); } if (stmt2  != null) { stmt2.close(); } 
				if (rs3  != null){ rs3.close(); } if (stmt3  != null) { stmt3.close(); } 
				if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
				SimpleEmailer se = new SimpleEmailer();
				try {
					se.sendMail("SQLException in User constructor", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}  	
		
	}
	
	public boolean isValid()
	{
		return valid;
	}
	
	public boolean isAnchor()
	{
		return anchor;
	}
	
	public boolean isWeather()
	{
		return weather;
	}
	
	public boolean isSports()
	{
		return sports;
	}
	
	public boolean isReporter()
	{
		return reporter;
	}
	
	public String getDesignation()
	{
		return designation;
	}
	
	/*
	public TreeSet<String> getStations()
	{
		return stations;
	}*/
	
	public String getDisplayName()
	{
		return display_name;
	}
	
	public double getHomogeneityArtificial()
	{
		return homogeneity_artificial;
	}
	
	public double getHomogeneityNatural()
	{
		return homogeneity_natural;
	}
	
	public long getFacebookUID()
	{
		return facebook_uid;
	}
	
	public String getFacebookAccessToken()
	{
		return facebook_access_token;
	}
	
	public long getFacebookAccessTokenExpires()
	{
		return facebook_access_token_expires;
	}
	
	/*
	public TreeSet<String> getStationsAsReporter()
	{
		TreeSet<String> returnset = new TreeSet<String>();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM stations WHERE reporters like ' %" + designation + "% '");
			while(rs.next())
			{
				returnset.add(rs.getString("call_letters"));
			}
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			SimpleEmailer se = new SimpleEmailer();
			try {
				se.sendMail("SQLException in User.getStationsAsReporter", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
				SimpleEmailer se = new SimpleEmailer();
				try {
					se.sendMail("SQLException in User.getStationsAsReporter", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}  	
		return returnset;
	}
	
	public TreeSet<String> getStationsAsAdministrator()
	{
		TreeSet<String> returnset = new TreeSet<String>();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM stations WHERE administrators like ' %" + designation + "% '");
			while(rs.next())
			{
				returnset.add(rs.getString("call_letters"));
			}
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			SimpleEmailer se = new SimpleEmailer();
			try {
				se.sendMail("SQLException in User.getStationsAsAdministrator", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
				SimpleEmailer se = new SimpleEmailer();
				try {
					se.sendMail("SQLException in User.getStationsAsAdministrator", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}  	
		return returnset;
	}
	*/
	
	public JSONObject getJSONObject() // for now, returns EVERYTHING. Even secret tokens and stuff.
	{
		JSONObject response_jo = new JSONObject();
		try {
			response_jo.put("designation", getDesignation());
			response_jo.put("display_name", getDisplayName());
			response_jo.put("stations_as_reporter", new JSONArray(stations_appearing));
			response_jo.put("stations_as_administrator", new JSONArray(stations_as_admin));
			response_jo.put("anchor", isAnchor());
			response_jo.put("weather", isWeather());
			response_jo.put("sports", isSports());
			response_jo.put("reporter", isReporter());
			response_jo.put("homogeneity_studio", homogeneity_studio);
			response_jo.put("homogeneity_natural", homogeneity_natural);
			response_jo.put("homogeneity_artificial", homogeneity_artificial);
			response_jo.put("weekend_expected_begin", weekend_expected_begin_string);
			response_jo.put("weekend_expected_end", weekend_expected_end_string);
			response_jo.put("weekday_expected_begin", weekday_expected_begin_string);
			response_jo.put("weekday_expected_end", weekday_expected_end_string);
			response_jo.put("twitter_handle", twitter_handle);
			response_jo.put("twitter_access_token", twitter_access_token);
			response_jo.put("twitter_access_token_secret", twitter_access_token_secret);
			response_jo.put("twitter_handle", twitter_handle);
			response_jo.put("facebook_uid", facebook_uid);
			response_jo.put("facebook_page_id", facebook_page_id);
			response_jo.put("facebook_access_token", facebook_access_token);
			response_jo.put("facebook_access_token_expires", facebook_access_token_expires);
			response_jo.put("twitter_alert_waiting_period", twitter_alert_waiting_period);
			response_jo.put("facebook_alert_waiting_period", facebook_alert_waiting_period);
			response_jo.put("twitter_delete_after", twitter_delete_after);
			response_jo.put("facebook_delete_after", facebook_delete_after);
			response_jo.put("twitter_last_alert", twitter_last_alert);
			response_jo.put("facebook_last_alert", facebook_last_alert);
			response_jo.put("facebook_page_id", facebook_page_id);
			response_jo.put("facebook_page_name", facebook_page_name);
			response_jo.put("facebook_page_access_token", facebook_page_access_token);
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response_jo;		
	}
	
	public String getTwitterAccessToken()
	{
		return twitter_access_token;
	}
	
	boolean setTwitterAccessTokenAndSecret(String access_token, String access_token_secret)
	{
		boolean returnval;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM people WHERE designation='" + designation + "'"); 
			while(rs.next())
			{
				rs.updateString("twitter_access_token", access_token);
				rs.updateString("twitter_access_token_secret", access_token_secret);
				rs.updateRow();
			}
			returnval = true;
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			returnval = false;
			SimpleEmailer se = new SimpleEmailer();
			try {
				se.sendMail("SQLException in Endpoint setTwitterAccessTokenAndSecret", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
				SimpleEmailer se = new SimpleEmailer();
				try {
					se.sendMail("SQLException in Endpoint setTwitterAccessTokenAndSecret", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}  	
		return returnval;
	}
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
