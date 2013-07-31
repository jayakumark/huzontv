package tv.huzon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

import facebook4j.Facebook;
import facebook4j.FacebookFactory;
import facebook4j.auth.AccessToken;

public class Alert implements java.lang.Comparable<Alert> {

	long id;
	long timestamp_in_ms;
	String timestamp_hr;
	String social_type;
	String image_url;
	Timestamp creation_timestamp;
	String designation;
	String station_str;
	String actual_text;
	String social_item_id;
	String created_by;
	Station station_object;
	
	private DataSource datasource;
	
	public Alert(long inc_id)
	{
		try {
			Context envCtx = (Context) new InitialContext().lookup("java:comp/env");
			datasource = (DataSource) envCtx.lookup("jdbc/huzondb");
		}
		catch (NamingException e) {
			e.printStackTrace();
		}
		
		id = inc_id;
		
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = datasource.getConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM alerts WHERE id=" + inc_id); // get the frames in the time range
			
			if(rs.next())
			{	
				timestamp_in_ms = rs.getLong("timestamp_in_ms");
				timestamp_hr = rs.getString("timestamp_hr");
				social_type = rs.getString("social_type");
				image_url = rs.getString("image_url");
				creation_timestamp = rs.getTimestamp("creation_timestamp");
				designation = rs.getString("designation");
				station_str = rs.getString("station");
				station_object = new Station(station_str);
				actual_text = rs.getString("actual_text");
				social_item_id = rs.getString("social_item_id");
				created_by = rs.getString("created_by");
			}
			else
			{
				id = -1L; // indicates failed lookup
				(new Platform()).addMessageToLog("Error in Alert.constructor: Failed lookup for id=" + inc_id);
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("SQLException in Alert.constructor: Error getting table row. message=" +sqle.getMessage());
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				sqle.printStackTrace();
			}
		}   		
	}
	
	long getID()
	{
		return id;
	}
	
	String getSocialType()
	{
		return social_type;
	}

	String getImageURL()
	{
		return image_url;
	}
	
	long getTimestampInMillis()
	{
		return timestamp_in_ms;
	}
	
	Timestamp getTimestamp()
	{
		return creation_timestamp;
	}
	
	long getAgeInMillis()
	{
		return (new Date().getTime() - getTimestampInMillis());
	}
	
	String getDesignation()
	{
		return designation;
	}
	
	String getStation()
	{
		return station_str;
	}
	
	String getActualText()
	{
		return actual_text;
	}
	
	String getSocialItemID()
	{
		return social_item_id;
	}
	
	User getCreatedByUser()
	{
		if(created_by.isEmpty())
			return (new User(designation, "designation")); // early on, the designation was also the creator. If the created_by value is empty, assume this.
		else
			return (new User(created_by, "designation")); 
	}
	
	boolean setDeletionTimestamp()
	{
		boolean returnval = false;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = datasource.getConnection();
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM alerts WHERE id=" + id); // get the frames in the time range
			
			if(rs.next())
			{	
				rs.updateTimestamp("deletion_timestamp", new Timestamp(System.currentTimeMillis()));
				rs.updateRow();
				returnval = true;
			}
			else
			{
				returnval = false;
				(new Platform()).addMessageToLog("Error in Alert.setDeletionTimestamp: Failed setDeletionTimestamp couldn't find row id=" + id);
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("SQLException in Alert.setDeletionTimestamp: message=" +sqle.getMessage());
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				sqle.printStackTrace();
			}
		}   		
		return returnval;
	}
	
	boolean deleteSocialItem() // this is not a deletion of the row from the database
	{
		boolean successful = false;
		if(social_type.equals("facebook"))
		{
			 try {
				 boolean communication_successful= false;
				 HttpClient httpClient = new DefaultHttpClient();
				 System.out.println("Alert.deleteSocialItem(): https://graph.facebook.com/" + getSocialItemID() + "?access_token=" + getCreatedByUser().getFacebookPageAccessToken());
				 HttpDelete hd = new HttpDelete("https://graph.facebook.com/" + getSocialItemID() + "?access_token=" + getCreatedByUser().getFacebookPageAccessToken());
				 HttpResponse response = httpClient.execute(hd);
				 int statusCode = response.getStatusLine().getStatusCode();
				 communication_successful = statusCode == 200 ? true : false; 
				 if(!communication_successful)
				 {
					 String response_from_facebook = EntityUtils.toString(response.getEntity());
					 System.out.println("Alert.deleteSocialItem(): communication with Facebook was unsuccessful. response=" + response_from_facebook);
					 successful = false;
				 }
				 else
				 {
					 String response_from_facebook = EntityUtils.toString(response.getEntity());
					 System.out.println("Alert.deleteSocialItem(): communication with Facebook was successful. response=" + response_from_facebook);
					 if(response_from_facebook.equals("true"))
						 successful = true;
					 else
						 successful = false;
				 }
				 
			 } catch (ClientProtocolException e) {
				 e.printStackTrace();
			 } catch (IOException e) {
				 e.printStackTrace();
			 } catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			//catch (JSONException e) {
			//	e.printStackTrace();
			//} 
		}
		else if(social_type.equals("twitter"))
		{
			Twitter twitter = new Twitter();
			JSONObject response_from_twitter = twitter.deleteStatus(getCreatedByUser().getTwitterAccessToken(), getCreatedByUser().getTwitterAccessTokenSecret(), getSocialItemID());
			if(response_from_twitter.has("errors"))
				successful = false;
			else
				successful = true; 
		}
		if(successful)
			setDeletionTimestamp();
		return successful;
	}
	
	public long getRedirectCount(boolean sansbot)
	{
		long returnval = 0;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = datasource.getConnection();
			stmt = con.createStatement();
			if(sansbot)
			{
				rs = stmt.executeQuery("SELECT COUNT(*) FROM redirects_" + station_object.getCallLetters() + " WHERE alert_id=" + id + 
						" AND `user_agent` NOT LIKE '%bot%' AND `user_agent` NOT LIKE '%UnwindFetchor%' AND `user_agent` NOT LIKE '%JS-Kit%'" +
						" AND `user_agent` NOT LIKE '%NING%' AND `user_agent` NOT LIKE '%facebookexternalhit%' AND `user_agent` NOT LIKE '%RockmeltEmbedder%' " +
						"AND `user_agent`!='' AND `user_agent` NOT LIKE '%LongURL API%' AND `user_agent` NOT LIKE '%PycURL%' AND `user_agent` NOT LIKE '%Java/%' " +
						"AND `user_agent` NOT LIKE '%spider%'  AND `user_agent` NOT LIKE '%Spider%' AND `user_agent` NOT LIKE '%Butterfly%'");
			}
			else
				rs = stmt.executeQuery("SELECT COUNT(*) FROM redirects_" + station_object.getCallLetters() + " WHERE alert_id=" + id); // get the frames in the time range
			rs.next();
			returnval = rs.getLong(1);
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("SQLException in Alert.setDeletionTimestamp: message=" +sqle.getMessage());
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				sqle.printStackTrace();
			}
		}   		
		return returnval;
	}
	
	public JSONObject getAsJSONObject()
	{
		JSONObject response_jo = new JSONObject();
		try {
			response_jo.put("designation", getDesignation());
			response_jo.put("id", getID());
			response_jo.put("social_type", getSocialType());
			if(getSocialType().equals("twitter"))
			{
				Twitter t = new Twitter();
				User user = new User(getCreatedByUser().getDesignation(), "designation");
				JSONObject twitter_object_response_jo = t.getTweet(user.getTwitterAccessToken(), user.getTwitterAccessTokenSecret(), getSocialItemID());
				if(!twitter_object_response_jo.has("response_status")) // no error returned from twitter, just the object itself
				{
					response_jo.put("tweet_jo", twitter_object_response_jo);
				}
			}
			if(getSocialType().equals("facebook"))
			{
				JSONObject jsonresponse = new JSONObject();
				try
				{
					HttpClient client = new DefaultHttpClient();
					HttpGet request = new HttpGet("https://graph.facebook.com/"+ getSocialItemID());
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
						System.out.println(text);
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
				response_jo.put("fbpost_jo", jsonresponse);
			}
			response_jo.put("image_url", getImageURL());
			response_jo.put("creation_timestamp", getTimestamp());
			response_jo.put("text", getActualText());
			response_jo.put("station", getStation());
			response_jo.put("social_item_id", getSocialItemID());
			response_jo.put("created_by", getCreatedByUser().getDesignation());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response_jo;		
	}
	
	public int compareTo(Alert o) // this sorts by designation alphabetically
	{
	    Timestamp othertimestmap = ((Alert)o).getTimestamp();
	    int x = othertimestmap.compareTo(creation_timestamp);
	    if(x >= 0) // this is to prevent equals
	    	return 1;
	    else
	    	return -1;
	}
	
}
