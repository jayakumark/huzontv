package tv.huzon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.TreeSet;

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
	String local_timestamp_hr;
	String social_type;
	String image_url;
	Timestamp creation_timestamp;
	String designation;
	String station_str;
	String actual_text;
	String social_item_id;
	String created_by;
	Station station_object;
	TreeSet<Redirect> sansbot_redirects_set;
	TreeSet<Redirect> unabridged_redirects_set;
	
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
		sansbot_redirects_set = null;
		unabridged_redirects_set = null;
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
				
				// standard calendar shit
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(timestamp_in_ms);
				cal.setTimeZone(TimeZone.getTimeZone(station_object.getJavaTimezoneString()));
				String year = new Integer(cal.get(Calendar.YEAR)).toString();
				String month = new Integer(cal.get(Calendar.MONTH) + 1).toString();
				if(month.length() == 1) { month = "0" + month; }
				String day = new Integer(cal.get(Calendar.DAY_OF_MONTH)).toString();
				if(day.length() == 1) { day = "0" + day;} 
				String hour24 = new Integer(cal.get(Calendar.HOUR_OF_DAY)).toString();
				if(hour24.length() == 1) { hour24 = "0" + hour24;} 
				String minute = new Integer(cal.get(Calendar.MINUTE)).toString();
				if(minute.length() == 1) { minute = "0" + minute;} 
				String second = new Integer(cal.get(Calendar.SECOND)).toString();
				if(second.length() == 1) { second = "0" + second;} 
				String ms = new Long(timestamp_in_ms%1000).toString();
				if(ms.length() == 1) { ms = "00" + ms;} 
				if(ms.length() == 2) { ms = "0" + ms;} 
				local_timestamp_hr = year  + month + day + "_" + hour24 + minute + second + "_" + ms;		
				// standard_calendar_shit
				
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
		if(sansbot)
		{	
			if(sansbot_redirects_set == null)
				return getSansbotRedirects().size();
			else
				return sansbot_redirects_set.size();
		}
		else
		{
			if(unabridged_redirects_set == null)
				return getUnabridgedRedirects().size();
			else
				return unabridged_redirects_set.size();
		}
	}
	
	private TreeSet<Redirect> getSansbotRedirects() 
	{
		if(sansbot_redirects_set != null)
			return sansbot_redirects_set;
		sansbot_redirects_set = new TreeSet<Redirect>();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = datasource.getConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM redirects_" + station_object.getCallLetters() + " WHERE alert_id=" + id + 
						" AND `user_agent` NOT LIKE '%bot%' AND `user_agent` NOT LIKE '%UnwindFetchor%' AND `user_agent` NOT LIKE '%JS-Kit%'" +
						" AND `user_agent` NOT LIKE '%NING%' AND `user_agent` NOT LIKE '%facebookexternalhit%' AND `user_agent` NOT LIKE '%RockmeltEmbedder%' " +
						"AND `user_agent`!='' AND `user_agent` NOT LIKE '%LongURL API%' AND `user_agent` NOT LIKE '%PycURL%' AND `user_agent` NOT LIKE '%Java/%' " +
						"AND `user_agent` NOT LIKE '%spider%'  AND `user_agent` NOT LIKE '%Spider%' AND `user_agent` NOT LIKE '%Butterfly%' " + 
						" AND `user_agent` NOT LIKE '%Bot%' AND `user_agent` NOT LIKE '%iCoreService%'");
			while(rs.next())
			{
				sansbot_redirects_set.add(new Redirect(rs.getLong("id"), station_object.getCallLetters()));
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("SQLException in Alert.getSansbotRedirects: message=" +sqle.getMessage());
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
				(new Platform()).addMessageToLog("SQLException in Alert.getSansbotRedirects closing rs, stmt or con: message=" +sqle.getMessage());
			}
		}   
		return sansbot_redirects_set;
	}
	
	private TreeSet<Redirect> getUnabridgedRedirects() 
	{
		if(unabridged_redirects_set != null)
			return unabridged_redirects_set;
		unabridged_redirects_set = new TreeSet<Redirect>();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = datasource.getConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM redirects_" + station_object.getCallLetters() + " WHERE alert_id=" + id);
			while(rs.next())
			{
				unabridged_redirects_set.add(new Redirect(rs.getLong("id"), station_object.getCallLetters()));
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("SQLException in Alert.getUnabridgedRedirects: message=" +sqle.getMessage());
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
				(new Platform()).addMessageToLog("SQLException in Alert.getUnabridgedRedirects closing rs, stmt or con: message=" +sqle.getMessage());
			}
		}   
		return unabridged_redirects_set;
	}
	
	private JSONObject getSansbotUltimateDestiationStatistics()
	{
		TreeSet<Redirect> redirects_set = getSansbotRedirects();
		Iterator<Redirect> redirect_it = redirects_set.iterator();
		JSONObject return_jo = new JSONObject();
		try
		{
			Redirect currentredirect = null;
			while(redirect_it.hasNext())
			{
				currentredirect = redirect_it.next();
				if(return_jo.has(currentredirect.getUltimateDestination()))
					return_jo.put(currentredirect.getUltimateDestination(), return_jo.getInt(currentredirect.getUltimateDestination()) + 1);
				else
				{
					return_jo.put(currentredirect.getUltimateDestination(), 1);
				}
			}
		}	
		catch (JSONException e) {
			e.printStackTrace();
		}
		return return_jo;
	}
	
	public JSONObject getAsJSONObject(boolean get_social_objects)
	{
		JSONObject response_jo = new JSONObject();
		try {
			response_jo.put("designation", getDesignation());
			response_jo.put("id", getID());
			response_jo.put("social_type", getSocialType());
			if(get_social_objects)
			{	
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
			}
			response_jo.put("unabridged_redirect_count", getRedirectCount(false));
			response_jo.put("sansbot_redirect_count", getRedirectCount(true));
			response_jo.put("ultimate_destination_stats", getSansbotUltimateDestiationStatistics());
			response_jo.put("image_url", getImageURL());
			response_jo.put("creation_timestamp", getTimestamp());
			response_jo.put("local_timestamp_hr", local_timestamp_hr);
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
	
	public static void main(String args[])
	{
		Alert a = new Alert(726);
		System.out.println(a.getSansbotUltimateDestiationStatistics());
	}
	
}
