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
	//boolean facebook_successful;
	//String facebook_failure_message;
	boolean simulation;
	
	public FacebookUploaderCallable(Frame inc_frame2upload, User inc_reporter, Station inc_station_object, boolean inc_simulation)
	{
		frame2upload = inc_frame2upload;
		station_object = inc_station_object;
		reporter = inc_reporter;
		//facebook_successful = false;
		//facebook_failure_message = "Init failure";
		simulation = inc_simulation;
	}
	
	/*public boolean getFacebookSuccessful()
	{
		return facebook_successful;
	}
	
	public String getFacebookFailureMessage()
	{
		return facebook_failure_message;
	}*/
	
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
		JSONObject return_jo = new JSONObject();
		try
		{
			if(simulation)
			{
				// send emails to admin on simulation since nothing goes to facebook, ok to turn this off if you want
				se.sendMail("FB triggered for " + reporter.getDesignation(), "url=" + frame2upload.getURLString(), "cyrus7580@gmail.com", "info@huzon.tv");
			}
			
			if(reporter.getFacebookPageAccessToken() == null || reporter.getFacebookPageAccessToken().equals(""))
			{
				return_jo.put("facebook_successful", false);
				return_jo.put("facebook_failure_message", "user_had_no_credentials");
				
				if(!simulation)
				{	
					String emailmessage = getMissingCredentialsEmailMessage();
					// send to reporter and to admin
					se.sendMail("Action required: huzon.tv FB alert was unable to fire. Please link your accounts.", emailmessage, reporter.getEmail(), "info@huzon.tv");
					se.sendMail(reporter.getDesignation() + " was notified of missing FB credentials", "The following email was sent to a reporter due to missing FB credentials:\n\n" + emailmessage, "cyrus7580@gmail.com", "info@huzon.tv");
				}
			}
			else // user appears to have facebook credentials
			{
				if(!simulation) // go ahead with the post
				{	
					System.out.println("FacebookUploaderCallable.call(): entering real post to reporter account");
					URL[] image_urls = frame2upload.get2x2CompositeURLs();
					if(image_urls == null)
					{
						System.out.println("FacebookUploaderCallable.call(): image_urls was null coming back from Frame.get2x2CompositeURLs()");
						return_jo.put("facebook_successful", false);
						return_jo.put("facebook_failure_message", "image_urls was null coming back from Frame.get2x2CompositeURLs()");
					}
					else
					{
						System.out.println("FacebookUploaderCallable.call(): image urls populated. Creating composite.");
						File[] image_files = new File[4];
						String tmpdir = System.getProperty("java.io.tmpdir");
						
						System.out.println("FacebookUploaderCallable.call(): downloading " + image_urls[0] + " and saving to " + tmpdir + "/image_for_facebook0.jpg");
						ReadableByteChannel rbc0 = Channels.newChannel(image_urls[0].openStream());
						FileOutputStream fos0 = new FileOutputStream(tmpdir + "/image_for_facebook0.jpg"); // FIXME this might have conflicts with multiple stations
						fos0.getChannel().transferFrom(rbc0, 0, Long.MAX_VALUE);
						image_files[0] = new File(tmpdir + "/image_for_facebook0.jpg");
						
						System.out.println("FacebookUploaderCallable.call(): downloading " + image_urls[1] + " and saving to " + tmpdir + "/image_for_facebook1.jpg");
						ReadableByteChannel rbc1 = Channels.newChannel(image_urls[1].openStream());
						FileOutputStream fos1 = new FileOutputStream(tmpdir + "/image_for_facebook1.jpg"); // FIXME this might have conflicts with multiple stations
						fos1.getChannel().transferFrom(rbc1, 0, Long.MAX_VALUE);
						image_files[1] = new File(tmpdir + "/image_for_facebook1.jpg");
						
						System.out.println("FacebookUploaderCallable.call(): downloading " + image_urls[2] + " and saving to " + tmpdir + "/image_for_facebook2.jpg");
						ReadableByteChannel rbc2 = Channels.newChannel(image_urls[2].openStream());
						FileOutputStream fos2 = new FileOutputStream(tmpdir + "/image_for_facebook2.jpg"); // FIXME this might have conflicts with multiple stations
						fos2.getChannel().transferFrom(rbc2, 0, Long.MAX_VALUE);
						image_files[2] = new File(tmpdir + "/image_for_facebook2.jpg");
						
						System.out.println("FacebookUploaderCallable.call(): downloading " + image_urls[3] + " and saving to " + tmpdir + "/image_for_facebook3.jpg");						
						ReadableByteChannel rbc3 = Channels.newChannel(image_urls[3].openStream());
						FileOutputStream fos3 = new FileOutputStream(tmpdir + "/image_for_facebook3.jpg"); // FIXME this might have conflicts with multiple stations
						fos3.getChannel().transferFrom(rbc3, 0, Long.MAX_VALUE);
						image_files[3] = new File(tmpdir + "/image_for_facebook3.jpg");
						fos0.close();fos1.close();fos2.close();fos3.close();
						
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
							System.out.println("FacebookUploaderCallable.call(): writing composite image.");
							ImageIO.write(after, "PNG", new File(tmpdir, "image_for_facebook.png"));
							File imagefile = new File(tmpdir + "/image_for_facebook.png");
							
							Platform p = new Platform();
							long redirect_id = p.createAlertInDB(station_object, "facebook", reporter.getDesignation(), frame2upload.getURLString());
							String message = station_object.getMessage("facebook", frame2upload.getTimestampInMillis(), redirect_id, reporter);

							Facebook facebook = new FacebookFactory().getInstance();
							facebook.setOAuthAppId("176524552501035", "dbf442014759e75f2f93f2054ac319a0");
							facebook.setOAuthPermissions("publish_stream,manage_page");
							facebook.setOAuthAccessToken(new AccessToken(reporter.getFacebookPageAccessToken(), null));
							
							String facebookresponse = "";
							try {
								facebookresponse = facebook.postPhoto(new Long(reporter.getFacebookPageID()).toString(), new Media(imagefile), message, "33684860765", false); // FIXME hardcode to wkyt station
								return_jo.put("facebook_successful", true); // if no exception thrown above, we get to this statement and assume post was successful, regardless of two db updates below
								se.sendMail("FB successful for " + reporter.getDesignation(), "url=" + frame2upload.getURLString(), "cyrus7580@gmail.com", "info@huzon.tv");
								// if either of these fail, notify admin from within functions themselves
								boolean alert_text_update_successful = p.updateAlertText(redirect_id, message);
								boolean social_id_update_successful = p.updateSocialItemID(redirect_id, facebookresponse);
							} 
							catch (FacebookException e) 
							{
								return_jo.put("facebook_successful", false);
								return_jo.put("facebook_failure_message", e.getErrorMessage());
								
								if((e.getErrorCode() == 190) || (e.getErrorCode() == 100)) // if one of these errors was generated...
								{
									reporter.resetFacebookCredentialsInDB(); // the credentials are no good anymore. Delete them to allow the user to start over.
									String emailmessage = getMissingCredentialsEmailMessage();
									// send to reporter and to admin
									se.sendMail("Action required: huzon.tv FB alert was unable to fire. Please link your accounts.", emailmessage, reporter.getEmail(), "info@huzon.tv");
									se.sendMail(reporter.getDesignation() + " was notified of invalid FB credentials", "The following email was sent to a reporter due to invalid FB credentials:\n\n" + emailmessage, "cyrus7580@gmail.com", "info@huzon.tv");
								}
								else
								{	
									e.printStackTrace();
									se.sendMail("Failed facebook photo post. Unknown error. (This is not a user-has-not-linked issue.)", reporter.getDesignation() + " " + 
											new Long(reporter.getFacebookPageID()).toString() + " " + message + " " + e.getMessage(),"cyrus7580@gmail.com", "info@huzon.tv");
								}
							}
						} catch (IOException e) {
							se.sendMail("IOException trying to create composite image and post to facebook.", e.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
						}
					}
					
					/*
					 * OLD SINGLE-FRAME METHOD
					 
					URL image_url = new URL(frame2upload.getURLString());
				    ReadableByteChannel rbc = Channels.newChannel(image_url.openStream());
				    String tmpdir = System.getProperty("java.io.tmpdir");
				    System.out.println("TEMP DIR=" + tmpdir);
				    FileOutputStream fos = new FileOutputStream(tmpdir + "/image_for_facebook.jpg"); // FIXME will cause problems with multiple stations
				    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				    File imagefile = new File(tmpdir + "/image_for_facebook.jpg");
				
					Facebook facebook = new FacebookFactory().getInstance();
					facebook.setOAuthAppId("176524552501035", "dbf442014759e75f2f93f2054ac319a0");
					facebook.setOAuthPermissions("publish_stream,manage_page");
					facebook.setOAuthAccessToken(new AccessToken(reporter.getFacebookPageAccessToken(), null));
					
					Platform p = new Platform();
					long redirect_id = p.createAlertInDB(station_object, "facebook", reporter.getDesignation(), frame2upload.getURLString());
					String message = station_object.getMessage("facebook", frame2upload.getTimestampInMillis(), redirect_id, reporter);
													
					String facebookresponse = "";
					try {
						facebookresponse = facebook.postPhoto(new Long(reporter.getFacebookPageID()).toString(), new Media(imagefile), message, "33684860765", false); // FIXME hardcode to wkyt station
						return_jo.put("facebook_successful", true); // if no exception thrown above, we get to this statement and assume post was successful, regardless of two db updates below
						se.sendMail("FB successful for " + reporter.getDesignation(), "url=" + frame2upload.getURLString(), "cyrus7580@gmail.com", "info@huzon.tv");
						// if either of these fail, notify admin from within functions themselves
						boolean alert_text_update_successful = p.updateAlertText(redirect_id, message);
						boolean social_id_update_successful = p.updateSocialItemID(redirect_id, facebookresponse);
					} 
					catch (FacebookException e) 
					{
						return_jo.put("facebook_successful", false);
						return_jo.put("facebook_failure_message", e.getErrorMessage());
						
						if((e.getErrorCode() == 190) || (e.getErrorCode() == 100)) // if one of these errors was generated...
						{
							reporter.resetFacebookCredentialsInDB(); // the credentials are no good anymore. Delete them to allow the user to start over.
							String emailmessage = getMissingCredentialsEmailMessage();
							// send to reporter and to admin
							se.sendMail("Action required: huzon.tv FB alert was unable to fire. Please link your accounts.", emailmessage, reporter.getEmail(), "info@huzon.tv");
							se.sendMail(reporter.getDesignation() + " was notified of invalid FB credentials", "The following email was sent to a reporter due to invalid FB credentials:\n\n" + emailmessage, "cyrus7580@gmail.com", "info@huzon.tv");
						}
						else
						{	
							e.printStackTrace();
							se.sendMail("Failed facebook photo post. Unknown error. (This is not a user-has-not-linked issue.)", reporter.getDesignation() + " " + 
									new Long(reporter.getFacebookPageID()).toString() + " " + message + " " + e.getMessage(),"cyrus7580@gmail.com", "info@huzon.tv");
						}
					}*/
				}
				else
				{
					return_jo.put("facebook_successful", false);
					return_jo.put("facebook_failure_message", "simulation");
				}
			}
		}
		catch(MalformedURLException murle)
		{
			murle.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		return return_jo;
	}
}
