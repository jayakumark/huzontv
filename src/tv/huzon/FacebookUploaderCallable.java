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

public class FacebookUploaderCallable implements Callable<JSONObject> {

	Frame frame2upload;
	User reporter;
	Station station_object;
	
	public FacebookUploaderCallable(Frame inc_frame2upload, User inc_reporter, Station inc_station_object)
	{
		frame2upload = inc_frame2upload;
		station_object = inc_station_object;
		reporter = inc_reporter;
	}
	public String getMissingCredentialsEmailMessage()
	{
		String message = "";
		message =
				reporter.getDisplayName() +
					",\n\nAn alert triggered for you with huzon.tv. However, our system was unable to actually fire the alert because your FB account " + 
					"has become disconnected from huzon.tv. This can happen for several reasons:" +
					"\n\n- huzon.tv access to your account has expired (60 days)" + 
					"\n- You disabled the huzon.tv app in your FB privacy configuration" +
					"\n- Your FB account was never linked to huzon.tv in the first place"+
					"\n\nPlease go to https://www.huzon.tv/registration.html to link your FB account to huzon.tv and enable automated alerts. " +
					"Thanks!\n\nhuzon.tv staff\n\nPS: Here's the image that would have posted: " + frame2upload.getURLString();	
			return message;
	}	
	
	
	public JSONObject call() {
		SimpleEmailer se = new SimpleEmailer();
		boolean facebook_successful = false;
		String facebook_failure_message = "Message not set.";
		try
		{
			(new Platform()).addMessageToLog("FB triggered for " + reporter.getDesignation() + " mode=" + station_object.getAlertMode());
			
			if(station_object.getAlertMode().equals("silent"))
			{
				facebook_successful = false;
				facebook_failure_message = "Station alert_mode is set to silent";
				//return_jo.put("facebook_successful", false);
				//return_jo.put("facebook_failure_message", "silent");
				(new Platform()).addMessageToLog("FB triggered but suppressed for " + reporter.getDesignation() + ". mode=silent");
			}
			else if(station_object.getAlertMode().equals("test") || station_object.getAlertMode().equals("live"))
			{
				User postinguser = null;
				if(station_object.getAlertMode().equals("test"))
					postinguser = new User("huzon_master", "designation");
				else
					postinguser = reporter;
				
				if(postinguser.getFacebookPageAccessToken() == null || postinguser.getFacebookPageAccessToken().equals(""))
				{
					facebook_successful = false;
					facebook_failure_message = "user " + postinguser.getDesignation() + " has no facebook credentials";
					//return_jo.put("facebook_successful", false);
					//return_jo.put("facebook_failure_message", "user " + postinguser.getDesignation() + " has no facebook credentials");
					(new Platform()).addMessageToLog("FB triggered for " + reporter.getDesignation() + " but failed due to lack of tw credentials. user=" + postinguser.getDesignation() + ". mode=" + station_object.getAlertMode());
					if(station_object.getAlertMode().equals("live"))
					{
						String emailmessage = getMissingCredentialsEmailMessage();
						se.sendMail("Action required: huzon.tv FB alert was unable to fire. Please link your accounts.", emailmessage, reporter.getEmail(), "info@huzon.tv");
						(new Platform()).addMessageToLog(reporter.getDesignation() + " was notified of missing FB credentials.");
					}
				}
				else // user appears to have facebook credentials
				{
					boolean faces_only = true;
					URL[] image_urls = frame2upload.get2x2CompositeURLs(faces_only, reporter.getDesignation());
					if(image_urls == null)
					{
						facebook_successful = false;
						facebook_failure_message = "image_urls was null coming back from Frame.get2x2CompositeURLs()";
						//return_jo.put("facebook_successful", false);
						//return_jo.put("facebook_failure_message", "image_urls was null coming back from Frame.get2x2CompositeURLs()");
						(new Platform()).addMessageToLog("FB triggered for " + reporter.getDesignation() + " + creds, but get2x2 failed.  user=" + postinguser.getDesignation() + ". mode=" + station_object.getAlertMode());
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
						
						System.out.println("FacebookUploaderCallable.call(): downloading " + image_urls[0] + " and saving to " + image_filenames[0]);
						ReadableByteChannel rbc0 = Channels.newChannel(image_urls[0].openStream());
						FileOutputStream fos0 = new FileOutputStream(image_filenames[0]); // FIXME this might have conflicts with multiple stations
						fos0.getChannel().transferFrom(rbc0, 0, Long.MAX_VALUE);
						image_files[0] = new File(image_filenames[0]);
						fos0.close();
						
						System.out.println("FacebookUploaderCallable.call(): downloading " + image_urls[1] + " and saving to " + image_filenames[1]);
						ReadableByteChannel rbc1 = Channels.newChannel(image_urls[1].openStream());
						FileOutputStream fos1 = new FileOutputStream(image_filenames[1]); // FIXME this might have conflicts with multiple stations
						fos1.getChannel().transferFrom(rbc1, 0, Long.MAX_VALUE);
						image_files[1] = new File(image_filenames[1]);
						fos1.close();
						
						System.out.println("FacebookUploaderCallable.call(): downloading " + image_urls[2] + " and saving to " + image_filenames[2]);
						ReadableByteChannel rbc2 = Channels.newChannel(image_urls[2].openStream());
						FileOutputStream fos2 = new FileOutputStream(image_filenames[2]); // FIXME this might have conflicts with multiple stations
						fos2.getChannel().transferFrom(rbc2, 0, Long.MAX_VALUE);
						image_files[2] = new File(image_filenames[2]);
						fos2.close();
						
						System.out.println("FacebookUploaderCallable.call(): downloading " + image_urls[3] + " and saving to " + image_filenames[3]);				
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
							
							//Graphics g = after.getGraphics();

							// Save as new image
							String composite_filename = tmpdir + "/" + UUID.randomUUID() + ".png";
							System.out.println("FacebookUploaderCallable.call(): writing composite image.");
							ImageIO.write(after, "PNG", new File(composite_filename));
							File composite_file = new File(composite_filename);
							
							image_files[0].delete();
							image_files[1].delete();
							image_files[2].delete();
							image_files[3].delete();
							
							//if(!station_object.isLocked("facebook"))
							//{
								String uuid = UUID.randomUUID().toString();
								boolean successfullylocked = station_object.lock(uuid, "facebook");
								
								if(successfullylocked)
								{	
							
									Platform p = new Platform();
									long redirect_id = p.createAlertInDB(station_object, "facebook", reporter.getDesignation(), frame2upload.getURLString(), postinguser);
									String message = station_object.getMessage("facebook", frame2upload.getTimestampInMillis(), redirect_id, reporter);

									Facebook facebook = new FacebookFactory().getInstance();
									facebook.setOAuthAppId("176524552501035", "dbf442014759e75f2f93f2054ac319a0");
									facebook.setOAuthPermissions("publish_stream,manage_page");
									facebook.setOAuthAccessToken(new AccessToken(postinguser.getFacebookPageAccessToken(), null)); // if "test" get huzontv access token, if "live" get reporter's access token
									
									String facebookresponse = "";
									try {
										facebookresponse = facebook.postPhoto(new Long(postinguser.getFacebookPageID()).toString(), new Media(composite_file), message, "33684860765", false); // FIXME hardcode to wkyt station
										
										facebook_successful = true;
										//return_jo.put("facebook_successful", true); // if no exception thrown above, we get to this statement and assume post was successful, regardless of two db updates below
										(new Platform()).addMessageToLog("FB successful for " + reporter.getDesignation() + ". Actual FB response=" + facebookresponse + " user=" + postinguser.getDesignation() + ". mode=" + station_object.getAlertMode());
										// if either of these fail, notify admin from within functions themselves
										boolean alert_text_update_successful = p.updateAlertText(redirect_id, message);
										boolean social_id_update_successful = p.updateSocialItemID(redirect_id, facebookresponse);
										se.sendMail("FB successful for " + reporter.getDesignation(), "Actual FB response=" + facebookresponse + " user=" + postinguser.getDesignation() + ". mode=" + station_object.getAlertMode() + "\n\nhttps://www.huzon.tv/alert_monitor.html", "cyrus7580@gmail.com", "info@huzon.tv");
									} 
									catch (FacebookException e) 
									{
										facebook_successful = false;
										facebook_failure_message = "facebook produced an error: " + e.getErrorMessage();
										//return_jo.put("facebook_successful", false);
										//return_jo.put("facebook_failure_message", e.getErrorMessage());
										
										if((e.getErrorCode() == 190) || (e.getErrorCode() == 100)) // if one of these errors was generated...
										{
											if(station_object.getAlertMode().equals("live"))
											{	
												reporter.resetFacebookCredentialsInDB(); // the credentials are no good anymore. Delete them to allow the user to start over.
												String emailmessage = getMissingCredentialsEmailMessage();
												se.sendMail("Action required: huzon.tv FB alert was unable to fire. Please link your accounts.", emailmessage, reporter.getEmail(), "info@huzon.tv");
												(new Platform()).addMessageToLog(reporter.getDesignation() + " was notified of invalid FB credentials. Actual FB response=" + facebookresponse + " user=" + postinguser.getDesignation() + ". mode=" + station_object.getAlertMode());
											}
											else
												(new Platform()).addMessageToLog("test account FB creds invalid trying to fire for " + reporter.getDesignation() + ". Actual FB response=" + facebookresponse + "\nuser=" + postinguser.getDesignation() + ". mode=" + station_object.getAlertMode());
											
										}
										else
										{	
											e.printStackTrace();
											(new Platform()).addMessageToLog("Failed facebook photo post. Unknown error. (This is not a user-has-not-linked issue.) Actual FB response=" + facebookresponse + " " + reporter.getDesignation() + " " + 
													new Long(reporter.getFacebookPageID()).toString() + " " + message + " " + e.getMessage() + "\nuser=" + postinguser.getDesignation() + ". mode=" + station_object.getAlertMode());
										}
									}
									station_object.unlock(uuid, "facebook");			
								}
								else
								{
									(new Platform()).addMessageToLog("Skipped FB post for " + reporter.getDesignation() + ". Tried to set lock but station.lock() returned false.  user=" + postinguser.getDesignation() + ". mode=" + station_object.getAlertMode());
								}
							//}
							//else
							//{
							//	(new Platform()).addMessageToLog("Skipped FB post due to lock.\nreporter=" + reporter.getDesignation() + " user=" + postinguser.getDesignation() + ". mode=" + station_object.getAlertMode());
							//}
							composite_file.delete();
							
						} catch (IOException e) {
							(new Platform()).addMessageToLog("IOException trying to create composite image and post to facebook." + "\nuser=" + postinguser.getDesignation() + ". mode=" + station_object.getAlertMode());
						}
					}
				}
			}
		}
		catch(MalformedURLException murle)
		{
			facebook_successful = false;
			facebook_failure_message = "MalformedURLException " + murle.getMessage();
			murle.printStackTrace();
		}
		catch (FileNotFoundException e) 
		{
			facebook_successful = false;
			facebook_failure_message = "FileNotFoundException " + e.getMessage();
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			facebook_successful = false;
			facebook_failure_message = "IOException " + e.getMessage();
			e.printStackTrace();
		} 
		catch (MessagingException e) 
		{
			facebook_successful = false;
			facebook_failure_message = "MessagingException " + e.getMessage();
			e.printStackTrace();
		}
		
		JSONObject return_jo = new JSONObject();
		try
		{
			return_jo.put("facebook_successful", facebook_successful);
			if(!facebook_successful)
				return_jo.put("facebook_failure_message", facebook_failure_message);
		} 
		catch (JSONException e) 
		{
			facebook_successful = false;
			facebook_failure_message = "JSONException (in construction of final return_jo block) " + e.getMessage();
			e.printStackTrace();
		} 
		return return_jo;
	}
}
