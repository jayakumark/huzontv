package tv.huzon;

import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.mail.MessagingException;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.Media;
import facebook4j.auth.AccessToken;

public class SocialUploaderCallable implements Callable<JSONObject> {

	Frame frame2upload;
	User reporter;
	Station station_object;
	String social_type;
	String which_lock;

	public SocialUploaderCallable(Frame inc_frame2upload, User inc_reporter, Station inc_station_object, String inc_social_type, String inc_which_lock)
	{
		frame2upload = inc_frame2upload;
		station_object = inc_station_object;
		reporter = inc_reporter;
		social_type = inc_social_type;
		which_lock = inc_which_lock;
	}

	public String getMissingCredentialsEmailMessage()
	{
		String message = "";

			message = 
				reporter.getDisplayName() + 
				",\n\nAn alert triggered for you with huzon.tv. However, our system was unable to actually fire the alert because your " + social_type + " account " + 
				"has become disconnected from huzon.tv. This can happen for several reasons:" +
				"\n\n- You disabled the huzon.tv app in your " + social_type + " configuration" +
				"\n- Your " + social_type + " account was never linked to huzon.tv in the first place"+
				"\n\nPlease go to https://www.huzon.tv/registration.html to link your " + social_type + " account to huzon.tv and enable automated alerts. " +
				"Thanks!\n\nhuzon.tv staff";
			return message;
	}	

	public JSONObject call() {

		String social_abbrev = "";
		if(social_type.equals("twitter"))
			social_abbrev = "TW";
		else if(social_type.equals("facebook"))
			social_abbrev = "FB";

		SimpleEmailer se = new SimpleEmailer();
		boolean social_successful = false;
		String social_failure_message = "Message not set.";
		
		User postinguser = null;
		if(which_lock.equals("master"))
			postinguser = new User(station_object.getMasterDesignation(), "designation");
		else
			postinguser = reporter;
		
		String uuid = UUID.randomUUID().toString();
		boolean successfullylocked = station_object.lock(uuid, social_type, which_lock);
		if(successfullylocked)
		{	
			try
			{
				(new Platform()).addMessageToLog("" + social_abbrev + " triggered for " + reporter.getDesignation() + " and station is " + social_abbrev + " active " + which_lock);
				if(!reporter.isSocialActive(social_type))
				{
					social_successful = false;
					social_failure_message = "Reporter is not " + social_type + "_active";
					(new Platform()).addMessageToLog("" + social_abbrev + " triggered but suppressed for " + reporter.getDesignation() + ". " + social_type + "_active=false");
				}
				else // station is active and reporter is active for this social_type. 
				{
					// the mode is live or test, we will either be firing a social alert OR sending an email about missing or invalid credentials. Both actions need a lock to prevent doubles.
					if(postinguser.getTwitterAccessToken() == null || postinguser.getTwitterAccessToken().equals("") || postinguser.getTwitterAccessTokenSecret() == null || postinguser.getTwitterAccessTokenSecret().equals(""))
					{
						social_successful = false;
						social_failure_message = "user " + postinguser.getDesignation() + " has no " + social_abbrev + " credentials";
						(new Platform()).addMessageToLog("" + social_abbrev + " triggered for " + reporter.getDesignation() + " but failed due to lack of credentials. user=" + postinguser.getDesignation() + ". which_lock=" + which_lock);
						if(which_lock.equals("individual"))
						{
							String emailmessage = getMissingCredentialsEmailMessage();
							se.sendMail("Action required: huzon.tv " + social_abbrev + " alert was unable to fire. Please link your accounts.", emailmessage, reporter.getEmail(), "info@huzon.tv");
							(new Platform()).addMessageToLog(reporter.getDesignation() + " was notified of MISSING " + social_abbrev + " credentials.");
						}
					}
					else // postinguser appears to have social credentials
					{
						boolean faces_only = true;
						URL[] image_urls = frame2upload.get2x2CompositeURLs(faces_only, reporter.getDesignation());
						if(image_urls == null)
						{
							social_successful = false;
							social_failure_message = "image_urls was null coming back from Frame.get2x2CompositeURLs()";
							(new Platform()).addMessageToLog("" + social_abbrev + " triggered for " + reporter.getDesignation() + " with cred. but 2x2 failed. user=" + postinguser.getDesignation() + ". which_lock=" + which_lock);
						}
						else
						{
							File[] image_files = new File[4];
							String tmpdir = System.getProperty("java.io.tmpdir");
							String[] image_filenames = new String[4];
							image_filenames[0] = tmpdir + "/" + UUID.randomUUID() + ".jpg";
							image_filenames[1] = tmpdir + "/" + UUID.randomUUID() + ".jpg";
							image_filenames[2] = tmpdir + "/" + UUID.randomUUID() + ".jpg";
							image_filenames[3] = tmpdir + "/" + UUID.randomUUID() + ".jpg";

							System.out.println("SocialUploaderCallable.call(): downloading " + image_urls[0] + " and saving to " + image_filenames[0]);
							ReadableByteChannel rbc0 = Channels.newChannel(image_urls[0].openStream());
							FileOutputStream fos0 = new FileOutputStream(image_filenames[0]); // FIXME this might have conflicts with multiple stations
							fos0.getChannel().transferFrom(rbc0, 0, Long.MAX_VALUE);
							image_files[0] = new File(image_filenames[0]);
							fos0.close();

							System.out.println("SocialUploaderCallable.call(): downloading " + image_urls[1] + " and saving to " + image_filenames[1]);
							ReadableByteChannel rbc1 = Channels.newChannel(image_urls[1].openStream());
							FileOutputStream fos1 = new FileOutputStream(image_filenames[1]); // FIXME this might have conflicts with multiple stations
							fos1.getChannel().transferFrom(rbc1, 0, Long.MAX_VALUE);
							image_files[1] = new File(image_filenames[1]);
							fos1.close();

							System.out.println("SocialUploaderCallable.call(): downloading " + image_urls[2] + " and saving to " + image_filenames[2]);
							ReadableByteChannel rbc2 = Channels.newChannel(image_urls[2].openStream());
							FileOutputStream fos2 = new FileOutputStream(image_filenames[2]); // FIXME this might have conflicts with multiple stations
							fos2.getChannel().transferFrom(rbc2, 0, Long.MAX_VALUE);
							image_files[2] = new File(image_filenames[2]);
							fos2.close();

							System.out.println("SocialUploaderCallable.call(): downloading " + image_urls[3] + " and saving to " + image_filenames[3]);				
							ReadableByteChannel rbc3 = Channels.newChannel(image_urls[3].openStream());
							FileOutputStream fos3 = new FileOutputStream(image_filenames[3]); // FIXME this might have conflicts with multiple stations
							fos3.getChannel().transferFrom(rbc3, 0, Long.MAX_VALUE);
							image_files[3] = new File(image_filenames[3]);
							fos3.close();

							try 
							{
								// create the new image, canvas size is the max. of both image sizes
								int w = 2560;
								int h = 1440;
								BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

								// load source images
								// paint both images, preserving the alpha channels
								Graphics G = combined.getGraphics();
								BufferedImage image0 = ImageIO.read(image_files[0]);
								G.drawImage(image0, 0, 0, null);
								BufferedImage image1 = ImageIO.read(image_files[1]);
								G.drawImage(image1, 1280, 0, null);
								BufferedImage image2 = ImageIO.read(image_files[2]);
								G.drawImage(image2, 0, 720, null);
								BufferedImage image3 = ImageIO.read(image_files[3]);
								G.drawImage(image3, 1280, 720, null);

								BufferedImage after = new BufferedImage(w/4, h/4, BufferedImage.TYPE_INT_ARGB);
								AffineTransform at = new AffineTransform();
								at.scale(0.25, 0.25);
								AffineTransformOp scaleOp = 
								   new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
								after = scaleOp.filter(combined, after);

								// Save as new image
								String composite_filename = tmpdir + "/" + UUID.randomUUID() + ".png";
								System.out.println("SocialUploaderCallable.call(): writing composite image.");
								ImageIO.write(after, "PNG", new File(composite_filename));
								File composite_file = new File(composite_filename);

								image_files[0].delete();
								image_files[1].delete();
								image_files[2].delete();
								image_files[3].delete();

								Platform p = new Platform();
								long redirect_id = p.createAlertInDB(station_object, social_type, reporter.getDesignation(), frame2upload.getURLString(), postinguser);
								String message = station_object.getMessage(social_type, frame2upload.getTimestampInMillis(), redirect_id, reporter);

								if(social_type.equals("twitter"))
								{	
									Twitter twitter = new Twitter();
									JSONObject twit_jo = null;
									twit_jo = twitter.updateStatusWithMedia(postinguser.getTwitterAccessToken(), postinguser.getTwitterAccessTokenSecret(), message, composite_file);

									if(twit_jo.has("response_status") && twit_jo.getString("response_status").equals("error")) // if an error was produced
									{
										social_successful = false;
										social_failure_message = "Twitter produced an error: " + twit_jo.getString("message");

										if(twit_jo.has("twitter_code") && (twit_jo.getInt("twitter_code") == 32 || twit_jo.getInt("twitter_code") == 89)) // and it was due to bad credentials
										{
											if(which_lock.equals("individual"))
											{
												reporter.resetTwitterCredentialsInDB(); // the user's credentials are no good anymore. Delete them to allow the user to start over. (Link is in email below)
												String emailmessage = getMissingCredentialsEmailMessage();
												se.sendMail("Action required: huzon.tv Twitter alert was unable to fire. Please link your accounts.", emailmessage, reporter.getEmail(), "info@huzon.tv");
												(new Platform()).addMessageToLog(reporter.getDesignation() + " was notified of INVALID twitter credentials");
											}	
											else
											{
												(new Platform()).addMessageToLog("test account " + social_abbrev + " creds invalid trying to fire for " + reporter.getDesignation() + ". user=" + postinguser.getDesignation() + ". which_lock=" + which_lock);
											}
										}
										else if(twit_jo.has("twitter_code"))
										{
											(new Platform()).addMessageToLog(reporter.getDesignation() + " unknown twitter error. There was an unknown error trying to tweet. twit_jo=" + twit_jo + "user=" + postinguser.getDesignation() + ". which_lock=" + which_lock);
										}
										else
										{
											(new Platform()).addMessageToLog(reporter.getDesignation() + " some other twitter error. There was some other error trying to tweet which DID NOT produce a twitter_code. twit_jo=" + twit_jo + " user=" + postinguser.getDesignation() + ". which_lock=" + which_lock);
										}
									}
									else
									{
										social_successful = true;
										(new Platform()).addMessageToLog("" + social_abbrev + " successful for " + reporter.getDesignation() + " user=" + postinguser.getDesignation() + ". which_lock=" + which_lock);
										// if either of these fail, alert the admin within the functions themselves
										boolean alert_text_update_successful = p.updateAlertText(redirect_id, message);
										boolean social_id_update_successful = p.updateSocialItemID(redirect_id,twit_jo.getString("id"));
										if(alert_text_update_successful && social_id_update_successful)
											se.sendMail("" + social_abbrev + " successful for " + reporter.getDesignation(), "Text and social id updated correctly, too. user=" + postinguser.getDesignation() + ". which_lock=" + which_lock + "\n\nhttps://www.huzon.tv/controlpanel.html", "cyrus7580@gmail.com", "info@huzon.tv");
										else
											se.sendMail("" + social_abbrev + " (mostly) successful for " + reporter.getDesignation(), "Text and social id did not update correctly in the DB, though, though. user=" + postinguser.getDesignation() + ". which_lock=" + which_lock + "\n\nhttps://www.huzon.tv/controlpanel.html", "cyrus7580@gmail.com", "info@huzon.tv");
									}
								}
								else if(social_type.equals("facebook"))
								{
									Facebook facebook = new FacebookFactory().getInstance();
									facebook.setOAuthAppId("176524552501035", "dbf442014759e75f2f93f2054ac319a0");
									facebook.setOAuthPermissions("publish_stream,manage_page");
									facebook.setOAuthAccessToken(new AccessToken(postinguser.getFacebookPageAccessToken(), null)); // if "test" get huzontv access token, if "live" get reporter's access token

									String facebookresponse = "";
									try {
										facebookresponse = facebook.postPhoto(new Long(postinguser.getFacebookPageID()).toString(), new Media(composite_file), message, null, false); // FIXME hardcode to wkyt station "33684860765"
										social_successful = true;
										//return_jo.put("facebook_successful", true); // if no exception thrown above, we get to this statement and assume post was successful, regardless of two db updates below
										(new Platform()).addMessageToLog("FB successful for " + reporter.getDesignation() + ". Actual FB response=" + facebookresponse + " user=" + postinguser.getDesignation() + ". which_lock=" + which_lock);
										// if either of these fail, notify admin from within functions themselves
										boolean alert_text_update_successful = p.updateAlertText(redirect_id, message);
										boolean social_id_update_successful = p.updateSocialItemID(redirect_id, facebookresponse);
										if(alert_text_update_successful && social_id_update_successful)
											se.sendMail("FB successful for " + reporter.getDesignation(), "Text and social id updated correctly, too. Actual FB response=" + facebookresponse + " user=" + postinguser.getDesignation() + ". which_lock=" + which_lock + "\n\nhttps://www.huzon.tv/controlpanel.html", "cyrus7580@gmail.com", "info@huzon.tv");
										else
											se.sendMail("FB (mostly) successful for " + reporter.getDesignation(), "Text and social id did not update correctly in the DB, though, though. Actual FB response=" + facebookresponse + " user=" + postinguser.getDesignation() + ". which_lock=" + which_lock + "\n\nhttps://www.huzon.tv/controlpanel.html", "cyrus7580@gmail.com", "info@huzon.tv");
									} 
									catch (FacebookException e) 
									{
										social_successful = false;
										social_failure_message = "facebook produced an error: " + e.getErrorMessage();
										//return_jo.put("facebook_successful", false);
										//return_jo.put("facebook_failure_message", e.getErrorMessage());

										if((e.getErrorCode() == 190) || (e.getErrorCode() == 100)) // if one of these errors was generated...
										{
											if(which_lock.equals("individual"))
											{	
												reporter.resetFacebookCredentialsInDB(); // the credentials are no good anymore. Delete them to allow the user to start over.
												String emailmessage = getMissingCredentialsEmailMessage();
												se.sendMail("Action required: huzon.tv FB alert was unable to fire. Please link your accounts.", emailmessage, reporter.getEmail(), "info@huzon.tv");
												(new Platform()).addMessageToLog(reporter.getDesignation() + " was notified of invalid FB credentials. Actual FB response=" + facebookresponse + " user=" + postinguser.getDesignation() + ". which_lock=" + which_lock);
											}
											else
												(new Platform()).addMessageToLog("test account FB creds invalid trying to fire for " + reporter.getDesignation() + ". Actual FB response=" + facebookresponse + "\nuser=" + postinguser.getDesignation() + ". which_lock=" + which_lock);
										}
										else
										{	
											e.printStackTrace();
											(new Platform()).addMessageToLog("Failed facebook photo post. Unknown error. (This is not a user-has-not-linked issue.) Actual FB response=" + facebookresponse + " " + reporter.getDesignation() + " " + 
													new Long(reporter.getFacebookPageID()).toString() + " " + message + " " + e.getMessage() + "\nuser=" + postinguser.getDesignation() + ". which_lock=" + which_lock);
										}
									}
								}
								composite_file.delete();
							} catch (IOException e) {
								(new Platform()).addMessageToLog("IOException trying to create composite image and post to " + social_type + " for " + reporter.getDesignation() + ". " + e.getMessage() + " user=" + postinguser.getDesignation() + ". which_lock=" + which_lock);
							}
						}
					}
				
				}// end else (i.e. mode is live or test"
			}
			catch(MalformedURLException murle)
			{
				social_successful = false;
				social_failure_message = "MalformedURLException " + murle.getMessage();
				murle.printStackTrace();
			}
			catch (FileNotFoundException e) 
			{
				social_successful = false;
				social_failure_message = "FileNotFoundException " + e.getMessage();
				e.printStackTrace();
			} 
			catch (IOException e) 
			{
				social_successful = false;
				social_failure_message = "IOException " + e.getMessage();
				e.printStackTrace();
			} 
			catch (JSONException e) 
			{
				social_successful = false;
				social_failure_message = "JSONException " + e.getMessage();
				e.printStackTrace();
			} 
			catch (MessagingException e) 
			{
				social_successful = false;
				social_failure_message = "MessagingException " + e.getMessage();
				e.printStackTrace();
			}
			
			station_object.unlock(uuid, social_type, which_lock);		
		}
		else
		{
			social_successful = false;
			social_failure_message = "Failed to set " + social_abbrev + " " + which_lock + " lock for this station. It seems to be in use already.";
			(new Platform()).addMessageToLog("Skipped " + social_abbrev + " action (post or email) for " + reporter.getDesignation() + ". Tried to set " + which_lock + " lock but station.lock() returned false.  user=" + postinguser.getDesignation() + ". which_lock=" + which_lock);
		}
		
		JSONObject return_jo = new JSONObject();
		try
		{
			if(social_type.equals("twitter"))
			{	
				return_jo.put("twitter_successful", social_successful);
				if(!social_successful)
					return_jo.put("twitter_failure_message", social_failure_message);
			}
			else if(social_type.equals("facebook"))
			{
				return_jo.put("facebook_successful", social_successful);
				if(!social_successful)
					return_jo.put("facebook_failure_message", social_failure_message);
			}
		} 
		catch (JSONException e) 
		{
			// do what with these values? If we get here, this function will just return an empty jsonobject...
			social_successful = false;
			social_failure_message = "JSONException (in construction of final return_jo block) " + e.getMessage();
			e.printStackTrace();
		} 
		return return_jo;
	}
}