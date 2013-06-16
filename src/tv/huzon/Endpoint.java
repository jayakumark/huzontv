package tv.huzon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Random;
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
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;


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
		System.out.println("tv.huzon.Endpoint.doPost(): entering...");
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
					String jsonpostbody = request.getParameter("jsonpostbody");
					if(jsonpostbody == null)
					{
						jsonresponse.put("message", "jsonpostbody was null. Couldn't find the parameter.");
						jsonresponse.put("response_status", "error");
					}
					else if(jsonpostbody.isEmpty())
					{
						jsonresponse.put("message", "jsonpostbody was empty.");
						jsonresponse.put("response_status", "error");
					}	
					else
					{
						System.out.println("Endpoint.commitFrameDataAndAlert(): Looking up existing frame....");
						JSONObject jo = new JSONObject(jsonpostbody);
						Connection con = null;
						try
						{
							con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
							
							double currentavgscore = 0.0;
							double reporter_total = 0.0;
							Station station = new Station(jo.getString("station"));
							JSONArray all_scores_ja = new JSONArray();
							JSONArray ja = jo.getJSONArray("reporter_scores");
							String fieldsstring = " (";
							String valuesstring = " (";
							fieldsstring = fieldsstring + "`" + "image_name" + "`, ";
							valuesstring = valuesstring + "'" + jo.getString("image_name") + "', ";
							fieldsstring = fieldsstring + "`" + "s3_location" + "`, ";
							valuesstring = valuesstring + "'s3://huzon-frames-" + station.getCallLetters() + "/" + jo.getString("image_name") + "', ";
							fieldsstring = fieldsstring + "`" + "url" + "`, ";
							valuesstring = valuesstring + "'http://" + station.getS3BucketPublicHostname() + "/" + jo.getString("image_name") + "', ";
							fieldsstring = fieldsstring + "`" + "timestamp_in_ms" + "`, ";
							valuesstring = valuesstring + "'" + jo.getLong("timestamp_in_ms") + "', ";
							fieldsstring = fieldsstring + "`" + "frame_rate" + "`, ";
							valuesstring = valuesstring + "'" + jo.getInt("frame_rate") + "', ";
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
								fieldsstring = fieldsstring + "`" + ja.getJSONObject(x).getString("designation")+"_scores" + "`, ";
								valuesstring = valuesstring + "'" + ja.getJSONObject(x).getJSONArray("scores").toString() + "', ";
								fieldsstring = fieldsstring + "`" + ja.getJSONObject(x).getString("designation")+"_avg" + "`, ";
								valuesstring = valuesstring + "'" + currentavgscore + "', ";
								fieldsstring = fieldsstring + "`" + ja.getJSONObject(x).getString("designation")+"_num" + "`, ";
								valuesstring = valuesstring + "'" + ja.getJSONObject(x).getJSONArray("scores").length() + "', ";
							}
							fieldsstring = fieldsstring.substring(0,fieldsstring.length() - 2) + ")";
							valuesstring = valuesstring.substring(0,valuesstring.length() - 2) + ")";
							System.out.println("Attempting to execute query: INSERT IGNORE INTO `frames_" + jo.getString("station") + "` " + fieldsstring + " VALUES " + valuesstring);
							con.createStatement().execute("INSERT IGNORE INTO `frames_" + jo.getString("station") + "` " + fieldsstring + " VALUES " + valuesstring);
							con.createStatement().execute("UPDATE `stations` SET `frame_rate`='" + jo.getInt("frame_rate") + "' WHERE call_letters='" + jo.getString("station") + "'");
							con.close();
							Frame newframe = new Frame(jo.getLong("timestamp_in_ms"), jo.getString("station"));
							if(newframe.getTimestampInMillis() > 0) // 0 indicates failure to insert/retrieve
							{	
								JSONObject jo2 = processNewFrame(new Frame(jo.getLong("timestamp_in_ms"), jo.getString("station")));
								jsonresponse.put("response_status", "success");
								jsonresponse.put("alert_triggered", jo2.get("alert_triggered"));
								if(jo2.get("alert_triggered").equals("yes"))
								{
									jsonresponse.put("designation", jo2.getString("designation"));
									jsonresponse.put("social_type", jo2.getString("social_type"));
									SimpleEmailer se = new SimpleEmailer();
									try {
										se.sendMail("Alert triggered for " + jo2.get("designation"), "url=" + newframe.getURL() + " social_type=" + jo2.getString("social_type"), "cyrus7580@gmail.com", "info@huzon.tv");
									} catch (MessagingException e) {
										e.printStackTrace();
									}
								}
								if(jo2.get("alert_triggered").equals("no"))
								{
									jsonresponse.put("reason", jo2.get("reason"));
								}
							}
							else
							{
								jsonresponse.put("message", "There was a problem inserting the data and/or building a Frame object from it.");
								jsonresponse.put("response_status", "error");
							}
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
		System.out.println("tv.huzon.Endpoint.doGet(): entering...");
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
				/*
				else if (method.equals("resetAllLastAlerts"))
				{	
					String admin_password = request.getParameter("huzon_admin_auth");
					if(admin_password == null)
					{
						// check for designation validity FIXME
						jsonresponse.put("message", "A admin_password value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						if(admin_password.equals("sanders.lov"))
						{
							String station = request.getParameter("station"); // required
							if(station == null || station.isEmpty()) // must always have the time range 
							{
								jsonresponse.put("message", "station was null or empty.");
								jsonresponse.put("response_status", "error");
							}
							else // station ok
							{	
								boolean successful = resetAllLastAlerts(station);
								jsonresponse.put("response_status", "success");
							}
						}
						else
						{
							jsonresponse.put("message", "Incorrect admin_password.");
							jsonresponse.put("response_status", "error");
						}
					}
				}
				
				else if (method.equals("deleteAlert"))
				{	
					String password = request.getParameter("huzon_admin_auth");
					if(password == null)
					{
						// check for designation validity FIXME
						jsonresponse.put("message", "A password value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						String social_type = request.getParameter("social_type"); // required
						String id = request.getParameter("id"); // required
						String designation = request.getParameter("designation");
						if(social_type == null || social_type.isEmpty() || id == null || id.isEmpty() || designation == null || designation.isEmpty()) // must always have the time range 
						{
							jsonresponse.put("message", "designation or id or social_type was empty.");
							jsonresponse.put("response_status", "error");
						}
						else
						{
							if(social_type.equals("facebook"))
							{
								boolean successful = deleteFacebookPost(designation, id);
								if(successful)
								{
									jsonresponse.put("response_status", "success");
									jsonresponse.put("response_from_facebook", "true");
								}
								else
								{
									jsonresponse.put("message", "Error. Could not delete from facebook. Unknown error. Try using graph api explorer instead.");
									jsonresponse.put("response_status", "error");
								}
							}
							else if(social_type.equals("twitter"))
							{
								User user = new User(designation, "designation");
								if(!user.getTwitterAccessToken().isEmpty() && !user.getTwitterAccessTokenSecret().isEmpty())
								{	
									Twitter twitter = new Twitter();
									JSONObject response_from_twitter = twitter.deleteStatus(user.getTwitterAccessToken(), user.getTwitterAccessTokenSecret(), id);
									jsonresponse.put("response_status", "success");
									jsonresponse.put("response_from_twitter", response_from_twitter);
								}
								else
								{
									jsonresponse.put("message", "Error. Could not get user twitter access token and secret");
									jsonresponse.put("response_status", "error");
								}
							}
							else
							{
								jsonresponse.put("message", "Error deleting alert. social_type must be twitter or facebook.");
								jsonresponse.put("response_status", "error");
							}
						}
					}
				}*/
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
	
	JSONObject processNewFrame(Frame newframe)
	{
		JSONObject return_jo = new JSONObject();
		System.out.println("Endpoint.processNewFrame(): Entering processNewFrame(Frame)...");
		try{
			// get all frames over the moving average window backward from this timestamp
			Station station_object = new Station(newframe.getStation());
			TreeSet<Frame> window_frames = station_object.getFrames(newframe.getTimestampInMillis()-5000, newframe.getTimestampInMillis(), null, 0);
			int num_frames_in_window = window_frames.size();
			System.out.println("Endpoint.processNewFrame(): Found " + num_frames_in_window + " frames in the specified window.");
			if(num_frames_in_window < 5)
			{
				System.out.println("Endpoint.processNewFrame(): Warning! Not enough frames in this window. Could be beginning of a recording, though. If so, that's ok.");
				return_jo.put("alert_triggered", "no");
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
				System.out.println("Endpoint.processNewFrame(): Examining " + currentframe.getImageName());
				int x = 0;
				while(x < reporter_designations.length)
				{
					if(reporter_designations[x].equals("alex_hayes") || reporter_designations[x].equals("kristen_kennedy"))
						System.out.println("Endpoint.processNewFrame(): " + reporter_designations[x] + " score= " + currentframe.getScore(reporter_designations[x]));
					reporter_totals[x] = reporter_totals[x] + currentframe.getScore(reporter_designations[x]);
					x++;
				}
			}
			
			// then divide the sum designation_avgs by dividing by the number of frames in the window
			int x = 0;
			double reporter_homogeneity = 0;
			double max_moving_average = 0;
			int max_moving_average_index = 0;
			
			while(x < reporter_totals.length)
			{
				reporter_moving_averages[x] = reporter_totals[x] / num_frames_in_window;
				if(reporter_moving_averages[x] > max_moving_average)
				{
					max_moving_average = reporter_moving_averages[x];
					max_moving_average_index = x;
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
			
			x = 0;
			while(x < reporter_designations.length)
			{
				// does it pass the ma threshold and is it the highest?
				if(reporter_moving_averages[x] > reporter_moving_average_thresholds[x] && reporter_moving_averages[x] == max_moving_average) 
				{
					boolean fb = false;
					boolean tw = false;
					User reporter = new User(reporter_designations[x], "designation");
					if((newframe.getTimestampInMillis() - reporter.getLastFacebookAlert()) > reporter.getFacebookWaitingPeriod())
					{	
						System.out.println(reporter_designations[x] + " passed the moving average threshold for this frame. " + reporter_moving_averages[x] + " > " + reporter_moving_average_thresholds[x]);
						return_jo.put("alert_triggered", "yes");
						return_jo.put("social_type", "facebook");
						return_jo.put("designation", reporter_designations[x]);
						reporter.setLastAlert(newframe.getTimestampInMillis(), "facebook");
						fb = true;
					}
					
					if((newframe.getTimestampInMillis() - reporter.getLastTwitterAlert()) > reporter.getTwitterWaitingPeriod())
					{	
						System.out.println(reporter_designations[x] + " passed the moving average threshold for this frame. " + reporter_moving_averages[x] + " > " + reporter_moving_average_thresholds[x]);
						return_jo.put("alert_triggered", "yes");
						if(fb)
							return_jo.put("social_type", "both");
						else
							return_jo.put("social_type", "facebook");
						return_jo.put("designation", reporter_designations[x]);
						reporter.setLastAlert(newframe.getTimestampInMillis(), "twitter");
						tw = true;
					}
					
					if(fb == false && tw == false)
					{
						return_jo.put("alert_triggered", "no");
						return_jo.put("reason", "Passed MA threshold, but was within waiting period of both facebook and twitter. Couldn't fire alert.");
					}
					
					return return_jo;
				}
				x++;
			}
			return_jo.put("alert_triggered", "no");
			return_jo.put("reason", "none passed MA threshold");
		}
		catch(JSONException jsone)
		{
			jsone.printStackTrace();
		}	
		return return_jo;
	}
	
	
	
	long createAlertInDB(String station, String social_type, String designation, String image_name)
	{
		long returnval = -1L;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			System.out.println("INSERT INTO alerts (`social_type`,`designation`,`image_name`,`livestream_url`,`station`) "
	                    + " VALUES('" + social_type + "','" + designation + "','" + image_name + "','" + "www.wkyt.com/livestream" + "','" + station + "')");
			stmt.executeUpdate(
	                    "INSERT INTO alerts (`social_type`,`designation`,`image_name`,`livestream_url`,`station`) "
	                    + " VALUES('" + social_type + "','" + designation + "','" + image_name + "','" + "www.wkyt.com/livestream" + "','" + station + "')",
	                    Statement.RETURN_GENERATED_KEYS);
			
		    rs = stmt.getGeneratedKeys();

		    if (rs.next()) {
		        returnval = rs.getLong(1);
		    } else {
		    	System.out.println("Endpoint.createAlertInDB(): error getting auto_increment value from row just entered.");
		    }
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			SimpleEmailer se = new SimpleEmailer();
			try {
				se.sendMail("SQLException in Endpoint createAlertInDB", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
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
					se.sendMail("SQLException in Endpoint createAlertInDB", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}  	
		return returnval;
	}
	
	boolean updateAlertText(long id_long, String actual_text)
	{
		boolean returnval = false;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM alerts WHERE id='" + id_long + "'"); 
			if(rs.next())
			{
				rs.updateString("actual_text", actual_text);
				rs.updateRow();
				returnval = true;
			}
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			SimpleEmailer se = new SimpleEmailer();
			try {
				se.sendMail("SQLException in Endpoint updateAlertText", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
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
					se.sendMail("SQLException in Endpoint updateAlertText", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}  	
		return returnval;
	}
	
	// DANGEROUS!!!! This will reset all alerts for every active reporter at this station
	boolean resetAllLastAlerts(String station)
	{
		boolean returnval;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM people WHERE (station='" + station + "' AND active=1)"); 
			while(rs.next())
			{
				rs.updateLong("last_alert_twitter", 0);
				rs.updateLong("last_alert_facebook", 0);
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
				se.sendMail("SQLException in Endpoint resetAllLastAlerts", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
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
					se.sendMail("SQLException in Endpoint resetAllLastAlerts", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}  	
		return returnval;
	}
	
	JSONArray getFiredAlerts(String station, String begin, String end)
	{
		JSONArray returnval = null;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		
		long begin_long = getTimestampInSecondsFromLouisvilleDatestring(begin); // this converts from local to epoch
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(begin_long * 1000); // set time from epoch value
		cal.setTimeZone(TimeZone.getTimeZone("UTC")); // set the cal to UTC before retrieving values
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
		String begin_datestring_in_utc = year  + month + day + "_" + hour24 + minute + second;
		
		long end_long = getTimestampInSecondsFromLouisvilleDatestring(end); // this converts from local to epoch
		cal = Calendar.getInstance();
		cal.setTimeInMillis(end_long * 1000); // set time from epoch value
		cal.setTimeZone(TimeZone.getTimeZone("UTC")); // set the cal to UTC before retrieving values
		year = new Integer(cal.get(Calendar.YEAR)).toString();
		month = new Integer(cal.get(Calendar.MONTH) + 1).toString();
		if(month.length() == 1) { month = "0" + month; }
		day = new Integer(cal.get(Calendar.DAY_OF_MONTH)).toString();
		if(day.length() == 1) { day = "0" + day;} 
		hour24 = new Integer(cal.get(Calendar.HOUR_OF_DAY)).toString();
		if(hour24.length() == 1) { hour24 = "0" + hour24;} 
		minute = new Integer(cal.get(Calendar.MINUTE)).toString();
		if(minute.length() == 1) { minute = "0" + minute;} 
		second = new Integer(cal.get(Calendar.SECOND)).toString();
		if(second.length() == 1) { second = "0" + second;} 
		String end_datestring_in_utc = year  + month + day + "_" + hour24 + minute + second;
		
		
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement();
			System.out.println("Endpoint.getFiredAlerts(): SELECT * FROM alerts WHERE station='" + station + "' AND creation_timestamp BETWEEN STR_TO_DATE('" + begin_datestring_in_utc + "', '%Y%m%d_%H%i%s') AND STR_TO_DATE('" + end_datestring_in_utc + "', '%Y%m%d_%H%i%s')"); 
			rs = stmt.executeQuery("SELECT * FROM alerts WHERE station='" + station + "' AND creation_timestamp BETWEEN STR_TO_DATE('" + begin_datestring_in_utc + "', '%Y%m%d_%H%i%s') AND STR_TO_DATE('" + end_datestring_in_utc + "', '%Y%m%d_%H%i%s')"); 
			int x = 0;
			JSONObject jo = new JSONObject();
			while(rs.next())
			{
				if(x == 0)
				{
					returnval = new JSONArray();
					x = 1;
				}
				jo = new JSONObject();
				jo.put("id",rs.getLong("id"));
				jo.put("social_type",rs.getString("social_type"));
				jo.put("image_name",rs.getString("image_name"));
				jo.put("creation_timestamp",rs.getTimestamp("creation_timestamp"));
				jo.put("designation",rs.getString("designation"));
				jo.put("station",rs.getString("station"));
				jo.put("livestream_url",rs.getString("livestream_url"));
				jo.put("actual_text",rs.getString("actual_text"));
				returnval.put(jo);
			}
			if(x == 0)
			{
				System.out.println("Endpoint.getFiredAlerts(). no results");
			}
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			System.out.println("SQLException in Endpoint getFiredAlerts message=" +sqle.getMessage());
			SimpleEmailer se = new SimpleEmailer();
			try {
				se.sendMail("SQLException in Endpoint getFiredAlerts", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
					se.sendMail("SQLException in Endpoint getFiredAlerts", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}  	
		return returnval;
	} 
	
	private long getTimestampInSecondsFromLouisvilleDatestring(String datestring)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("America/Louisville"));
		cal.set(Calendar.YEAR, Integer.parseInt(datestring.substring(0,4)));
		cal.set(Calendar.MONTH, Integer.parseInt(datestring.substring(4,6)) - 1);
		cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(datestring.substring(6,8)));
		cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(datestring.substring(9,11)));
		cal.set(Calendar.MINUTE, Integer.parseInt(datestring.substring(11,13)));
		cal.set(Calendar.SECOND, Integer.parseInt(datestring.substring(13,15)));
		return (cal.getTimeInMillis()/1000);
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
	
	public boolean isNumeric(String incoming_string) // only works for whole numbers, positive and negative
	{
		  int x=0;
		  while(x < incoming_string.length())
		  {
			  if((x==0 && incoming_string.substring(0,1).equals("-")) ||  // OK if first element is "-"
					  incoming_string.substring(x,x+1).equals("0") || incoming_string.substring(x,x+1).equals("1") ||
					  incoming_string.substring(x,x+1).equals("2") || incoming_string.substring(x,x+1).equals("3") ||
					  incoming_string.substring(x,x+1).equals("4") || incoming_string.substring(x,x+1).equals("5") ||
					  incoming_string.substring(x,x+1).equals("6") || incoming_string.substring(x,x+1).equals("7") ||
					  incoming_string.substring(x,x+1).equals("8") || incoming_string.substring(x,x+1).equals("9"))
			  {
				  // ok
			  }
			  else
			  {
				  return false;
			  }
			  x++;
		  }
		  return true;
	}	
	
	private String[] morning_greetings = {"Good morning", "Morning"};
	private String[] afternoon_greetings = {"Good afternoon", "Afternoon"};
	private String[] evening_greetings = {"Good evening", "Evening"};
	private String[] generic_greetings = {"Hello", "Greetings"};
	private String[] objects = {"Lexington", "Bluegrass", "Central Kentucky", "everyone", "folks", "viewers"};
	
	
	String getMessage(String social_type, long timestamp_in_seconds, long redirect_id)
	{
		String returnval = "";
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("America/Louisville"));
		cal.setTimeInMillis(timestamp_in_seconds * 1000);
		ArrayList<String> greeting_choices = new ArrayList<String>();
		if(cal.get(Calendar.HOUR_OF_DAY) < 12)
		{	
			for(int x = 0; x < morning_greetings.length; x++)
				greeting_choices.add(morning_greetings[x]);
			for(int x = 0; x < generic_greetings.length; x++)
				greeting_choices.add(generic_greetings[x]);
		}
		else if(cal.get(Calendar.HOUR_OF_DAY) < 18)
		{	
			for(int x = 0; x < afternoon_greetings.length; x++)
				greeting_choices.add(afternoon_greetings[x]);
			for(int x = 0; x < generic_greetings.length; x++)
				greeting_choices.add(generic_greetings[x]);
		}
		else if(cal.get(Calendar.HOUR_OF_DAY) < 24)
		{	
			for(int x = 0; x < evening_greetings.length; x++)
				greeting_choices.add(evening_greetings[x]);
			for(int x = 0; x < generic_greetings.length; x++)
				greeting_choices.add(generic_greetings[x]);
		}
			
		ArrayList<String> object_choices = new ArrayList<String>();
		for(int x = 0; x < objects.length; x++)
			object_choices.add(objects[x]);
		
		Random random = new Random();
		int greetings_index = random.nextInt(greeting_choices.size());
		int objects_index = random.nextInt(object_choices.size());
		int hour = cal.get(Calendar.HOUR);
		if(hour == 0)
			hour = 12;
		int minute = cal.get(Calendar.MINUTE);
		String minutestring = (new Integer(minute)).toString();
		if(minutestring.length() < 2)
			minutestring = "0" + minutestring;
		String am_or_pm_string = "";
		if(cal.get(Calendar.AM_PM) == 0)
			am_or_pm_string = " AM";
		else
			am_or_pm_string = " PM";
		String ts_string = hour + ":" + minutestring + am_or_pm_string;
		
		int selector = random.nextInt(4);
		
		if(social_type.equals("facebook"))
		{
			if(selector == 0) // no greeting,  "I", no "right now", "watch", timestamp last
				returnval = "I am on the air. Tune in or watch the live stream here: huzon.wkyt.com/livestream?id=" + redirect_id + " -- " + ts_string;  
			else if (selector == 1) // no greeting, "we", "live", "catch" timestamp first
				returnval = "The time is " + ts_string + " and we are live on the air. Tune in or watch the live stream here: huzon.wkyt.com/livestream?id=" + redirect_id;
			else if (selector == 2) // greeting, "I", "right now", "watch", timestamp last 
				returnval = greeting_choices.get(greetings_index) + ", " + object_choices.get(objects_index) + ". I am on-air right now. Tune in or watch the live stream here: huzon.wkyt.com/livestream?id=" + redirect_id + " -- " + ts_string;
			else if (selector == 3) // greeting, "I", no "right now", "view", timestamp after greeting 
				returnval = greeting_choices.get(greetings_index) + ", " + object_choices.get(objects_index) + ". It is " + ts_string + " and I am on-air. Tune in or watch the live stream here: huzon.wkyt.com/livestream?id=" + redirect_id; 
		}
		else if(social_type.equals("twitter"))
		{
			if(selector == 0 || selector == 1)
				returnval = "I'm on the air right now (" + ts_string + "). Tune in or watch the live stream here: huzon.wkyt.com/livestream?id=" + redirect_id + " #wkyt";  
			else if(selector == 2 || selector == 3)
				returnval = greeting_choices.get(greetings_index) + ", " + object_choices.get(objects_index) + ". I'm on the air (" + ts_string + "). Tune in or stream here: huzon.wkyt.com/livestream?id=" + redirect_id + " #wkyt";  
		}
		return returnval;
	}
	
	public boolean deleteFacebookPost(String designation, String item_id)
	{
		 boolean successful = false;
		try {
			HttpClient httpClient = new DefaultHttpClient();
			User user = new User(designation, "designation");
			HttpDelete hd = new HttpDelete("https://graph.facebook.com/" + item_id + "?access_token=" + user.getFacebookSubAccount().getString("facebook_page_access_token"));
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
	
	public static void main(String[] args) {
		Endpoint e = new Endpoint();
		e.processNewFrame(new Frame(1371291193612L, "wkyt"));
	}
	
}

/*
JSONObject processNewFrame(long ts_long, String station, int moving_average_window_int, 
		double ma_modifier_double, double single_modifier_double, int awp_twitter, int awp_facebook, boolean simulation)
{
	
	 //* this function takes certain parameters and simulates what would happen if this were a realtime new 
	 //* frame coming into the system and we were checking for alerts. 
	 //* 
	 //* 1. It gets X frames, cycles through them, putting together a JSONArray of X frames with timestamp, image name and scores object for each frame.  (see structure below)
	 //* 2. It then cycles through the designations array getting an average score for each designation over the window, creating a designation_averages_ja and finding the max avg
	 //* 3. If the max average is greater than the moving average threshold for that person and <strike>the last frame</strike> any frame in the window is greater than the singlethresh... 
	 //* 4. then respond with all the information about that frame.
	 
	 
	
	JSONObject jsonresponse = new JSONObject();
	try
	{
		Station station_object = new Station(station); 
		TreeSet<User> reporter_objects_ts = new TreeSet<User>();
		TreeSet<String> reporter_strings_ts = station_object.getReporters();
		Iterator<String> reporter_it = reporter_strings_ts.iterator();
		while(reporter_it.hasNext())
		{
			reporter_objects_ts.add(new User(reporter_it.next(),"designation"));
		}
		//JSONArray dnh_ja = getDesignationsAndHomogeneities(station); // get the designations and homogeneities for WKYT
		//JSONArray designations_ja = getReporterDesignations(station);
		try
		{
			ResultSet rs = null;
			Connection con = null;
			Statement stmt = null;
			try
			{
				con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
				stmt = con.createStatement();
				// search database for X frames
				rs = stmt.executeQuery("SELECT * FROM frames_" + station + " WHERE (timestamp_in_seconds > " + (ts_long - moving_average_window_int) + " AND timestamp_in_seconds <= " + ts_long + ") ORDER BY timestamp_in_seconds ASC"); 
				rs.last();
				jsonresponse.put("frames_processed", rs.getRow());  // get a row count
				if(rs.getRow() == 0) // no frames at all
				{
					jsonresponse.put("message", "No frames for this entire window. Returning with next_frame.");
					jsonresponse.put("response_status", "error");
				}
				else if(rs.getRow() < moving_average_window_int) // missing a frame
				{
					jsonresponse.put("message", "missing a frame in the target window, cant draw safe conclusions");
					jsonresponse.put("error_code", "100");
					jsonresponse.put("response_status", "error");
				}
				else // had all X frames
				{	
					System.out.println("Endpoint.processNewFrame(): had all " + moving_average_window_int + " frames.");
					// check the ts frame. One of the designations' average scores has to be above their single threshold to continue. If not, quick failure.
						rs.beforeFirst(); // go back to the beginning for parsing
						JSONArray frames_ja = new JSONArray();
						JSONObject current_frame_jo = null;
						JSONObject current_frame_scores_jo = null;
						
						//{
						//	"response_status": "success",
						//	"frames":
						//		[
						//			{
						//				"timestamp_in_seconds": 12312312,
						//				"image_name": "url",
						//				"scores":
						//					{
						//						"person_a": .984332,
						//						"person_b": .3423423
						//						...
						//					}
						//			}
						//			...											
						//		],
						//	"averages":
						//		[
						//			{
						//				"designation": "person_a",
						//				"average": .9754
						//			},
						//			{
						//				"designation": "person_b",
						//				"average": .5432
						//			},
						//			...
						//		]
						//}
						// convert the maw_int frames into a jsonarray called frames_ja
						while(rs.next()) // get one row
						{
							current_frame_jo = new JSONObject();
							current_frame_jo.put("image_name", rs.getString("image_name"));
							current_frame_jo.put("timestamp_in_seconds", rs.getLong("timestamp_in_seconds"));
							current_frame_scores_jo = new JSONObject();
							Iterator<User> it = reporter_objects_ts.iterator();
							User currentuser = null;
							while(it.hasNext())
							{
								currentuser = it.next();
								current_frame_scores_jo.put(currentuser.getDesignation(), rs.getDouble(currentuser.getDesignation() + "_avg"));
							}
							current_frame_jo.put("scores", current_frame_scores_jo);
							frames_ja.put(current_frame_jo);
						}
						
						
						//(designation_averages_ja)
						//[
						//	{ "designation" : "guy_mcperson", "average": .4345 },
						//	{ "designation" : "suzi_cue", "average": .6555 }
						//	...
						//]
						
						// compute the window averages for each designation
						double person_total = 0.0;
						double person_avg = 0.0;
						JSONArray designation_averages_ja = new JSONArray(); // this is a simple jsonarray where we will put {"designation":"some_des","average":0.342} objects
						JSONObject jo = new JSONObject();
						double max_avg = 0.0;
						String max_designation = "";
						Iterator<User> it = reporter_objects_ts.iterator();
						User currentuser = null;
						while(it.hasNext())
						{
							currentuser = it.next();
							jo = new JSONObject();
							person_total = 0.0;							// the total for the current designation starts off as 0
							for(int j = 0; j < frames_ja.length(); j++) // loop through the frames for this window
							{	
								person_total = person_total + frames_ja.getJSONObject(j).getJSONObject("scores").getDouble(currentuser.getDesignation()); // add to the total for this person
							}
							person_avg = person_total/frames_ja.length();
							jo.put("designation", currentuser.getDesignation());
							jo.put("average", person_avg);
							if(person_avg > max_avg)
							{
								max_avg = person_avg;
								max_designation = currentuser.getDesignation();
							}
							designation_averages_ja.put(jo);
						}
													
						System.out.println("Endpoint.processNewFrame(): max_designation=" + max_designation + " and max_avg=" + max_avg);
						
						// max_avg is now set to the highest average found for the designations
						// max_designation is set to the designation of whomever that max_avg belonged to
																		
						String twitter_handle = null;
						User user = new User(max_designation, "designation");
						double max_homogeneity_double = user.getHomogeneity();
						if(max_avg > (max_homogeneity_double * ma_modifier_double)) // the moving average is greater than the moving average threshold
						{
							System.out.println("Endpoint.processNewFrame(): datestring=" + getLouisvilleDatestringFromTimestamp(ts_long,"seconds") + " max_designation=" + max_designation + " and max_avg=" + max_avg + " > " + (max_homogeneity_double * ma_modifier_double) + " <-- ma thresh PAST THRESH");
							// FIXME The above logic may not be right. It's conceivable that the max_avg is the highest, but not the most signficant.
							// For instance, let's say the max avg is .9 but for that person, their homogeneity is .999
							// There could be another avg that is .89 while their homogeneity is .895. 
							// This is probably a minor concern since these should be canceled out by delta-checking anyway.
							
							// at this point we know the following:
							// (a) the value of the highest moving average across the window
							// (b) who that highest moving average belongs to
							// we need to know (c) the maximum single frame score for the designation over the previous maw_int-1 frames
							// and (d) for c, does that pass the designation's single thresh?

							// so, loop through the frames and check the scores for the designation
							double max_frame_score_for_designation_with_max_average = 0.0;
							int window_index_of_max_score_for_designation_with_max_average = 0;
							long timestamp_in_seconds_for_frame_with_highest_score_across_window_for_designation_with_max_average = 0L;
							String image_name_of_frame_with_highest_score_in_window = "";
							double currentscore = 0.0;
							for(int j = 0; j < frames_ja.length(); j++) // loop through the frames for this window
							{	
								currentscore = frames_ja.getJSONObject(j).getJSONObject("scores").getDouble(max_designation);
								if(currentscore > max_frame_score_for_designation_with_max_average)
								{
									max_frame_score_for_designation_with_max_average = currentscore;
									window_index_of_max_score_for_designation_with_max_average = j;
									timestamp_in_seconds_for_frame_with_highest_score_across_window_for_designation_with_max_average = frames_ja.getJSONObject(j).getLong("timestamp_in_seconds");
									image_name_of_frame_with_highest_score_in_window = frames_ja.getJSONObject(j).getString("image_name");
								}
							}
							double designation_score_for_last_frame_in_window = currentscore;
							// now we additionally know:
							// (c) the maximum single frame score for the designation, the index of that frame within the window, the name of the image, and the timestamp of it
							// need to know: (d) does that score cross the single frame threshold?
							if(max_frame_score_for_designation_with_max_average > (max_homogeneity_double * single_modifier_double)) 
							{
								System.out.println("Endpoint.processNewFrame(): datestring=" + getLouisvilleDatestringFromTimestamp(ts_long,"seconds") + " max_frame_score in window=" + max_frame_score_for_designation_with_max_average + " > " + (max_homogeneity_double * single_modifier_double) + " <-- single thresh PAST THRESH");
								
								// (d) yes it does
								long last_alert_twitter = user.getLastTwitterAlert();
								boolean alert_triggered_twitter = false;
								if((ts_long - last_alert_twitter) > awp_twitter)
								{
									user.setLastAlert(ts_long, "twitter");
									alert_triggered_twitter = true;
									jsonresponse.put("alert_triggered_twitter", "yes");
									twitter_handle = user.getTwitterHandle();
									if(twitter_handle != null)
									{
										jsonresponse.put("designation_twitter_handle",  user.getTwitterHandle());
										
										if(!user.getTwitterAccessToken().isEmpty() && !user.getTwitterAccessTokenSecret().isEmpty())
										{
											jsonresponse.put("twitter_access_token",user.getTwitterAccessToken());
											jsonresponse.put("twitter_access_token_secret",user.getTwitterAccessTokenSecret());
											if(!simulation)
											{	
												long twitter_redirect_id = createAlertInDB("wkyt", "twitter", max_designation ,image_name_of_frame_with_highest_score_in_window); 
												jsonresponse.put("twitter_redirect_id", twitter_redirect_id);
												String message = getMessage("twitter", ts_long, twitter_redirect_id);
												jsonresponse.put("twitter_message_firstperson", message);
												boolean successful = updateAlertText(twitter_redirect_id, message);
											}
										}
										else
										{
											if(!simulation)
											{	
												SimpleEmailer se = new SimpleEmailer();
												try {
													se.sendMail("Invalid Twitter info", "Alert fired, but twitter_stuff was null or invalid for " + max_designation , "cyrus7580@gmail.com", "info@huzon.tv");
												} catch (MessagingException e) {
													e.printStackTrace();
												}
											}
										}
									}
								}
								
								
								long last_alert_facebook = user.getLastFacebookAlert();
								boolean alert_triggered_facebook = false;
								System.out.println("Endpoint processNewFrame(): ts_long=" + ts_long + " last_alert_facebook=" + last_alert_facebook + " diff=" + (ts_long - last_alert_facebook) + " vs awp_facebook=" + awp_facebook);
								if((ts_long - last_alert_facebook) > awp_facebook)
								{	 
									user.setLastAlert(ts_long, "facebook");
									alert_triggered_facebook = true;
									jsonresponse.put("alert_triggered_facebook", "yes");
									JSONObject facebook_stuff = user.getFacebookSubAccount(); 
									if(facebook_stuff != null)
									{
										jsonresponse.put("facebook_page_id",facebook_stuff.getLong("facebook_page_id"));
										jsonresponse.put("facebook_page_access_token",facebook_stuff.getString("facebook_page_access_token"));
										jsonresponse.put("facebook_page_name",facebook_stuff.getString("facebook_page_name"));
										if(!simulation)
										{	
											long facebook_redirect_id = createAlertInDB("wkyt", "facebook", max_designation, image_name_of_frame_with_highest_score_in_window); 
											jsonresponse.put("facebook_redirect_id", facebook_redirect_id);
											String message = getMessage("facebook", ts_long, facebook_redirect_id);
											jsonresponse.put("facebook_message_firstperson",message);
											boolean successful = updateAlertText(facebook_redirect_id, message);
										}
									}
									else
									{
										if(!simulation)
										{	
											SimpleEmailer se = new SimpleEmailer();
											try {
												se.sendMail("Invalid facebook info", "Alert fired, but facebook_stuff was null or invalid for designation " + max_designation + ". facebook_stuff=" + facebook_stuff, "cyrus7580@gmail.com", "info@huzon.tv");
											} catch (MessagingException e) {
												e.printStackTrace();
											}
										}
									}
								}
								
								// NOTE: The above is set up where "alert_triggered_[social_type]" can be set to "yes" even if the "[social_type]_stuff" is missing or invalid
								// this is to note when an alert SHOULD have happened and update the database accordingly, even if the social account credentials are missing.
								
								if(alert_triggered_twitter || alert_triggered_facebook) // at least one of the two was true, add all the necessary information
								{	
									jsonresponse.put("alert_triggered", "yes");
									jsonresponse.put("designation", max_designation);
									jsonresponse.put("designation_moving_average_over_window", max_avg);
									jsonresponse.put("designation_score_for_last_frame_in_window", designation_score_for_last_frame_in_window);
									jsonresponse.put("designation_highest_frame_score_in_window", max_frame_score_for_designation_with_max_average);
									jsonresponse.put("index_of_designation_highest_frame_score_in_window", window_index_of_max_score_for_designation_with_max_average);
									jsonresponse.put("designation_display_name", user.getDisplayName());
									jsonresponse.put("designation_homogeneity_score", max_homogeneity_double);
									jsonresponse.put("designation_moving_average_threshold", max_homogeneity_double * ma_modifier_double);
									jsonresponse.put("designation_single_threshold", max_homogeneity_double * single_modifier_double);
									jsonresponse.put("datestring_of_last_frame_in_window", getLouisvilleDatestringFromTimestamp(ts_long,"seconds"));
									jsonresponse.put("datestring_of_frame_with_highest_score_in_window", getLouisvilleDatestringFromTimestamp(timestamp_in_seconds_for_frame_with_highest_score_across_window_for_designation_with_max_average, "seconds"));
									//jsonresponse.put("image_name_of_last_frame_in_window", getLouisvilleDatestringFromTimestamp(ts_long, "seconds") + ".jpg");
									jsonresponse.put("image_name_of_frame_with_highest_score_in_window", image_name_of_frame_with_highest_score_in_window);
								}
								else
								{
									jsonresponse.put("alert_triggered", "no");
								}
							}
							else
							{
								System.out.println("Endpoint.processNewFrame(): datestring=" + getLouisvilleDatestringFromTimestamp(ts_long, "seconds") + " max_frame_score in window=" + max_frame_score_for_designation_with_max_average + " <= " + (max_homogeneity_double * single_modifier_double) + " <-- single thresh");
								// (d) no it doesn't
								jsonresponse.put("alert_triggered", "no");
							}
						}
						else
						{
							System.out.println("Endpoint.processNewFrame(): datestring=" + getLouisvilleDatestringFromTimestamp(ts_long, "seconds") + " max_designation=" + max_designation + " and max_avg=" + max_avg + " <= " + (max_homogeneity_double * ma_modifier_double) + " <-- ma thresh");
							jsonresponse.put("alert_triggered", "no");
						}
						//jsonresponse.put("designation_averages", designation_averages_ja);
						jsonresponse.put("response_status", "success");
						//jsonresponse.put("frames", frames_ja);
				}
			}
			catch(SQLException sqle)
			{
				jsonresponse.put("message", "Error sqle.getMessage()=" + sqle.getMessage());
				jsonresponse.put("response_status", "error");
				sqle.printStackTrace();
				SimpleEmailer se = new SimpleEmailer();
				try {
					se.sendMail("SQLException in Endpoint processNewFrame", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
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
					jsonresponse.put("warning", "Problem closing resultset, statement and/or connection to the database.");
					SimpleEmailer se = new SimpleEmailer();
					try {
						se.sendMail("SQLException in Endpoint processNewFrame", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
					} catch (MessagingException e) {
						e.printStackTrace();
					}
				}
			}   	
			
		}
		catch(NumberFormatException nfe)
		{
			jsonresponse.put("message", "threshold was not a valid double value");
			jsonresponse.put("response_status", "error");
		}	
	}
	catch(JSONException jsone)
	{
		System.out.println("endpoint: JSONException thrown in large try block. " + jsone.getMessage());
	}	
	return jsonresponse;
}
*/


