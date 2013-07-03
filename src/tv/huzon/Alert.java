package tv.huzon;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import javax.mail.MessagingException;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class Alert {

	long id;
	String social_type;
	String image_url;
	Timestamp creation_timestamp;
	String designation;
	String station_str;
	String actual_text;
	String social_item_id;
	String created_by;
	
	String dbName = System.getProperty("RDS_DB_NAME"); 
	String userName = System.getProperty("RDS_USERNAME"); 
	String password = System.getProperty("RDS_PASSWORD"); 
	String hostname = System.getProperty("RDS_HOSTNAME");
	String port = System.getProperty("RDS_PORT");
	
	public Alert(long inc_id)
	{
		try {
	        Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
		id = inc_id;
		
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM alerts WHERE id=" + inc_id); // get the frames in the time range
			
			// calculate the number of reporters by looping through all columns and looking for "_avg"
			if(rs.next())
			{	
				social_type = rs.getString("social_type");
				image_url = rs.getString("image_url");
				creation_timestamp = rs.getTimestamp("creation_timestamp");
				designation = rs.getString("designation");
				station_str = rs.getString("station");
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
	
	Timestamp getTimestamp()
	{
		return creation_timestamp;
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
			
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM alerts WHERE id=" + id); // get the frames in the time range
			
			// calculate the number of reporters by looping through all columns and looking for "_avg"
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
}
