package tv.hoozon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.mail.MessagingException;

import org.json.JSONArray;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class User {

	/**
	 * @param args
	 */
	
	private String designation;
	//private String station; 
	private String display_name;
	private String job_function;
	private String twitter_handle;
	private String twitter_access_token;
	private String twitter_access_token_secret;
	private boolean active;
	private double homogeneity;
	private long last_alert_twitter;
	private long last_alert_facebook;
	private long facebook_uid;
	private String facebook_access_token;
	private long facebook_access_token_expires;
	private long facebook_account_id;
	private String facebook_account_name;
	private String facebook_account_access_token;
	boolean valid;
	//private TreeSet<String> stations;
	
	public User(String inc_twitter_handle, String constructor_type)
	{
		valid = false;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
			stmt = con.createStatement();
			String query_to_exec = "";
			if(constructor_type.equals("twitter_handle"))
				query_to_exec = "SELECT * FROM people WHERE twitter_handle='" + inc_twitter_handle + "'";
			else if (constructor_type.equals("designation"))
				query_to_exec = "SELECT * FROM people WHERE designation='" + designation + "'";
			
			rs = stmt.executeQuery(query_to_exec);
			if(rs.next())
			{
				display_name = rs.getString("display_name");
				job_function = rs.getString("job_function");
				designation = rs.getString("designation");
				
				if(rs.getString("twitter_handle") != null && !rs.getString("twitter_handle").isEmpty())
				{
					twitter_handle = rs.getString("twitter_handle");
					if(rs.getString("twitter_access_token") != null && !rs.getString("twitter_access_token").isEmpty() &&
							rs.getString("twitter_access_token_secret") != null && !rs.getString("twitter_access_token_secret").isEmpty())
					{
						twitter_access_token = rs.getString("twitter_access_token");
						twitter_access_token_secret = rs.getString("twitter_access_token_secret");
					}
					else
					{
						twitter_access_token = null;
						twitter_access_token_secret = null;
					}
				}
				else
				{	
					twitter_handle = null;
					twitter_access_token = null;
					twitter_access_token_secret = null;
				}
				
				active = rs.getBoolean("active");
				homogeneity = rs.getDouble("homogeneity"); // will be set to zero if null
				last_alert_twitter = rs.getLong("last_alert_twitter"); // default in DB is zero if no last_alert
				last_alert_facebook = rs.getLong("last_alert_facebook"); // default in DB is zero if no last_alert
				facebook_uid = rs.getLong("facebook_uid"); // will be set to zero if null
				
				if(rs.getString("facebook_access_token") != null && !rs.getString("facebook_access_token").isEmpty())
				{
					facebook_access_token = rs.getString("facebook_access_token");
				}
				
				facebook_access_token_expires = rs.getLong("facebook_access_token_expires"); // will be set to zero if null
				facebook_account_id = rs.getLong("facebook_account_id"); // will be set to zero if null
				
				if(rs.getString("facebook_account_name") != null && !rs.getString("facebook_account_name").isEmpty())
				{
					facebook_account_name = rs.getString("facebook_account_name");
				}
				
				if(rs.getString("facebook_account_access_token") != null && !rs.getString("facebook_account_access_token").isEmpty())
				{
					facebook_account_access_token = rs.getString("facebook_account_access_token");
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
				se.sendMail("SQLException in User constructor", "valid set to false. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@hoozon.tv");
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
					se.sendMail("SQLException in User constructor", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@hoozon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}  	
		
	}
	
	/*
	public User(String inc_designation)
	{
		designation = inc_designation;
		valid = false;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT designation FROM people WHERE designation='" + designation + "'");
			if(rs.next())
			{
				display_name = rs.getString("display_name");
				acct_type = rs.getString("acct_type");
				job_function = rs.getString("job_function");
				station
				if(rs.getString("twitter_handle") != null && !rs.getString("twitter_handle").isEmpty())
				{
					twitter_handle = rs.getString("twitter_handle");
					if(rs.getString("twitter_access_token") != null && !rs.getString("twitter_access_token").isEmpty() &&
							rs.getString("twitter_access_token_secret") != null && !rs.getString("twitter_access_token_secret").isEmpty())
					{
						twitter_access_token = rs.getString("twitter_access_token");
						twitter_access_token_secret = rs.getString("twitter_access_token_secret");
					}
					else
					{
						twitter_access_token = null;
						twitter_access_token_secret = null;
					}
				}
				else
				{	
					twitter_handle = null;
					twitter_access_token = null;
					twitter_access_token_secret = null;
				}
				
				active = rs.getBoolean("active");
				homogeneity = rs.getDouble("homogeneity"); // will be set to zero if null
				last_alert_twitter = rs.getLong("last_alert_twitter"); // default in DB is zero if no last_alert
				last_alert_facebook = rs.getLong("last_alert_facebook"); // default in DB is zero if no last_alert
				facebook_uid = rs.getLong("facebook_uid"); // will be set to zero if null
				
				if(rs.getString("facebook_access_token") != null && !rs.getString("facebook_access_token").isEmpty())
				{
					facebook_access_token = rs.getString("facebook_access_token");
				}
				
				facebook_access_token_expires = rs.getLong("facebook_access_token_expires"); // will be set to zero if null
				facebook_account_id = rs.getLong("facebook_account_id"); // will be set to zero if null
				
				if(rs.getString("facebook_account_name") != null && !rs.getString("facebook_account_name").isEmpty())
				{
					facebook_account_name = rs.getString("facebook_account_name");
				}
				
				if(rs.getString("facebook_account_access_token") != null && !rs.getString("facebook_account_access_token").isEmpty())
				{
					facebook_account_access_token = rs.getString("facebook_account_access_token");
				}
				valid = true;
			}
			else
				valid = false;
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			SimpleEmailer se = new SimpleEmailer();
			try {
				se.sendMail("SQLException in User constructor", "valid set to false. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@hoozon.tv");
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
					se.sendMail("SQLException in User constructor", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@hoozon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}  	
		
	}*/
	
	public boolean isValid()
	{
		return valid;
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
	
	public String getJobFunction()
	{
		return job_function;
	}
	
	public String getTwitterHandle()
	{
		return twitter_handle;
	}
	
	public String getTwitterAccessToken()
	{
		return twitter_access_token;
	}
	
	public String getTwitterAccessTokenSecret()
	{
		return twitter_access_token_secret;
	}
	
	public boolean isActive()
	{
		return active;
	}
	
	public double getHomogeneity()
	{
		return homogeneity;
	}
	
	public long getLastAlertTwitter()
	{
		return last_alert_twitter;
	}
	
	public long getLastAlertFacebook()
	{
		return last_alert_facebook;
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
	
	public long getFacebookSubaccountID()
	{
		return facebook_account_id;
	}
	
	public String getFacebookAccountName()
	{
		return facebook_account_name;
	}
	
	public String getFacebookAccountAccessToken()
	{
		return facebook_account_access_token;
	}
	
	public TreeSet<String> getStationsAsReporter()
	{
		TreeSet<String> returnset = new TreeSet<String>();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
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
				se.sendMail("SQLException in User.getStationsAsReporter", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@hoozon.tv");
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
					se.sendMail("SQLException in User.getStationsAsReporter", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@hoozon.tv");
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
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
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
				se.sendMail("SQLException in User.getStationsAsAdministrator", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@hoozon.tv");
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
					se.sendMail("SQLException in User.getStationsAsAdministrator", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@hoozon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}  	
		return returnset;
	}
	
	public JSONObject getJSONObject()
	{
		JSONObject response_jo = new JSONObject();
		try {
			response_jo.put("designation", getDesignation());
			response_jo.put("twitter_handle", getTwitterHandle());
			response_jo.put("display_name", getDisplayName());
			response_jo.put("job_function", getJobFunction());
			response_jo.put("stations_as_reporter", new JSONArray(getStationsAsReporter()));
			response_jo.put("stations_as_administrator", new JSONArray(getStationsAsAdministrator()));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response_jo;		
	}
	
	boolean setTwitterAccessTokenAndSecret(String access_token, String access_token_secret)
	{
		boolean returnval;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
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
				se.sendMail("SQLException in Endpoint setTwitterAccessTokenAndSecret", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@hoozon.tv");
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
					se.sendMail("SQLException in Endpoint setTwitterAccessTokenAndSecret", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@hoozon.tv");
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
