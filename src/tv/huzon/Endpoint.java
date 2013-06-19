package tv.huzon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.mail.MessagingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.Media;
import facebook4j.PostUpdate;
import facebook4j.auth.AccessToken;


public class Endpoint extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public void init(ServletConfig config) throws ServletException
	{
		System.err.println("Endpoint init()");
		 try {
		        Class.forName("com.mysql.jdbc.Driver");
		    } catch (ClassNotFoundException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    } 
		super.init(config);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		//System.out.println("tv.huzon.Endpoint.doPost(): entering...");
		response.setContentType("application/json; charset=UTF-8;");
		response.setHeader("Access-Control-Allow-Origin","*"); //FIXME
		PrintWriter out = response.getWriter();
		JSONObject jsonresponse = new JSONObject();
		Calendar tempcal = Calendar.getInstance();
		long timestamp_at_entry = tempcal.getTimeInMillis();
		try
		{
			if(!request.isSecure())
			{
				jsonresponse.put("message", "The huzon.tv API endpoint must be communicated with securely.");
				jsonresponse.put("response_status", "error");
			}
			else
			{	
				String method = request.getParameter("method");
				if(method == null)
				{
					jsonresponse.put("message", "Method not specified. This should probably produce HTML output reference information at some point.");
					jsonresponse.put("response_status", "error");
				}
				else if (method.equals("commitFrameDataAndAlert"))
				{
					String jsonpostbody_str = request.getParameter("jsonpostbody");
					if(jsonpostbody_str == null)
					{
						jsonresponse.put("message", "jsonpostbody was null. Couldn't find the parameter.");
						jsonresponse.put("response_status", "error");
					}
					else if(jsonpostbody_str.isEmpty())
					{
						jsonresponse.put("message", "jsonpostbody was empty.");
						jsonresponse.put("response_status", "error");
					}	
					else
					{
						JSONObject jsonpostbody = new JSONObject(jsonpostbody_str);
						boolean simulation = false;
						if(jsonpostbody.has("simulation") && (jsonpostbody.getString("simulation").equals("yes") || jsonpostbody.getString("simulation").equals("true")))
						{ 
							System.out.println("Endpoint.commitFrameDataAndAlert(): This is a simulation. Looking up existing frame....");
							simulation = true;
							// no need to do anything here. The frame (if it exists in the db) gets constructed and processed below.
						}
						else // treat as real commit
						{	
							Connection con = null;
							try
							{
								con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
								double currentavgscore = 0.0;
								double reporter_total = 0.0;
								Station station = new Station(jsonpostbody.getString("station"));
								JSONArray all_scores_ja = new JSONArray();
								JSONArray ja = jsonpostbody.getJSONArray("reporter_scores");
								String fieldsstring = " (";
								String valuesstring = " (";
								fieldsstring = fieldsstring + "`" + "image_name" + "`, ";
								valuesstring = valuesstring + "'" + jsonpostbody.getString("image_name") + "', ";
								fieldsstring = fieldsstring + "`" + "s3_location" + "`, ";
								valuesstring = valuesstring + "'s3://huzon-frames-" + station.getCallLetters() + "/" + jsonpostbody.getString("image_name") + "', ";
								fieldsstring = fieldsstring + "`" + "url" + "`, ";
								valuesstring = valuesstring + "'http://" + station.getS3BucketPublicHostname() + "/" + jsonpostbody.getString("image_name") + "', ";
								fieldsstring = fieldsstring + "`" + "timestamp_in_ms" + "`, ";
								valuesstring = valuesstring + "'" + jsonpostbody.getLong("timestamp_in_ms") + "', ";
								fieldsstring = fieldsstring + "`" + "frame_rate" + "`, ";
								valuesstring = valuesstring + "'" + jsonpostbody.getInt("frame_rate") + "', ";
								for(int x = 0; x < ja.length(); x++)
								{
									reporter_total = 0.0;
									for(int i = 0; i < ja.getJSONObject(x).getJSONArray("scores").length(); i++)
									{
										reporter_total = reporter_total + ja.getJSONObject(x).getJSONArray("scores").getDouble(i); 
										all_scores_ja.put(ja.getJSONObject(x).getJSONArray("scores").getDouble(i));
										//total_score = total_score + ja.getJSONObject(x).getJSONArray("scores").getDouble(i); 
									}
									currentavgscore = reporter_total / ja.getJSONObject(x).getJSONArray("scores").length();
									
									// have decided these raw scores are unnecessary. Leaving it out makes for a smaller, more efficient database.
									//fieldsstring = fieldsstring + "`" + ja.getJSONObject(x).getString("designation")+"_scores" + "`, ";
									//valuesstring = valuesstring + "'" + ja.getJSONObject(x).getJSONArray("scores").toString() + "', ";
									fieldsstring = fieldsstring + "`" + ja.getJSONObject(x).getString("designation")+"_avg" + "`, ";
									valuesstring = valuesstring + "'" + currentavgscore + "', ";
									fieldsstring = fieldsstring + "`" + ja.getJSONObject(x).getString("designation")+"_num" + "`, ";
									valuesstring = valuesstring + "'" + ja.getJSONObject(x).getJSONArray("scores").length() + "', ";
								}
								fieldsstring = fieldsstring.substring(0,fieldsstring.length() - 2) + ")";
								valuesstring = valuesstring.substring(0,valuesstring.length() - 2) + ")";
								//System.out.println("Attempting to execute query: INSERT IGNORE INTO `frames_" + jo.getString("station") + "` " + fieldsstring + " VALUES " + valuesstring);
								con.createStatement().execute("INSERT IGNORE INTO `frames_" + jsonpostbody.getString("station") + "` " + fieldsstring + " VALUES " + valuesstring);
								con.createStatement().execute("UPDATE `stations` SET `frame_rate`='" + jsonpostbody.getInt("frame_rate") + "' WHERE call_letters='" + jsonpostbody.getString("station") + "'");
								con.close();
							}
							catch(SQLException sqle)
							{
								jsonresponse.put("message", "There was a problem attempting to insert the scores into the database. sqle.getMessage()=" + sqle.getMessage());
								jsonresponse.put("response_status", "error");
								sqle.printStackTrace();
								
								SimpleEmailer se = new SimpleEmailer();
								try {
									se.sendMail("SQLException in Endpoint commitFrameDataAndAlert", "Error occurred when inserting frame scores. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
								} catch (MessagingException e) {
									e.printStackTrace();
								}
								
							} 
							finally
							{
								try
								{
									if (con  != null)
										con.close();
								}
								catch(SQLException sqle)
								{
									jsonresponse.put("warning", "There was a problem closing the resultset, statement and/or connection to the database.");
									
									SimpleEmailer se = new SimpleEmailer();
									try {
										se.sendMail("SQLException in Endpoint commitFrameDataAndAlert", "Error occurred when closing rs, stmt and con. message=" + sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
									} catch (MessagingException e) {
										e.printStackTrace();
									}
								}
							}  
						}
						
						Frame newframe = new Frame(jsonpostbody.getLong("timestamp_in_ms"), jsonpostbody.getString("station"));
						if(newframe.getTimestampInMillis() > 0) // 0 indicates failure to insert/retrieve
						{	
							JSONObject jo2 = processNewFrame(newframe, simulation);
							jsonresponse.put("response_status", "success");
							jsonresponse.put("alert_triggered", jo2.get("alert_triggered"));
							jsonresponse.put("alert_fired", jo2.get("alert_fired"));
							if(simulation) // then return additional info to the simulator. If not, don't.
							{	
								if(jsonpostbody.has("designation"))
								{
									System.out.println("Endpoint.commitFrameDataAndAlert(): a designation=" + jsonpostbody.getString("designation") + " was specified by the simulator. Returning specialized information in each frame_jo.");
									jsonresponse.put("frame_jo", newframe.getAsJSONObject(true, jsonpostbody.getString("designation")));
								}
								else
									jsonresponse.put("frame_jo", newframe.getAsJSONObject(true, null));
								
								// uncomment these three lines to send huge amounts of frame score data to the simulator (for creating context collages)
								//Station station_object = new Station(jo.getString("station"));
								//JSONArray frames_ja = station_object.getFramesAsJSONArray(jo.getLong("timestamp_in_ms") - 6000, jo.getLong("timestamp_in_ms") + 3000, true);
								//jsonresponse.put("frames_ja", frames_ja);
								
								if(jo2.get("alert_triggered").equals("yes") && jo2.getString("alert_fired").equals("yes"))
								{
									jsonresponse.put("image_name_of_frame_in_window_that_passed_single_thresh", jo2.getString("image_name_of_frame_in_window_that_passed_single_thresh"));
									jsonresponse.put("designation", jo2.getString("designation"));
									jsonresponse.put("social_type", jo2.getString("social_type"));
								}
								if(jo2.has("reason"))
								{
									jsonresponse.put("reason", jo2.get("reason")); 
								}
							}
							
							/*
							if(jo2.get("alert_triggered").equals("yes") && jo2.getString("alert_fired").equals("yes"))
							{
								jsonresponse.put("frame_jo", newframe.getAsJSONObject(true));
								jsonresponse.put("image_name_of_frame_in_window_that_passed_single_thresh", jo2.getString("image_name_of_frame_in_window_that_passed_single_thresh"));
								jsonresponse.put("designation", jo2.getString("designation"));
								jsonresponse.put("social_type", jo2.getString("social_type"));
								jsonresponse.put("url", newframe.getURL());
								jsonresponse.put("image_name", newframe.getImageName());
								SimpleEmailer se = new SimpleEmailer();
								try {
									se.sendMail("Alert triggered for " + jo2.get("designation"), "url=" + newframe.getURL() + " social_type=" + jo2.getString("social_type"), "cyrus7580@gmail.com", "info@huzon.tv");
								} catch (MessagingException e) {
									e.printStackTrace();
								}
								Station station_object = new Station(jo.getString("station"));
								JSONArray frames_ja = station_object.getFramesAsJSONArray(jo.getLong("timestamp_in_ms") - 6000, jo.getLong("timestamp_in_ms") + 3000, true);
								jsonresponse.put("frames_ja", frames_ja);
							}
							if(jo2.get("alert_fired").equals("no"))
							{
								jsonresponse.put("frame_jo", newframe.getAsJSONObject(true));
								jsonresponse.put("reason", jo2.get("reason"));
							}*/
							
						}
						else
						{
							jsonresponse.put("message", "There was a problem inserting the data and/or building a Frame object from it.");
							jsonresponse.put("response_status", "error");
						}
					}
				}
				else
				{
					jsonresponse.put("message", "Unknown method.");
					jsonresponse.put("response_status", "error");
				}
			}
			tempcal = Calendar.getInstance();
			long timestamp_at_exit = tempcal.getTimeInMillis();
			long elapsed = timestamp_at_exit - timestamp_at_entry;
			jsonresponse.put("elapsed", elapsed);
			System.out.println("Endpoint.doGet(): final jsonresponse=" + jsonresponse);	// respond with object, success response, or error
			out.println(jsonresponse);
		}
		catch(JSONException jsone)
		{
			out.println("{ \"error\": { \"message\": \"JSONException caught in Endpoint\" } }");
			System.out.println("endpoint: JSONException thrown in large try block. " + jsone.getMessage());
			jsone.printStackTrace();
		}	
		return;
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		//System.out.println("tv.huzon.Endpoint.doGet(): entering...");
		response.setContentType("application/json; charset=UTF-8;");
		response.setHeader("Access-Control-Allow-Origin","*"); //FIXME
		PrintWriter out = response.getWriter();
		JSONObject jsonresponse = new JSONObject();
		Calendar tempcal = Calendar.getInstance();
		long timestamp_at_entry = tempcal.getTimeInMillis();
		try
		{
			if(!request.isSecure())
			{
				jsonresponse.put("message", "The huzon.tv API endpoint must be communicated with securely.");
				jsonresponse.put("response_status", "error");
			}
			else
			{	
				String method = request.getParameter("method");
				if(method == null)
				{
					jsonresponse.put("message", "Method not specified. This should probably produce HTML output reference information at some point.");
					jsonresponse.put("response_status", "error");
				}
				
				/***
				 *    ______ _____ _____ _____ _____ ___________  ___ _____ _____ _____ _   _ 
				 *    | ___ \  ___|  __ \_   _/  ___|_   _| ___ \/ _ \_   _|_   _|  _  | \ | |
				 *    | |_/ / |__ | |  \/ | | \ `--.  | | | |_/ / /_\ \| |   | | | | | |  \| |
				 *    |    /|  __|| | __  | |  `--. \ | | |    /|  _  || |   | | | | | | . ` |
				 *    | |\ \| |___| |_\ \_| |_/\__/ / | | | |\ \| | | || |  _| |_\ \_/ / |\  |
				 *    \_| \_\____/ \____/\___/\____/  \_/ \_| \_\_| |_/\_/  \___/ \___/\_| \_/
				 *                                                                            
				 *                                                                            
				 */
				
				else if (method.equals("startTwitterAuthentication"))
				{
					Twitter twitter = new Twitter();
					jsonresponse = twitter.startTwitterAuthentication();
				}
				else if (method.equals("getTwitterAccessTokenFromAuthorizationCode"))
				{
					String oauth_verifier = request.getParameter("oauth_verifier");
					String oauth_token = request.getParameter("oauth_token");
					if(oauth_verifier == null)
					{
						jsonresponse.put("message", "This method requires an oauth_verifier value.");
						jsonresponse.put("response_status", "error");
					}
					else if(oauth_token == null)
					{
						jsonresponse.put("message", "This method requires an oauth_token value.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						// at this point the user has been sent to twitter with an initial request_token. They've been sent back
						// to the registration page with an oauth_token and oauth_verifier. However, as they come back, we don't know who they are 
						// except for this oauth_token we've seen before. Therefore, we've saved it above in startTwitterAuthentication
						// and we need to search the DB for it to figure out who it belongs to.
						//String designation = getDesignationFromOAuthToken("wkyt", oauth_token);
						
						Twitter twitter = new Twitter();
						JSONObject preliminary_jsonresponse = new JSONObject();
						preliminary_jsonresponse = twitter.getTwitterAccessTokenFromAuthorizationCode(oauth_verifier, oauth_token);
						if(preliminary_jsonresponse.getString("response_status").equals("success"))
						{
							User user = new User(preliminary_jsonresponse.getString("screen_name"), "twitter_handle");
							if(!user.isValid())
							{
								jsonresponse.put("message", "The screen name returned by Twitter (" + preliminary_jsonresponse.getString("screen_name") + ") was not found in our database.");
								jsonresponse.put("response_status", "error");
							}
							else
							{
								user.setTwitterAccessTokenAndSecret(preliminary_jsonresponse.getString("access_token"), preliminary_jsonresponse.getString("access_token_secret"));
								//setTwitterAccessTokenAndSecret("wkyt", designation, preliminary_jsonresponse.getString("access_token"), preliminary_jsonresponse.getString("access_token_secret"));
								jsonresponse.put("response_status", "success");
								jsonresponse.put("message", "The access_token and access_token_secret should be set in the database now.");
								jsonresponse.put("twitter_handle", preliminary_jsonresponse.getString("screen_name"));
								jsonresponse.put("twitter_access_token", preliminary_jsonresponse.getString("access_token"));
							}
						}
						else
						{
							jsonresponse = preliminary_jsonresponse;
						}
					}
				}
				else if (method.equals("getSelf")) // used for getting oneself, no admin priviliges required
				{
					String twitter_handle = request.getParameter("twitter_handle");
					String twitter_access_token = request.getParameter("twitter_access_token");
					if(twitter_handle == null)
					{
						jsonresponse.put("message", "This method requires a twitter_handle value.");
						jsonresponse.put("response_status", "error");
					}
					else if(twitter_access_token == null)
					{
						jsonresponse.put("message", "This method requires an twitter_access_token value.");
						jsonresponse.put("response_status", "error");
					}
					else
					{
						User user = new User(twitter_handle, "twitter_handle");
						if(user.getTwitterAccessToken().equals(twitter_access_token))
						{
							jsonresponse.put("response_status", "success");
							System.out.println("Endpoint.getUser(): getting user for twitter_handle=" + twitter_handle + "... " + user.getJSONObject());
							jsonresponse.put("user_jo", user.getJSONObject());
						}
						else
						{
							jsonresponse.put("message", "User twitter credentials were invalid.");
							jsonresponse.put("response_status", "error");
							jsonresponse.put("error_code", "07734");
						}
					}
				}
				else if (method.equals("getUser")) // for getting a DIFFERENT user, global_admin required
				{
					String designation = request.getParameter("designation");
					String twitter_handle = request.getParameter("twitter_handle");
					String twitter_access_token = request.getParameter("twitter_access_token");
					if(twitter_handle == null)
					{
						jsonresponse.put("message", "This method requires a twitter_handle value.");
						jsonresponse.put("response_status", "error");
					}
					else if(twitter_access_token == null)
					{
						jsonresponse.put("message", "This method requires a twitter_access_token value.");
						jsonresponse.put("response_status", "error");
					}
					else if(designation == null)
					{
						jsonresponse.put("message", "This method requires a designation value.");
						jsonresponse.put("response_status", "error");
					}
					else
					{
						User user = new User(twitter_handle, "twitter_handle");
						if(!user.getTwitterAccessToken().equals(twitter_access_token))
						{
							jsonresponse.put("message", "The twitter credentials provided were invalid. Can't retrieve user.");
							jsonresponse.put("response_status", "error");
						}
						else // twitter creds were OK
						{
							if(user.isGlobalAdmin())
							{
								jsonresponse.put("response_status", "success");
								User target_user = new User(designation, "designation");
								System.out.println("Endpoint.getUser(): getting user for designation=" + designation + "... " + target_user.getJSONObject());
								jsonresponse.put("user_jo", target_user.getJSONObject());
							}
							else
							{
								jsonresponse.put("message", "You do not have the required permissions to call this method.");
								jsonresponse.put("response_status", "error");
							}
						}
					}
				}
				else if (method.equals("getFacebookAccessTokenFromAuthorizationCode"))
				{
					// twitter account must be linked first. That info is then used as the verifier in lieu of a password
					// hence, this is why we're asking for that information here.
					String twitter_handle = request.getParameter("twitter_handle");
					String twitter_access_token = request.getParameter("twitter_access_token");
					if(twitter_handle == null)
					{
						jsonresponse.put("message", "A twitter_handle value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(twitter_access_token == null)
					{
						jsonresponse.put("message", "A twitter_access_token value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						// check twitter_handle and twitter_access_token for validity
						User user = new User(twitter_handle, "twitter_handle");
						if(!user.getTwitterAccessToken().equals(twitter_access_token))
						{
							jsonresponse.put("message", "The twitter credentials provided were invalid. Can't proceed with Facebook linking.");
							jsonresponse.put("response_status", "error");
						}
						else // twitter creds were OK
						{
							String facebook_code = request.getParameter("facebook_code");
							if(facebook_code == null)
							{
								jsonresponse.put("message", "This method requires a facebook_code value.");
								jsonresponse.put("response_status", "error");
							}
							else
							{	
								// at this point the user has been sent to facebook for permission. The response came back with a code.
								// Now we need to get and store the user's access_token
								JSONObject preliminary_jsonresponse = new JSONObject();
								preliminary_jsonresponse = getFacebookAccessTokenFromAuthorizationCode(facebook_code);
								
								if(preliminary_jsonresponse.getString("response_status").equals("success"))
								{
									JSONObject fb_profile_jo = user.getProfileFromFacebook(preliminary_jsonresponse.getString("access_token"));
									long fb_uid = 0L;
									try
									{
										if(fb_profile_jo != null && fb_profile_jo.has("id"))
										{
											fb_uid = fb_profile_jo.getLong("id");
											Calendar cal = Calendar.getInstance();
											cal.setTimeZone(TimeZone.getTimeZone("America/Louisville"));
											String expires = preliminary_jsonresponse.getString("expires");
											int expires_in_seconds = 0;
											if(expires == null || expires.isEmpty()) // this seems to happen when the user has already given the fb permission but is re-linking the account for whatever reason 
											{
												expires_in_seconds = 5184000; // FIXME? Defaulting to 60 days may not be the right behavior. 
											}
											else
												expires_in_seconds = Integer.parseInt(expires);
											cal.add(Calendar.SECOND, expires_in_seconds);
											long expires_timestamp = cal.getTimeInMillis() / 1000;
											boolean successful = user.setFacebookAccessTokenExpiresAndUID(preliminary_jsonresponse.getString("access_token"), expires_timestamp, fb_uid);
											if(successful)
											{	
												jsonresponse.put("response_status", "success");
												jsonresponse.put("message", "The access_token, expires and uid should be set in the database now.");
											}
											else
											{
												jsonresponse.put("message", "encountered error attempting to update the database with the 3 fb values");
												jsonresponse.put("response_status", "error");
											}
										}
										else
										{
											jsonresponse.put("message", "fb profile didn't have id field");
											jsonresponse.put("response_status", "error");
										}
									}
									catch(NumberFormatException nfe)
									{
										jsonresponse.put("message", "Number format exception for expires=" + preliminary_jsonresponse.getString("expires") + " or for fb profile id value full preliminary_jsonresponse=" + preliminary_jsonresponse);
										jsonresponse.put("response_status", "error");
									}
								}
								else
								{
									jsonresponse = preliminary_jsonresponse; // just return the error
								}
							}
						}
					
					}		
				}
				else if (method.equals("getFacebookSubAccountInfoFromFacebook"))
				{
					// twitter account must be linked first. That info is then used as the verifier in lieu of a password
					// hence, this is why we're asking for that information here.
					String twitter_handle = request.getParameter("twitter_handle");
					String twitter_access_token = request.getParameter("twitter_access_token");
					if(twitter_handle == null)
					{
						jsonresponse.put("message", "A twitter_handle value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(twitter_access_token == null)
					{
						jsonresponse.put("message", "A twitter_access_token value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						// check twitter_handle and twitter_access_token for validity
						User user = new User(twitter_handle, "twitter_handle");
						if(!user.getTwitterAccessToken().equals(twitter_access_token))
						{
							jsonresponse.put("message", "The twitter credentials provided were invalid. Can't proceed with Facebook linking.");
							jsonresponse.put("response_status", "error");
						}
						else // twitter creds were OK
						{
							// now check to see if top-level FB is linked
							if(!user.facebookTopLevelIsLinked())
							{
								jsonresponse.put("message", "It appears the top-level facebook account is not linked. Thus, we can't get the subaccount (reporter page) information.");
								jsonresponse.put("response_status", "error");
							}
							else
							{
								JSONArray fb_subaccounts_ja = user.getSubAccountsFromFacebook();
								if(fb_subaccounts_ja == null)
								{
									jsonresponse.put("message", "Error retrieving subaccount information from Facebook.");
									jsonresponse.put("response_status", "error");
								}
								else
								{	
									if(fb_subaccounts_ja.length() == 0)
									{
										jsonresponse.put("response_status", "success");
										jsonresponse.put("message", "Successfully pinged facebook, but no subaccounts found.");
									}
									else
									{
										jsonresponse.put("response_status", "success");
										jsonresponse.put("fb_subaccounts_ja", fb_subaccounts_ja);
									}
								}
									
							}
						}
					}
				}
				else if (method.equals("setFacebookSubAccountInfo")) // sets the designated journalist page for this user
				{
					// twitter account must be linked first. That info is then used as the verifier in lieu of a password
					// hence, this is why we're asking for that information here.
					String twitter_handle = request.getParameter("twitter_handle");
					String twitter_access_token = request.getParameter("twitter_access_token");
					String fb_subaccount_id = request.getParameter("fb_subaccount_id");
					if(twitter_handle == null)
					{
						jsonresponse.put("message", "A twitter_handle value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(twitter_access_token == null)
					{
						jsonresponse.put("message", "A twitter_access_token value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(fb_subaccount_id == null)
					{
						jsonresponse.put("message", "A fb_subaccount_id value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						// check twitter_handle and twitter_access_token for validity
						User user = new User(twitter_handle, "twitter_handle");
						if(!user.getTwitterAccessToken().equals(twitter_access_token))
						{
							jsonresponse.put("message", "The twitter credentials provided were invalid. Can't proceed with Facebook linking.");
							jsonresponse.put("response_status", "error");
						}
						else // twitter creds were OK
						{
							// now check to see if top-level FB is linked
							if(!user.facebookTopLevelIsLinked())
							{
								jsonresponse.put("message", "It appears the top-level facebook account is not linked. Thus, we can't set the subaccount (reporter page).");
								jsonresponse.put("response_status", "error");
							}
							else
							{
								JSONArray fb_subaccounts_ja = user.getSubAccountsFromFacebook();
								if(fb_subaccounts_ja == null)
								{
									jsonresponse.put("message", "Error retrieving subaccount information from Facebook.");
									jsonresponse.put("response_status", "error");
								}
								else
								{	
									if(fb_subaccounts_ja.length() == 0)
									{
										jsonresponse.put("response_status", "error");
										jsonresponse.put("message", "Successfully pinged facebook, but no subaccounts found. Can't set subaccount.");
									}
									else
									{
										boolean specified_subaccount_exists = false;
										for(int x = 0; x < fb_subaccounts_ja.length(); x++)
										{
											if(fb_subaccounts_ja.getJSONObject(x).getString("id").equals(fb_subaccount_id))
											{
												specified_subaccount_exists = true;
											}
										}
										if(!specified_subaccount_exists)
										{
											jsonresponse.put("message", "The specified subaccount id (" + fb_subaccount_id + ") doesn't exist for this user's top-level facebook account.");
											jsonresponse.put("response_status", "error");
											jsonresponse.put("fb_subaccounts_ja", fb_subaccounts_ja);
										}
										else
										{
											boolean successful = user.setFacebookSubAccountIdNameAndAccessToken(fb_subaccount_id, fb_subaccounts_ja);
											if(successful)
											{
												jsonresponse.put("response_status", "success");
											}
											else
											{
												jsonresponse.put("message", "Pinged facebook, specified subaccount is valid, but ran into error inserting into db.");
												jsonresponse.put("response_status", "error");
											}
										}
									}
								}
							}
						}
					}
				}
				
				
				/***
				 *     _____ ________  ____   _ _       ___ _____ ___________ 
				 *    /  ___|_   _|  \/  | | | | |     / _ \_   _|  _  | ___ \
				 *    \ `--.  | | | .  . | | | | |    / /_\ \| | | | | | |_/ /
				 *     `--. \ | | | |\/| | | | | |    |  _  || | | | | |    / 
				 *    /\__/ /_| |_| |  | | |_| | |____| | | || | \ \_/ / |\ \ 
				 *    \____/ \___/\_|  |_/\___/\_____/\_| |_/\_/  \___/\_| \_|
				 *                                                            
				 *                                                            
				 */
				
				else if (method.equals("getStations")) // inclusive
				{
					String twitter_handle = request.getParameter("twitter_handle");
					String twitter_access_token = request.getParameter("twitter_access_token");
					if(twitter_handle == null)
					{
						jsonresponse.put("message", "A twitter_handle value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(twitter_access_token == null)
					{
						jsonresponse.put("message", "A twitter_access_token value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						// check twitter_handle and twitter_access_token for validity
						User user = new User(twitter_handle, "twitter_handle");
						if(!user.getTwitterAccessToken().equals(twitter_access_token))
						{
							jsonresponse.put("message", "The twitter credentials provided were invalid. Can't retrieve stations.");
							jsonresponse.put("response_status", "error");
						}
						else // twitter creds were OK
						{
							if(user.isGlobalAdmin())
							{
								Platform p = new Platform();
								JSONArray stations_ja = p.getStationsAsJSONArray();
								if(stations_ja != null)
								{
									jsonresponse.put("response_status", "success");
									jsonresponse.put("stations_ja", stations_ja);
								}
								else
								{
									jsonresponse.put("message", "Error getting stations as JSONArray");
									jsonresponse.put("response_status", "error");
								}
							}
							else
							{
								jsonresponse.put("message", "You do not have the required permissions to call this method.");
								jsonresponse.put("response_status", "error");
							}
						}
					}
				}
				else if (method.equals("getActiveReporterDesignations"))
				{	
					String twitter_handle = request.getParameter("twitter_handle");
					String twitter_access_token = request.getParameter("twitter_access_token");
					String station_param = request.getParameter("station");
					if(twitter_handle == null)
					{
						jsonresponse.put("message", "A twitter_handle value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(twitter_access_token == null)
					{
						jsonresponse.put("message", "A twitter_access_token value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(station_param == null)
					{
						jsonresponse.put("message", "A station value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						// check twitter_handle and twitter_access_token for validity
						User user = new User(twitter_handle, "twitter_handle");
						if(!user.getTwitterAccessToken().equals(twitter_access_token))
						{
							jsonresponse.put("message", "The twitter credentials provided were invalid. Can't retrieve stations.");
							jsonresponse.put("response_status", "error");
						}
						else // twitter creds were OK
						{
							if(user.isGlobalAdmin())
							{
								Station station = new Station(station_param);
								if(!station.isValid())
								{
									jsonresponse.put("message", "The station value provided was not valid.");
									jsonresponse.put("response_status", "error");
								}
								else
								{
									jsonresponse.put("response_status", "success");
									jsonresponse.put("reporters_ja", new JSONArray(station.getReporters()));
								}
							}
							else
							{
								jsonresponse.put("message", "You do not have the required permissions to call this method.");
								jsonresponse.put("response_status", "error");
							}
						}
					}
				}
				else if (method.equals("getFrameTimestamps")) // inclusive
				{
					String twitter_handle = request.getParameter("twitter_handle");
					String twitter_access_token = request.getParameter("twitter_access_token");
					String station_param = request.getParameter("station");
					String begin = request.getParameter("begin");
					String end = request.getParameter("end");
					if(twitter_handle == null)
					{
						jsonresponse.put("message", "A twitter_handle value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(twitter_access_token == null)
					{
						jsonresponse.put("message", "A twitter_access_token value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(station_param == null)
					{
						jsonresponse.put("message", "A station value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(begin == null)
					{
						jsonresponse.put("message", "A begin value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(end == null)
					{
						jsonresponse.put("message", "A end value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						// check twitter_handle and twitter_access_token for validity
						User user = new User(twitter_handle, "twitter_handle");
						if(!user.getTwitterAccessToken().equals(twitter_access_token))
						{
							jsonresponse.put("message", "The twitter credentials provided were invalid. Can't retrieve stations.");
							jsonresponse.put("response_status", "error");
						}
						else // twitter creds were OK
						{
							if(user.isGlobalAdmin())
							{
								Station station = new Station(station_param);
								if(!station.isValid())
								{
									jsonresponse.put("message", "The station value provided was not valid.");
									jsonresponse.put("response_status", "error");
								}
								else
								{
									JSONArray timestamps_ja = station.getFrameTimestamps(new Long(begin).longValue()*1000, new Long(end).longValue()*1000);
									jsonresponse.put("response_status", "success");
									jsonresponse.put("timestamps_ja", timestamps_ja);
								}
							}
							else
							{
								jsonresponse.put("message", "You do not have the required permissions to call this method.");
								jsonresponse.put("response_status", "error");
							}
						}
					}
				}
				else if (method.equals("getFrames")) // inclusive
				{
					String twitter_handle = request.getParameter("twitter_handle");
					String twitter_access_token = request.getParameter("twitter_access_token");
					String station_param = request.getParameter("station");
					String begin = request.getParameter("begin");
					String end = request.getParameter("end");
					String get_score_data = request.getParameter("get_score_data");
					if(twitter_handle == null)
					{
						jsonresponse.put("message", "A twitter_handle value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(twitter_access_token == null)
					{
						jsonresponse.put("message", "A twitter_access_token value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(station_param == null)
					{
						jsonresponse.put("message", "A station value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(begin == null)
					{
						jsonresponse.put("message", "A begin value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(end == null)
					{
						jsonresponse.put("message", "A end value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(get_score_data == null)
					{
						jsonresponse.put("message", "A get_score_data value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						// check twitter_handle and twitter_access_token for validity
						User user = new User(twitter_handle, "twitter_handle");
						if(!user.getTwitterAccessToken().equals(twitter_access_token))
						{
							jsonresponse.put("message", "The twitter credentials provided were invalid. Can't retrieve stations.");
							jsonresponse.put("response_status", "error");
						}
						else // twitter creds were OK
						{
							if(user.isGlobalAdmin())
							{
								Station station = new Station(station_param);
								if(!station.isValid())
								{
									jsonresponse.put("message", "The station value provided was not valid.");
									jsonresponse.put("response_status", "error");
								}
								else
								{
									if(get_score_data.equals("true"))
									{	
										jsonresponse.put("response_status", "success");
										jsonresponse.put("frames_ja", station.getFramesAsJSONArray(new Long(begin).longValue()*1000, new Long(end).longValue()*1000, true));
									}
									else if(get_score_data.equals("false"))
									{
										jsonresponse.put("response_status", "success");
										jsonresponse.put("frames_ja", station.getFramesAsJSONArray(new Long(begin).longValue()*1000, new Long(end).longValue()*1000, false));
									}
									else
									{	
										jsonresponse.put("message", "get_score_data must be true or false.");
										jsonresponse.put("response_status", "error");
									}
								}
							}
							else
							{
								jsonresponse.put("message", "You do not have the required permissions to call this method.");
								jsonresponse.put("response_status", "error");
							}
						}
					}
				}
				else if (method.equals("getFramesAboveDesignationHomogeneityThreshold"))
				{	
					String twitter_handle = request.getParameter("twitter_handle");
					String twitter_access_token = request.getParameter("twitter_access_token");
					String station_param = request.getParameter("station");
					String begin = request.getParameter("begin");
					String end = request.getParameter("end");
					String designation = request.getParameter("designation");
					String singlemodifier = request.getParameter("singlemodifier");
					String delta = request.getParameter("delta");
					if(twitter_handle == null)
					{
						jsonresponse.put("message", "A twitter_handle value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(twitter_access_token == null)
					{
						jsonresponse.put("message", "A twitter_access_token value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(station_param == null)
					{
						jsonresponse.put("message", "A station value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(begin == null)
					{
						jsonresponse.put("message", "A begin value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(end == null)
					{
						jsonresponse.put("message", "A end value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(designation == null)
					{
						jsonresponse.put("message", "A designation value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(singlemodifier == null)
					{
						jsonresponse.put("message", "A singlemodifier value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(delta == null)
					{
						jsonresponse.put("message", "A delta value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						// check twitter_handle and twitter_access_token for validity
						User user2 = new User(twitter_handle, "twitter_handle");
						if(!user2.getTwitterAccessToken().equals(twitter_access_token))
						{
							jsonresponse.put("message", "The twitter credentials provided were invalid. Can't retrieve stations.");
							jsonresponse.put("response_status", "error");
						}
						else // twitter creds were OK
						{
							if(user2.isGlobalAdmin())
							{
								Station station = new Station(station_param);
								if(!station.isValid())
								{
									jsonresponse.put("message", "The station parameter provided was invalid. ");
									jsonresponse.put("response_status", "error");
								}
								else
								{	
									jsonresponse.put("response_status", "success");
									boolean get_score_data = true;
									jsonresponse.put("frames_ja", station.getFramesAboveDesignationHomogeneityThresholdAsJSONArray(new Long(begin).longValue()*1000, new Long(end).longValue()*1000, 
											designation, new Double(singlemodifier).doubleValue(), get_score_data)); 
								}
							}
							else
							{
								jsonresponse.put("message", "You do not have the required permissions to call this method.");
								jsonresponse.put("response_status", "error");
							}
						}
					}
				}
				else if (method.equals("getAlertFrames"))
				{
					System.out.println("Endpoint begin getAlertFrames");
					String twitter_handle = request.getParameter("twitter_handle");
					String twitter_access_token = request.getParameter("twitter_access_token");
					String station_param = request.getParameter("station");
					
					String begin = request.getParameter("begin");
					String end = request.getParameter("end");
					String singlemodifier = request.getParameter("singlemodifier");
					String mamodifier = request.getParameter("mamodifier");
					String mawindow = request.getParameter("mawindow");
					String delta = request.getParameter("delta");
					
					if(twitter_handle == null)
					{
						jsonresponse.put("message", "A twitter_handle value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(twitter_access_token == null)
					{
						jsonresponse.put("message", "A twitter_access_token value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(station_param == null)
					{
						jsonresponse.put("message", "A station value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(begin == null)
					{
						jsonresponse.put("message", "A begin value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(end == null)
					{
						jsonresponse.put("message", "A end value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(delta == null)
					{
						jsonresponse.put("message", "A delta value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(singlemodifier == null)
					{
						jsonresponse.put("message", "A singlemodifier value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(mamodifier == null)
					{
						jsonresponse.put("message", "A mamodifier value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(mawindow == null)
					{
						jsonresponse.put("message", "A mawindow value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						// check twitter_handle and twitter_access_token for validity
						User user2 = new User(twitter_handle, "twitter_handle");
						if(!user2.getTwitterAccessToken().equals(twitter_access_token))
						{
							jsonresponse.put("message", "The twitter credentials provided were invalid. Can't retrieve stations.");
							jsonresponse.put("response_status", "error");
						}
						else // twitter creds were OK
						{
							if(user2.isGlobalAdmin())
							{
								Station station = new Station(station_param);
								if(!station.isValid())
								{
									jsonresponse.put("message", "The station parameter provided was invalid. ");
									jsonresponse.put("response_status", "error");
								}
								else
								{	
									int moving_average_window_int = Integer.parseInt(request.getParameter("mawindow"));
									double ma_modifier_double = (new Double(request.getParameter("mamodifier"))).doubleValue();
									double single_modifier_double = (new Double(request.getParameter("singlemodifier"))).doubleValue();
									double delta_double = (new Double(request.getParameter("delta"))).doubleValue();
									long begin_long = Long.parseLong(begin);
									long end_long = Long.parseLong(end);
									System.out.println("Endpoint.getAlertsForTimePeriod(): passed validation gauntlet, moving to getAlertFrames() function");
									JSONArray alert_frames_ja = station.getAlertFrames(begin_long, end_long, moving_average_window_int, ma_modifier_double, single_modifier_double, delta_double);
									jsonresponse.put("response_status", "success");
									jsonresponse.put("alert_frames_ja", alert_frames_ja);
								}
							}
							else
							{
								jsonresponse.put("message", "You do not have the required permissions to call this method.");
								jsonresponse.put("response_status", "error");
							}
						}
					}
				}
				//***
				// VERY DANGEROUS!!! 
				//***
				else if (method.equals("resetProductionAlertTimers"))
				{
					System.out.println("Endpoint begin getAlertFrames");
					String twitter_handle = request.getParameter("twitter_handle");
					String twitter_access_token = request.getParameter("twitter_access_token");
					String station_param = request.getParameter("station");
					if(twitter_handle == null)
					{
						jsonresponse.put("message", "A twitter_handle value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(twitter_access_token == null)
					{
						jsonresponse.put("message", "A twitter_access_token value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(station_param == null)
					{
						jsonresponse.put("message", "A station value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						// check twitter_handle and twitter_access_token for validity
						User user2 = new User(twitter_handle, "twitter_handle");
						if(!user2.getTwitterAccessToken().equals(twitter_access_token))
						{
							jsonresponse.put("message", "The twitter credentials provided were invalid. Can't retrieve stations.");
							jsonresponse.put("response_status", "error");
						}
						else // twitter creds were OK
						{
							if(user2.isGlobalAdmin())
							{
								Station station = new Station(station_param);
								if(!station.isValid())
								{
									jsonresponse.put("message", "The station parameter provided was invalid. ");
									jsonresponse.put("response_status", "error");
								}
								else
								{	
									station.resetProductionAlertTimers();
									jsonresponse.put("response_status", "success");
								}
							}
							else
							{
								jsonresponse.put("message", "You do not have the required permissions to call this method.");
								jsonresponse.put("response_status", "error");
							}
						}
					}
				} 
				else if (method.equals("resetTestAlertTimers"))
				{
					System.out.println("Endpoint begin getAlertFrames");
					String twitter_handle = request.getParameter("twitter_handle");
					String twitter_access_token = request.getParameter("twitter_access_token");
					String station_param = request.getParameter("station");
					if(twitter_handle == null)
					{
						jsonresponse.put("message", "A twitter_handle value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(twitter_access_token == null)
					{
						jsonresponse.put("message", "A twitter_access_token value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else if(station_param == null)
					{
						jsonresponse.put("message", "A station value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						// check twitter_handle and twitter_access_token for validity
						User user2 = new User(twitter_handle, "twitter_handle");
						if(!user2.getTwitterAccessToken().equals(twitter_access_token))
						{
							jsonresponse.put("message", "The twitter credentials provided were invalid. Can't retrieve stations.");
							jsonresponse.put("response_status", "error");
						}
						else // twitter creds were OK
						{
							if(user2.isGlobalAdmin())
							{
								Station station = new Station(station_param);
								if(!station.isValid())
								{
									jsonresponse.put("message", "The station parameter provided was invalid. ");
									jsonresponse.put("response_status", "error");
								}
								else
								{	
									station.resetTestAlertTimers();
									jsonresponse.put("response_status", "success");
								}
							}
							else
							{
								jsonresponse.put("message", "You do not have the required permissions to call this method.");
								jsonresponse.put("response_status", "error");
							}
						}
					}
				} 
				else
				{
					jsonresponse.put("message", "Unknown method " + method); // we have already checked for null above
					jsonresponse.put("response_status", "error");
				}
			}
			tempcal = Calendar.getInstance();
			long timestamp_at_exit = tempcal.getTimeInMillis();
			long elapsed = timestamp_at_exit - timestamp_at_entry;
			jsonresponse.put("elapsed", elapsed);
			out.println(jsonresponse);
		}
		catch(JSONException jsone)
		{
			out.println("{ \"error\": { \"message\": \"JSONException caught in Endpoint\" } }");
			System.out.println("endpoint: JSONException thrown in large try block. " + jsone.getMessage());
		}	
		return;
	}
	
	// this is hardcoded with a 5 second moving average window, a .67 moving average threshold and a 1 single frame threshold
	
	JSONObject processNewFrame(Frame newframe, boolean simulation)
	{
		JSONObject return_jo = new JSONObject();
		System.out.println("Endpoint.processNewFrame(): Entering processNewFrame(Frame)...");
		try{
			// get all frames over the moving average window backward from this timestamp
			Station station_object = new Station(newframe.getStation());
			TreeSet<Frame> window_frames = station_object.getFrames(newframe.getTimestampInMillis()-5000, newframe.getTimestampInMillis(), null, 0);
			int num_frames_in_window = window_frames.size();
			System.out.println("Endpoint.processNewFrame(): Found " + num_frames_in_window + " frames in the specified window. Examining...");
			if(num_frames_in_window < 5)
			{
				System.out.println("Endpoint.processNewFrame(): Warning! Not enough frames in this window. Could be beginning of a recording, though. If so, that's ok.");
				return_jo.put("alert_triggered", "no");
				return_jo.put("alert_fired", "no");
				return_jo.put("reason", "not enough frames");
				return return_jo;
			}
			
			String[] reporter_designations = newframe.getReporterDesignations();
			double[] reporter_totals = new double[reporter_designations.length];
			double[] reporter_moving_averages = new double[reporter_designations.length];
			double[] reporter_moving_average_thresholds = new double[reporter_designations.length];
			double[] reporter_single_thresholds = new double[reporter_designations.length];

			// loop through the frames in the window, calculating total designation_avg for each reporter
					Iterator<Frame> it = window_frames.iterator();
			Frame currentframe = null;
			while(it.hasNext())
			{
				currentframe = it.next();
				//System.out.println("Endpoint.processNewFrame(): Examining " + currentframe.getImageName());
				int x = 0;
				while(x < reporter_designations.length)
				{
					//if(reporter_designations[x].equals("alex_hayes") || reporter_designations[x].equals("kristen_kennedy"))
					//	System.out.println("Endpoint.processNewFrame(): " + reporter_designations[x] + " score= " + currentframe.getScore(reporter_designations[x]));
					reporter_totals[x] = reporter_totals[x] + currentframe.getScore(reporter_designations[x]);
					x++;
				}
			}
			
			// then divide the sum designation_avgs by dividing by the number of frames in the window
			int x = 0;
			double reporter_homogeneity = 0;
			double max_moving_average = 0;
			
			while(x < reporter_totals.length)
			{
				reporter_moving_averages[x] = reporter_totals[x] / num_frames_in_window;
				if(reporter_moving_averages[x] > max_moving_average)
				{
					max_moving_average = reporter_moving_averages[x];
				}
				if(reporter_moving_averages[x] > 0.5) // moving average has to be AT LEAST .5 to even be considered (which is approx .67 * .75) i.e. no reporter homogeneity should ever be below .75
				{	
					reporter_homogeneity = (new User(reporter_designations[x],"designation")).getHomogeneity();
					reporter_moving_average_thresholds[x] = .67 * reporter_homogeneity;
					reporter_single_thresholds[x] = reporter_homogeneity;
				}
				else
				{
					reporter_moving_average_thresholds[x] = 100;
					reporter_single_thresholds[x] = 100;
				}
				x++;
			}
			
			//boolean passed_ma_thresh = false;
			//long timestamp_in_ms_of_frame_in_window_that_passed_single_thresh = 0L;
			boolean frame_in_window_passed_single_thresh = false;
			String designation_that_passed_ma_thresh = "";
			
			String image_name_of_frame_in_window_that_passed_single_thresh = "";
			x = 0;
			while(x < reporter_designations.length)
			{
				// does it pass the ma threshold and is it the highest?
				if(reporter_moving_averages[x] > reporter_moving_average_thresholds[x] && reporter_moving_averages[x] == max_moving_average) 
				{
					//passed_ma_thresh = true;
					designation_that_passed_ma_thresh = reporter_designations[x];
					User reporter = new User(designation_that_passed_ma_thresh, "designation");
					
					if((newframe.getTimestampInMillis() - reporter.getLastFacebookAlert(simulation)) < reporter.getFacebookWaitingPeriodInMillis()
							&& ((newframe.getTimestampInMillis() - reporter.getLastTwitterAlert(simulation)) < reporter.getTwitterWaitingPeriodInMillis()))
					{	
						return_jo.put("alert_triggered", "no");
						return_jo.put("social_type", "neither");
						return_jo.put("alert_fired", "no");
						return_jo.put("reason", "Passed MA threshold, unknown if passed single frame thresh because user within waiting period of both facebook and twitter and couldn't fire alert, regardless.");
						return_jo.put("designation", reporter_designations[x]);
						return return_jo;
					}

					// at this point, a reporter passed ma_thresh and is without fb or twit waiting periods (so an alert is possible. Check single frame thresh)
					JSONArray frames_ja = station_object.getFramesAsJSONArray(newframe.getTimestampInMillis()-5000, newframe.getTimestampInMillis(), true);
					System.out.println("Looking for frame in window that passes single thresh");
					for(int y = 0; y < frames_ja.length(); y++)
					{
						System.out.println("Looking at " + frames_ja.getJSONObject(y).getString("image_name"));
						if(frames_ja.getJSONObject(y).getJSONObject("reporters").getJSONObject(designation_that_passed_ma_thresh).getDouble("score_avg") > reporter.getHomogeneity())
						{
							frame_in_window_passed_single_thresh = true;
							//timestamp_in_ms_of_frame_in_window_that_passed_single_thresh = frames_ja.getJSONObject(y).getLong("timestamp_in_ms");
							image_name_of_frame_in_window_that_passed_single_thresh  = frames_ja.getJSONObject(y).getString("image_name");
							break; // important to prevent getting later frames that may have also passed
						}
					}
					if(frame_in_window_passed_single_thresh)// frame in window passed single thresh
					{	
						System.out.println(reporter_designations[x] + " passed the moving average threshold for this frame. " + reporter_moving_averages[x] + " > " + reporter_moving_average_thresholds[x]);
						boolean fb = false;
						boolean tw = false;
						
						if((newframe.getTimestampInMillis() - reporter.getLastFacebookAlert(simulation)) >= reporter.getFacebookWaitingPeriodInMillis())
						{	
							System.out.println("Facebook fired! ts=" + newframe.getTimestampInMillis() + " last=" + reporter.getLastFacebookAlert(simulation) + " diff=" + (newframe.getTimestampInMillis() - reporter.getLastFacebookAlert(simulation)) + " wait=" + reporter.getFacebookWaitingPeriodInMillis());
							return_jo.put("alert_triggered", "yes");
							return_jo.put("alert_fired", "yes");
							return_jo.put("social_type", "facebook");
							reporter.setLastAlert(newframe.getTimestampInMillis(), "facebook", simulation);
							fb = true;
						}
						
						if((newframe.getTimestampInMillis() - reporter.getLastTwitterAlert(simulation)) >= reporter.getTwitterWaitingPeriodInMillis())
						{	
							System.out.println("Twitter fired! ts=" + newframe.getTimestampInMillis() + " last=" + reporter.getLastTwitterAlert(simulation) + " diff=" + (newframe.getTimestampInMillis() - reporter.getLastTwitterAlert(simulation)) + " wait=" + reporter.getTwitterWaitingPeriodInMillis());
							return_jo.put("alert_triggered", "yes");
							return_jo.put("alert_fired", "yes");
							if(fb)
								return_jo.put("social_type", "both");
							else
								return_jo.put("social_type", "twitter");
							reporter.setLastAlert(newframe.getTimestampInMillis(), "twitter", simulation);
							tw = true;
						}
						
						// temporary hardcode email on new alert
						if(fb || tw)
						{
							if(!simulation) // don't send email on simulation... because the user is watching the alerts in the simulator. No point.
							{	
								SimpleEmailer se = new SimpleEmailer();
								try {
									se.sendMail("Alert triggered for " + reporter_designations[x], "url=" + newframe.getURL() + " tw=" + tw + " fb=" + fb, "cyrus7580@gmail.com", "info@huzon.tv");
								} catch (MessagingException e) {
									e.printStackTrace();
								}
							}
						}
						
						if(tw || fb)
						{	
							Platform p = new Platform();
							if(tw)
							{
								/*
								 * 
								 * 					long twitter_redirect_id = createAlertInDB("wkyt", "twitter", max_designation ,image_name_of_frame_with_highest_score_in_window); 
													jsonresponse.put("twitter_redirect_id", twitter_redirect_id);
													String message = getMessage("twitter", ts_long, twitter_redirect_id);
													jsonresponse.put("twitter_message_firstperson", message);
													boolean successful = updateAlertText(twitter_redirect_id, message);
								 */
								System.out.println("Endpoint.processNewFrame(): Firing tweet for " + reporter.getDisplayName());
								User test_user = new User("huzon_master", "designation");
								
								// download file first
								URL image_url = new URL(newframe.getURL());
							    ReadableByteChannel rbc = Channels.newChannel(image_url.openStream());
							    String tmpdir = System.getProperty("java.io.tmpdir");
							    System.out.println("TEMP DIR=" + tmpdir);
							    FileOutputStream fos = new FileOutputStream(tmpdir + "/image.jpg");
							    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
							    File f = new File(tmpdir + "/image.jpg");
							    //
							   
								Twitter twitter = new Twitter();
								long redirect_id = p.createAlertInDB(station_object, "twitter", reporter.getDesignation(), newframe.getURL());
								String message = station_object.getMessage("twitter", newframe.getTimestampInMillis(), redirect_id);
								//JSONObject twit_jo = twitter.updateStatus(test_user.getTwitterAccessToken(), test_user.getTwitterAccessTokenSecret(), reporter.getDisplayName() + " is live on the air." + newframe.getURL());
								JSONObject twit_jo = twitter.updateStatusWithMedia(test_user.getTwitterAccessToken(), test_user.getTwitterAccessTokenSecret(), message, f);
								
								// update the table row with the message actual_text and social_id
								boolean alert_text_update_successful = p.updateAlertText(redirect_id, message);
								boolean social_id_update_successful = p.updateSocialItemID(redirect_id,twit_jo.getString("id"));
								
								System.out.println("Endpoint.processNewFrame(): Twitter result=" + twit_jo.toString());
							}
							
							if(fb)
							{
								/*
								 * 
								 * 				long facebook_redirect_id = createAlertInDB("wkyt", "facebook", max_designation, image_name_of_frame_with_highest_score_in_window); 
												jsonresponse.put("facebook_redirect_id", facebook_redirect_id);
												String message = getMessage("facebook", ts_long, facebook_redirect_id);
												jsonresponse.put("facebook_message_firstperson",message);
												boolean successful = updateAlertText(facebook_redirect_id, message);
								 */
								System.out.println("Endpoint.processNewFrame(): Firing facebook post for " + reporter.getDisplayName());
								User test_user = new User("huzon_master", "designation");
								
								
								Facebook facebook = new FacebookFactory().getInstance();
								facebook.setOAuthAppId("176524552501035", "dbf442014759e75f2f93f2054ac319a0");
								facebook.setOAuthPermissions("publish_stream,manage_page");
								facebook.setOAuthAccessToken(new AccessToken(test_user.getFacebookPageAccessToken(), null));
								
								long redirect_id = p.createAlertInDB(station_object, "facebook", reporter.getDesignation(), newframe.getURL());
								String message = station_object.getMessage("facebook", newframe.getTimestampInMillis(), redirect_id);
								
								// download file first
								URL image_url = new URL(newframe.getURL());
							    ReadableByteChannel rbc = Channels.newChannel(image_url.openStream());
							    String tmpdir = System.getProperty("java.io.tmpdir");
							    System.out.println("TEMP DIR=" + tmpdir);
							    FileOutputStream fos = new FileOutputStream(tmpdir + "/image.jpg");
							    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
							    File f = new File(tmpdir + "/image.jpg");
							    //
								
								/*PostUpdate post = new PostUpdate(message)
			                    	.picture(new URL(newframe.getURL()))
			                    	.name("")
			                    	.caption("")
			                    	.description("");*/
								String facebookresponse = "";
								try {
									//facebookresponse = facebook.postFeed(post);
									facebookresponse = facebook.postPhoto(new Long(test_user.getFacebookPageID()).toString(), new Media(f), message, "33684860765", false);
								} catch (FacebookException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								boolean alert_text_update_successful = p.updateAlertText(redirect_id, message);
								boolean social_id_update_successful = p.updateSocialItemID(redirect_id, facebookresponse);
								
								System.out.println("Endpoint.processNewFrame(): Facebook result=" + facebookresponse);
							}
						}
						
						
						if(!fb && !tw)
						{
							return_jo.put("alert_triggered", "yes");
							return_jo.put("social_type", "neither");
							return_jo.put("alert_fired", "no");
							return_jo.put("reason", "Passed MA threshold, but was within waiting period of both facebook and twitter. Couldn't fire alert.");
						}
						return_jo.put("designation", reporter_designations[x]);
						return_jo.put("image_name_of_frame_in_window_that_passed_single_thresh", image_name_of_frame_in_window_that_passed_single_thresh);
						return return_jo;
					}
					else
					{
						return_jo.put("alert_triggered", "no");
						return_jo.put("alert_fired", "no");
						return_jo.put("reason", designation_that_passed_ma_thresh + " passed MA threshold, but none of the frames in the window passed single thresh");
						return return_jo;
					}
				} 
				x++;
			}
			return_jo.put("alert_triggered", "no");
			return_jo.put("alert_fired", "no");
			return_jo.put("reason", "none passed MA threshold");
		}
		catch(JSONException jsone)
		{
			jsone.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return return_jo;
	}

	public JSONObject getFacebookAccessTokenFromAuthorizationCode(String code)
	{
		JSONObject jsonresponse = new JSONObject();
		String client_id = "176524552501035";
		String client_secret = "dbf442014759e75f2f93f2054ac319a0";
		String redirect_uri = "https://www.huzon.tv/registration.html";
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet("https://graph.facebook.com/oauth/access_token?client_id=" + client_id + "&client_secret=" + client_secret + "&redirect_uri=" + redirect_uri + "&code=" + code);
		HttpResponse response;
		try
		{
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
				jsonresponse.put("response_status", "success");
				jsonresponse.put("response_from_facebook", text);
				StringTokenizer st = new StringTokenizer(text,"&");
				String currenttoken = "";
				String access_token = "";
				String expires = "";
				while(st.hasMoreTokens())
				{
					 currenttoken = st.nextToken();
					 if(currenttoken.startsWith("access_token="))
						 access_token = currenttoken.substring(currenttoken.indexOf("=") + 1);
					 else if(currenttoken.startsWith("expires="))
						 expires = currenttoken.substring(currenttoken.indexOf("=") + 1);
					 else
					 {
						 // something else. The 4 values above are the only ones twitter should return, so this case would be weird.
						 // skip
					 }
				}
				jsonresponse.put("access_token", access_token);
				jsonresponse.put("expires", expires);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				jsonresponse.put("response_status", "error");
				jsonresponse.put("message", "clientprotocolexception " + e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
				jsonresponse.put("response_status", "error");
				jsonresponse.put("message", "ioexception " + e.getMessage());
			}
		}	
		catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonresponse;
	}
	
	
	
	public static void main(String[] args) {
		//Endpoint e = new Endpoint();
	}
	
}



