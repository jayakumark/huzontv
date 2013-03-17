package hoozon_remote;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SimmatInterpreter {

	/**
	 * @param args
	 */
	List<String> readSmallTextFile(String aFileName) throws IOException {
	    Path path = Paths.get(aFileName);
	    return Files.readAllLines(path, StandardCharsets.UTF_8);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		SimmatInterpreter si = new SimmatInterpreter();
		
		if(args.length != 3)
		{
			System.out.println("Usage: java -classpath jarfile SimmatInterpreter <simmat_filename> <station> <timestamp_in_milliseconds>");
		}
		else
		{
			if(!args[0].startsWith("/"))
			{
				System.out.println("Simmat filename must have an absolute path on this filesystem.");
			}
			else if(!(args[0].endsWith(".csv")))
			{
				System.out.println("Simmat filename must end with .csv");
			}
			else
			{	
				String[] files_array = null;
				String[] scores_array = null;
				try {
					List<String> simmat_file_as_list_of_lines = si.readSmallTextFile(args[0]);
					StringTokenizer filenames_st = new StringTokenizer(simmat_file_as_list_of_lines.get(0), ",");
					StringTokenizer scores_st = new StringTokenizer(simmat_file_as_list_of_lines.get(1), ",");
					if(!filenames_st.hasMoreTokens())
					{
						System.out.println("Error... the first line of the simmat had no tokens");
					}
					else if(filenames_st.countTokens() != scores_st.countTokens())
					{
						System.out.println("Error... the first and second lines of simmat did not have the same number of tokens");
					}
					else
					{
						JSONObject jo = new JSONObject();
						try {
							jo.put("station", args[1]);
							jo.put("timestamp", args[2]);
							
							files_array = new String[filenames_st.countTokens() - 1];
							filenames_st.nextToken();
							
							int x = 0;
							while(filenames_st.hasMoreTokens())
							{
								files_array[x] = filenames_st.nextToken();
								x++;
							}
							
							// currently this only looks at the first scores line of the simmat
							// thus, only looking at one face per picture
							// in the future, it should read all score lines of the simmat
							// and return scores for all faces in a single image 
							x = 0;
							scores_array = new String[files_array.length];
							scores_st.nextToken();
							double currentscore = 0;
							String currentdesignation = "";
							JSONArray current_ja = new JSONArray();
							JSONObject temp_reporters_jo = new JSONObject();
							while(scores_st.hasMoreTokens())
							{
								scores_array[x] = scores_st.nextToken();
								currentscore = (new Double(scores_array[x])).doubleValue();
								currentdesignation = files_array[x].substring(files_array[x].lastIndexOf("/")+1, files_array[x].lastIndexOf("_"));
								if(temp_reporters_jo.has(currentdesignation))
								{
									(temp_reporters_jo.getJSONArray(currentdesignation)).put(currentscore);
								}
								else
								{
									current_ja = new JSONArray();
									current_ja.put(currentscore);
									temp_reporters_jo.put(currentdesignation, current_ja);
								}
								x++;
							}
							
							Iterator<String> keysit = temp_reporters_jo.keys();
							JSONArray reporters_ja = new JSONArray();
							JSONObject current_jo = null;
							String currentkey = "";
							while(keysit.hasNext())
							{
								currentkey = keysit.next();
								current_jo = new JSONObject();
								current_jo.put("designation", currentkey);
								current_jo.put("scores", temp_reporters_jo.getJSONArray(currentkey));
								reporters_ja.put(current_jo);
							}
							jo.put("reporter_scores",reporters_ja);
							
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						//System.out.println(jo);
						
						
						HttpClient client = new DefaultHttpClient();
						HttpPost post = new HttpPost("http://localhost:8080/hoozontv/endpoint");
						try {
							List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
							nameValuePairs.add(new BasicNameValuePair("method", "commitFrameData"));
							nameValuePairs.add(new BasicNameValuePair("jsonpostbody", jo.toString()));
							post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
						 
							HttpResponse response = client.execute(post);
							BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
							String line = "";
							while ((line = rd.readLine()) != null) {
								//System.out.println(line);
							}
							
						} catch (IOException e) {
							e.printStackTrace();
						}
												
						/*Iterator<String> it = jo.keys();
						String max_key = "";
						double max_average = 0.0;
						String second_to_max_key = "";
						double second_to_max_average = 0.0;
						String third_to_max_key = "";
						double third_to_max_average = 0.0;
						JSONArray ja = new JSONArray();
						String currentkey = "";
						double total = 0;
						double average = 0;
						int num_perfect = 0;
						while(it.hasNext())
						{
							currentkey = it.next();
							try
							{
								ja = jo.getJSONArray(currentkey);
								total = 0;
								for(int a = 0; a < ja.length(); a++)
								{
									total = total + ja.getDouble(a);
									if(ja.getDouble(a) == 1.0)
									{
										num_perfect++;
									}
								}
								average = total / ja.length();
								
								if(average > max_average)
								{
									third_to_max_average = second_to_max_average;
									third_to_max_key = second_to_max_key;
									second_to_max_average = max_average;
									second_to_max_key = max_key;
									max_average = average;
									max_key = currentkey;
								}
								else if(average > second_to_max_average && average < max_average)
								{
									third_to_max_average = second_to_max_average;
									third_to_max_key = second_to_max_key;
									second_to_max_average = average;
									second_to_max_key = currentkey;
								}
								else if(average > third_to_max_average && average < second_to_max_average)
								{
									third_to_max_average = average;
									third_to_max_key = currentkey;
								}
							}
							catch (JSONException e) {
								System.out.println("key=" + currentkey + " was not a JSONArray");
							}
						}
						//System.out.println("------>>>>>>>>> " + max_key + " <<<<<<<<<---------------- (score = " + max_average + ")");
						//System.out.println("------>>>>>>>>> " + second_to_max_key + " <<<<<<<<<---------------- (score = " + second_to_max_average + ")");
						//System.out.println("------>>>>>>>>> " + third_to_max_key + " <<<<<<<<<---------------- (score = " + third_to_max_average + ")");
						
						if(max_average > .90 && (max_average - second_to_max_average) > .2)
						{
							System.out.println("This image passed the .90 absolute threshold and .2 delta. Storing in directory...");
							System.out.println("/usr/bin/cp /hoozon/finished/" + args[2] + ".png /hoozon/provisionally_known/" + max_key + "_" + (new Integer((int)(max_average*100))) + "_" + (new Integer((int)((max_average - second_to_max_average)*100))) + "_" +(new Integer((int)(((new Double(num_perfect))/total)*100))) +".png");
							Runtime runtime = Runtime.getRuntime();
							try{
												
								Process p = runtime.exec(
										new String[] {  "/bin/sh", "-c",
												"/usr/bin/cp /hoozon/finished/" + args[2] + ".png /hoozon/provisionally_known/" + max_key + "_" + (new Integer((int)(max_average*100))) + "_" + (new Integer((int)((max_average - second_to_max_average)*100))) + "_" +(new Integer((int)(((new Double(num_perfect))/total)*100))) +".png" 
										}); 
								try{ p.waitFor(); } catch (InterruptedException ie) { System.out.println("interrupted exception.");}
							}
							catch(IOException ioe){ System.out.println("ioexception."); }
							System.out.println("done.");
						}	*/
					
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}

}
