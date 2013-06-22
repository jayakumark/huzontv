package tv.huzon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.mail.MessagingException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;


import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class User implements java.lang.Comparable<User> {

	/**
	 * @param args
	 */
	
	// values directly from "people" table
	private String designation;
	private String display_name;
	private String email;
	private double homogeneity;
	private boolean twitter_active;
	private boolean facebook_active;
	private int twitter_alert_waiting_period;
	private int facebook_alert_waiting_period;
	private long twitter_last_alert;
	private long facebook_last_alert;
	private long twitter_last_alert_test;
	private long facebook_last_alert_test;
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
	private boolean global_admin;
	
	// values collected from "stations" table in constructor
	//private TreeSet<String> stations_appearing;
	//private TreeSet<String> stations_as_admin;
		
	// additional values
	boolean valid;
		
	public User(String inc_des_or_twit, String constructor_type)
	{
		//System.out.println("User(): entering inc_des_or_twit=" + inc_des_or_twit + " and constructor_type=" + constructor_type);
		//System.out.println("Creating user from inc_des_or_twit=" + inc_des_or_twit + " and constructor_type=" + constructor_type);
		valid = false;
		ResultSet rs = null;  		Connection con = null; 		Statement stmt = null;  	
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement();
			String query_to_exec = "";
			if(constructor_type.equals("twitter_handle"))
				query_to_exec = "SELECT * FROM people WHERE twitter_handle='" + inc_des_or_twit + "'";
			else if (constructor_type.equals("designation"))
				query_to_exec = "SELECT * FROM people WHERE designation='" + inc_des_or_twit + "'";
			System.out.println("User(): query_to_exec=" + query_to_exec);
			rs = stmt.executeQuery(query_to_exec);
			if(rs.next())
			{
				designation = rs.getString("designation");
				display_name = rs.getString("display_name");
				email = rs.getString("email");
				homogeneity = rs.getDouble("homogeneity");
				twitter_alert_waiting_period = rs.getInt("twitter_alert_waiting_period");
				facebook_alert_waiting_period = rs.getInt("facebook_alert_waiting_period");
				twitter_active = rs.getBoolean("twitter_active");
				facebook_active = rs.getBoolean("facebook_active");
				twitter_last_alert = rs.getLong("twitter_last_alert");
				facebook_last_alert = rs.getLong("facebook_last_alert");
				twitter_last_alert_test = rs.getLong("twitter_last_alert_test");
				facebook_last_alert_test = rs.getLong("facebook_last_alert_test");
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
				global_admin = rs.getBoolean("global_admin");
				valid = true;
			}
			else
			{
				System.out.println("User constructor: valid = false for inc_des_or_twit=" + inc_des_or_twit + " and constructor_type=" + constructor_type);
				valid = false;
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			System.out.println("SQLException in User constructor: " + sqle.getMessage());
			sqle.printStackTrace();
			SimpleEmailer se = new SimpleEmailer();
			try {
				se.sendMail("SQLException in User constructor", "valid set to false  for inc_des_or_twit=" + inc_des_or_twit + " and constructor_type=" + constructor_type + ". message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } 
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
		//System.out.println("User(): exiting inc_des_or_twit=" + inc_des_or_twit + " and constructor_type=" + constructor_type);
	}
	
	public boolean isWithinFacebookWindow(long frame_millis, boolean simulation)
	{
		if(frame_millis - getLastFacebookAlert(simulation) < getFacebookWaitingPeriodInMillis())
			return true;
		else
			return false;
	}
	
	public boolean isWithinTwitterWindow(long frame_millis, boolean simulation)
	{
		if(frame_millis - getLastTwitterAlert(simulation) < getTwitterWaitingPeriodInMillis())
			return true;
		else
			return false;
	}
	
	public long getFacebookPageID()
	{
		return facebook_page_id;
	}
	
	public String getFacebookPageAccessToken()
	{
		return facebook_page_access_token;
	}
	
	public String getEmail()
	{
		return email;
	}
	
	public boolean isGlobalAdmin()
	{
		return global_admin;
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
	
	public String getTwitterHandle()
	{
		return twitter_handle;
	}
	
	public boolean isTwitterActive()
	{
		return twitter_active;
	}

	public boolean isFacebookActive()
	{
		return facebook_active;
	}
	
	public long getLastTwitterAlert(boolean simulation)
	{
		if(simulation)
			return twitter_last_alert_test;
		return twitter_last_alert;
	}
	
	public long getLastFacebookAlert(boolean simulation)
	{
		if(simulation)
			return facebook_last_alert_test;
		return facebook_last_alert;
	}
	
	public String getDisplayName()
	{
		return display_name;
	}
	
	public double getHomogeneity()
	{
		if(!valid)
			return 2;
		return homogeneity;
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
	
	boolean setLastAlert(long alert_ts, String social_type, boolean simulation)
	{
		if(!(social_type.equals("facebook") || social_type.equals("twitter")))
		{
			System.out.println("User.setLastAlert: social type was not \"facebook\" or \"twitter\". returning false.");
			return false;
		}
		boolean returnval = false; 
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM people WHERE designation='" + designation + "' "); 
			while(rs.next())
			{
				if(social_type.equals("facebook"))
				{
					if(simulation)
						rs.updateLong("facebook_last_alert_test", alert_ts);
					else
						rs.updateLong("facebook_last_alert", alert_ts);
				}
				else												// we can just say "else" because we checked value of social_type above
				{
					if(simulation)
						rs.updateLong("twitter_last_alert_test", alert_ts);
					else
						rs.updateLong("twitter_last_alert", alert_ts);
				}
				rs.updateRow();
			}
			returnval = true;
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			returnval = false;
			SimpleEmailer se = new SimpleEmailer();
			try {
				se.sendMail("SQLException in User.setLastAlert", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
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
					se.sendMail("SQLException in User.setLastAlert", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}  	
		return returnval;
	}
	
	public long getFacebookWaitingPeriodInMillis()
	{
		return facebook_alert_waiting_period*1000;
	}
	
	public long getTwitterWaitingPeriodInMillis()
	{
		return twitter_alert_waiting_period*1000;
	}
	
	public JSONObject getJSONObject() // for now, returns EVERYTHING. Even secret tokens and stuff.
	{
		JSONObject response_jo = new JSONObject();
		try {
			response_jo.put("designation", getDesignation());
			response_jo.put("display_name", getDisplayName());
			response_jo.put("anchor", isAnchor());
			response_jo.put("weather", isWeather());
			response_jo.put("sports", isSports());
			response_jo.put("reporter", isReporter());
			response_jo.put("homogeneity", homogeneity);
			response_jo.put("weekend_expected_begin", weekend_expected_begin_string);
			response_jo.put("weekend_expected_end", weekend_expected_end_string);
			response_jo.put("weekday_expected_begin", weekday_expected_begin_string);
			response_jo.put("weekday_expected_end", weekday_expected_end_string);
			response_jo.put("twitter_handle", twitter_handle);
			response_jo.put("twitter_access_token", twitter_access_token);
			response_jo.put("twitter_access_token_secret", twitter_access_token_secret);
			response_jo.put("twitter_handle", twitter_handle);
			if(facebook_uid != 0)
				response_jo.put("facebook_uid", facebook_uid);
			if(!facebook_access_token.isEmpty())
				response_jo.put("facebook_access_token", facebook_access_token);
			if(facebook_access_token_expires != 0)
				response_jo.put("facebook_access_token_expires", facebook_access_token_expires);
			if(facebook_page_id != 0)
				response_jo.put("facebook_page_id", facebook_page_id);
			if(!facebook_page_name.isEmpty())
				response_jo.put("facebook_page_name", facebook_page_name);
			if(!facebook_page_access_token.isEmpty())
				response_jo.put("facebook_page_access_token", facebook_page_access_token);
			response_jo.put("facebook_alert_waiting_period", facebook_alert_waiting_period);
			response_jo.put("facebook_active", facebook_active);
			response_jo.put("facebook_last_alert", facebook_last_alert);
			response_jo.put("twitter_alert_waiting_period", twitter_alert_waiting_period);
			response_jo.put("twitter_active", twitter_active);
			response_jo.put("twitter_last_alert", twitter_last_alert);
			
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
	
	public String getTwitterAccessTokenSecret()
	{
		return twitter_access_token_secret;
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
			rs.close();
			stmt.close();
			con.close();
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
	
	boolean resetFacebookCredentialsInDB()
	{
		boolean reset_top_level = setFacebookAccessTokenExpiresAndUID("", 0L, 0L);
		boolean reset_sub_account = resetFacebookSubAccountIdNameAndAccessToken();
		if(reset_top_level && reset_sub_account)
			return true;
		return false;
	}
	
	boolean resetTwitterCredentialsInDB()
	{
		boolean reset = setTwitterAccessTokenAndSecret("","");
		if(reset)
			return true;
		else 
			return false;
	}
	
	boolean setFacebookAccessTokenExpiresAndUID(String access_token, long expires_timestamp, long fb_uid)
	{
		boolean returnval = false;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM people WHERE designation='" + designation + "' "); 
			if(rs.next())
			{
				rs.updateString("facebook_access_token", access_token);
				rs.updateLong("facebook_access_token_expires", expires_timestamp);
				rs.updateLong("facebook_uid", fb_uid);
				rs.updateRow();
				returnval = true;
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			SimpleEmailer se = new SimpleEmailer();
			try {
				se.sendMail("SQLException in Endpoint setFacebookAccessTokenExpiresAndUID", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
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
					se.sendMail("SQLException in Endpoint setFacebookAccessTokenExpiresAndUID", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}  	
		return returnval;
	}
	
	public boolean facebookTopLevelIsLinked()
	{
		if(facebook_uid != 0 && !facebook_access_token.isEmpty())
		{
			// FIXME -> should check that these are still valid against Facebook
			// for now, we're assuming that they are
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public JSONObject getProfileFromFacebook(String access_token)
	{
		JSONObject jsonresponse = new JSONObject();
		
		try
		{
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet("https://graph.facebook.com/me?access_token=" + access_token);
			HttpResponse response;
			
			try 
			{
				response = client.execute(request);
				// Get the response
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				String text = "";
				String line = "";
				while ((line = rd.readLine()) != null) {
					text = text + line;
				} 
				jsonresponse = new JSONObject(text);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
		catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonresponse;
	}
	
	
	public JSONArray getSubAccountsFromFacebook()
	{
		// if returnvalue == null, then facebook access token was null, empty, invalid or we couldn't reach the server.
		// if returnvalue is empty, then we reached facebook successfully, but there were no subaccounts.
		if(facebook_access_token == null || facebook_access_token.isEmpty())
		{
			return null;
		}
		JSONArray jsonresponse = new JSONArray();
		try
		{
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet("https://graph.facebook.com/me/accounts?access_token=" + facebook_access_token);
			HttpResponse response;
			
			try 
			{
				response = client.execute(request);
				// Get the response
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				String text = "";
				String line = "";
				while ((line = rd.readLine()) != null) {
					text = text + line;
				} 
				System.out.println("Endpoint.getFacebookSubAccounts(): response to https://graph.facebook.com/me/accounts?access_token=" + facebook_access_token + "=" + text);
				JSONObject jo = new JSONObject(text);
				jsonresponse = jo.getJSONArray("data");
			} catch (ClientProtocolException e) {
				jsonresponse = null;
				e.printStackTrace();
			} catch (IOException e) {
				jsonresponse = null;
				e.printStackTrace();
			}
		}	
		catch (JSONException e) {
			jsonresponse = null;
			e.printStackTrace();
		}
		return jsonresponse;
	}
	
	public JSONObject getFacebookSubAccount()
	{
		JSONObject return_jo = new JSONObject();
		try{
			if(facebook_page_id == 0)
			{
				return_jo.put("response_status", "error");
				return_jo.put("message", "FB subaccount not set in the database");
			}
			else if (facebook_page_name == null || facebook_page_name.isEmpty())
			{
				return_jo.put("response_status", "error");
				return_jo.put("message", "FB subaccount id is set, but the subaccount name is empty. Weird.");
			}
			else if (facebook_page_access_token == null || facebook_page_access_token.isEmpty())
			{
				return_jo.put("response_status", "error");
				return_jo.put("message", "FB subaccount id and name are set, but the subaccount facebook_page_access_token is empty. Weird.");
			}
			else
			{
				return_jo.put("response_status", "success");
				return_jo.put("facebook_page_id", facebook_page_id);
				return_jo.put("facebook_page_access_token", facebook_page_access_token);
				return_jo.put("facebook_page_name", facebook_page_name);
			}
		}	
		catch (JSONException e) {
			return_jo = null;
			e.printStackTrace();
		}
		return return_jo;
	}
	
	boolean resetFacebookSubAccountIdNameAndAccessToken()
	{
		boolean returnval = false;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM people WHERE designation='" + designation + "' "); 
			if(rs.next())
			{
				rs.updateString("facebook_page_access_token", "");
				rs.updateString("facebook_page_name", "");
				rs.updateLong("facebook_page_id", 0);
				rs.updateRow();
				returnval = true;
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			SimpleEmailer se = new SimpleEmailer();
			try {
				se.sendMail("SQLException in Endpoint resetFacebookSubAccountIdNameAndAccessToken", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
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
					se.sendMail("SQLException in Endpoint resetFacebookSubAccountIdNameAndAccessToken", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}  	
		return returnval;
	}
	
	boolean setFacebookSubAccountIdNameAndAccessToken(String id, JSONArray fb_subaccounts_ja)
	{
		String name = "";
		String subaccount_access_token = "";
		long id_long = 0L;
		try
		{
			for(int x =0; x < fb_subaccounts_ja.length(); x++)
			{
				if(fb_subaccounts_ja.getJSONObject(x).getLong("id") == (new Long(Long.parseLong(id)).longValue()))
				{
					name = fb_subaccounts_ja.getJSONObject(x).getString("name");
					subaccount_access_token = fb_subaccounts_ja.getJSONObject(x).getString("access_token");
					id_long =  (new Long(Long.parseLong(id)).longValue());
				}
			}
		}	
		catch (JSONException e) {
			e.printStackTrace();
		}
		
		boolean returnval = false;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM people WHERE designation='" + designation + "' "); 
			if(rs.next())
			{
				rs.updateString("facebook_page_access_token", subaccount_access_token);
				rs.updateString("facebook_page_name", name);
				rs.updateLong("facebook_page_id", id_long);
				rs.updateRow();
				returnval = true;
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			SimpleEmailer se = new SimpleEmailer();
			try {
				se.sendMail("SQLException in Endpoint setFacebookSubAccountIdNameAndAccessToken", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
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
					se.sendMail("SQLException in Endpoint setFacebookSubAccountIdNameAndAccessToken", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}  	
		return returnval;
	}
	
	public boolean deleteFacebookPost(String item_id)
	{
		 boolean successful = false;
		try {
			HttpClient httpClient = new DefaultHttpClient();
			HttpDelete hd = new HttpDelete("https://graph.facebook.com/" + item_id + "?access_token=" + getFacebookSubAccount().getString("facebook_page_access_token"));
			HttpResponse response = httpClient.execute(hd);
			int statusCode = response.getStatusLine().getStatusCode();
	        successful = statusCode == 200 ? true : false;
			//String responseBody = EntityUtils.toString(response.getEntity());
			//System.out.println("Endpoint.deleteFacebookPost(): responsebody=" + responseBody);
			//response_from_facebook = new JSONObject(responseBody);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return successful;
	}
	
	
	public int compareTo(User o) // this sorts by designation alphabetically
	{
	    String otherdesignation = ((User)o).getDesignation();
	    int x = otherdesignation.compareTo(designation);
	    if(x >= 0) // this is to prevent equals
	    	return 1;
	    else
	    	return -1;
	}
	
}
