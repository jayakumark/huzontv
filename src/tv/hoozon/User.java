package tv.hoozon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.mail.MessagingException;

public class User {

	/**
	 * @param args
	 */
	
	private String designation;
	private String station; 
	private String display_name;
	private String acct_type;
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
	
	public User(String inc_station, String inc_designation)
	{
		station = inc_station;
		designation = inc_designation;
		valid = false;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT designation FROM people_" + station + " WHERE designation='" + designation + "'");
			if(rs.next())
			{
				display_name = rs.getString("display_name");
				acct_type = rs.getString("acct_type");
				job_function = rs.getString("job_function");
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
		
	}
	
	public boolean isValid()
	{
		return valid;
	}
	
	public String getDesignation()
	{
		return designation;
	}
	
	public String getStation()
	{
		return station;
	}
	
	public String getDisplayName()
	{
		return display_name;
	}

	public String getAccountType()
	{
		return acct_type;
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
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
