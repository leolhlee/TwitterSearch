package search;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;




public class TwitterSample {
	public static final String STREAM_HOST = "https://api.twitter.com";
  
	public static final String consumerKey ="3NvqdhWbZdHoMyvo7049klkZt";
	public static final String consumerSecret ="82b04MU3hC2MyxCLbti5UlHPLTeHBw9GOP7bCBR4SKv1TjnMEl";
	public static final String token ="1179430219653890048-i2z5rjjxOpXjZvSq4czXhpyhKPc3sG";
	public static final String secret ="zf45pY6feR5uGdJCv2fCwTcbbck957pCTQUpIYIFdmhtA";
	public static final int TOPIC_BUMBER = 5;
	public static final int ACCESS_INTERVAL = 15*60*1000; // For avoiding "Rate limit exceeded" error
	
	public static final String PATH = "/search/tweets.json";
	  
	public static void main(String[] args) {
		List<String> topics = new ArrayList<String>();
		ExecutorService  executorService = null;
		
		 while (true) {
			 Scanner scanner = new Scanner(System.in);
			 String inputString = scanner.next();
		        if (inputString.equalsIgnoreCase("exit") && executorService != null) {
		        	executorService.shutdownNow();
		          break;
		        }
		        topics.add(inputString);
		        if (topics.size() == TOPIC_BUMBER) {
		        	executorService = process(topics);
		        }
		 }
	}
		

	private static ExecutorService process(List<String> topics) {
	    ThreadFactory threadFactory = new ThreadFactoryBuilder()
	            .setDaemon(true)
	            .setNameFormat("Twitter-client-io-thread-%d")
	            .build();
	    ExecutorService  executorService = Executors.newFixedThreadPool(TOPIC_BUMBER, threadFactory);
    	topics.forEach( t -> processIndividual(t, executorService));
    	return executorService;
	}
	
	 private static void processIndividual(String topic,  ExecutorService  executorService) {

		 Runnable task = () -> {
			 CloseableHttpClient httpclient = HttpClients.createDefault();
				BaseEndpoint endpoint = new BaseEndpoint(PATH, HttpConstants.HTTP_GET);

				try {
					OAuth1 auth = new OAuth1(consumerKey, consumerSecret, token, secret);
					endpoint.addQueryParameter(Constants.QUERY, topic);
					
					HttpUriRequest request = constructRequest(endpoint, auth);
					
					while (true) {
						HttpResponse response = httpclient.execute(request);
						
						HttpEntity entity = response.getEntity();
						String responseString = EntityUtils.toString(entity, "UTF-8");
						try (PrintStream out = new PrintStream(new FileOutputStream(topic + ".txt"))) {
							out.println("******************************************************");
						    out.print(responseString);
						}	
						Thread.sleep(ACCESS_INTERVAL);
					}

				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
 
		 };
			
		 executorService.submit(task);
	 }
	  
	  public static HttpUriRequest constructRequest(BaseEndpoint endpoint, OAuth1 auth) {
		    String url = STREAM_HOST + endpoint.getURI();
		    if (endpoint.getHttpMethod().equalsIgnoreCase(HttpGet.METHOD_NAME)) {
		      HttpGet get = new HttpGet(url);
		      if (auth != null)
		        auth.signRequest(get, null);
		      return get;
		    } else if (endpoint.getHttpMethod().equalsIgnoreCase(HttpPost.METHOD_NAME) ) {
		      HttpPost post = new HttpPost(url);

		      post.setEntity(new StringEntity(endpoint.getPostParamString(), Constants.DEFAULT_CHARSET));
		      post.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
		      if (auth != null)
		        auth.signRequest(post, endpoint.getPostParamString());

		      return post;
		    } else {
		      throw new IllegalArgumentException("Bad http method: " + endpoint.getHttpMethod());
		    }
		  }
}
