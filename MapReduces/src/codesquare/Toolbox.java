package codesquare;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Random;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

/**
 * 
 * Class providing commonly used functions
 */
public class Toolbox {

	public static final double reduceTaskConstant= (1.75) * 6;


	/**
	 * 
	 * @return the Jive CodeSquare HDFS file system
	 */
	public static FileSystem getHDFS(Configuration config) {

		// Connect to and Return HDFS
		try {
			FileSystem dfs = FileSystem.get(config);
			return dfs;
		} catch (IOException e) {
			System.err.println(e.getMessage());
			return null;
		}

	}
	
	/**
	 * 
	 * Retrieves a configuration object pertaining to the cluster our HDFS is located in 
	 */
	public static Configuration getConfiguration() {
		Configuration config = new Configuration();
		Path file = new Path("/etc/hadoop-0.20/conf.empty/hdfs-site.xml");
		config.addResource(file);
		file = new Path("/etc/hadoop-0.20/conf.empty/core-site.xml");
		config.addResource(file);
		return config;
	}
	
	
	/**
	 * 
	 * Retrieves a configuration object pertaining to the cluster our HBase is located in 
	 */
	public static Configuration getHBaseConfiguration() {
        Configuration config = HBaseConfiguration.create();
        //config.clear();
        //Path file = new Path("hbase_conf.xml");
        //config.addResource(file);
        InputStream in = null;
		try {
			in = new FileInputStream(new File("/etc/hbase/conf/hbase-site.xml"));
			config.addResource(in);
		} catch (FileNotFoundException e) {
			System.out.println("Could not find: HBase config file");
		}
        return config;
    }

	/**
	 * 
	 * adds all non-hidden files to InputPath (excludes "_SUCCESS" file)
	 * 
	 */
	public static void addDirectory(Job job, FileSystem hdfs, Path directory)
			throws Exception {

		if (hdfs.exists(directory) && !directory.toString().contains("_logs")) {

			if (hdfs.isFile(directory)) {
				if (directory.toString().contains("_SUCCESS")) {
					//do nothing 
				}
				else {
					FileInputFormat.addInputPath(job, directory);
				}
			} else {

				FileStatus[] fs = hdfs.listStatus(directory);
				
				for (FileStatus entry : fs) {
					addDirectory(job, hdfs, entry.getPath());
				}

			}
		}

		// directory.get

		/*
		 * File[] entries = directory.listFiles(); for (File entry : entries) {
		 * if (entry.isFile() && (!entry.isHidden()) &&
		 * (!entry.toString().contains("_SUCCESS"))) {
		 * FileInputFormat.addInputPath(job, new Path(entry.toString())); } if
		 * (entry.isDirectory()) { addDirectory(job, hdfs,entry); } }
		 */
	}

	/**
	 * 
	 * deletes directory and its contents
	 * 
	 * @throws IOException
	 * 
	 */
	public static boolean deleteDirectory(Path path, FileSystem hdfs)
			throws IOException {
		return hdfs.delete(path, true);
		/*
		 * if (path.exists()) { File[] files = path.listFiles(); for (int i = 0;
		 * i < files.length; i++) { if (files[i].isDirectory()) {
		 * deleteDirectory(files[i]); } else { files[i].delete(); } } }
		 */
	}

	/**
	 * 
	 * generates a random string of length 36 (intended to be used for file
	 * names)
	 * 
	 */
	public static String generateString() {
		Random r = new Random();
		return "/user/interns/Commits/tmpOutput/"
				+ Long.toString(Math.abs(r.nextLong()), 36);
	}

	/**
	 * 
	 * adds all non-hidden files to InputPath (excludes "_SUCCESS" file)
	 * 
	 */
	@SuppressWarnings("deprecation")
	public static int subtractTime(String[] x, String[] y) {
		Time time1 = new Time(0, new Integer(x[0]), new Integer(x[1]));
		Time time2 = new Time(0, new Integer(y[0]), new Integer(y[1]));
		System.out.println((int) (time1.getTime() - time2.getTime()));
		return Math.abs((int) (time1.getTime() - time2.getTime()));
	}

	/***
	 * This method retrieves acquired badges checks against the new badges, and
	 * updates the table
	 * 
	 * @param email
	 *            Row Identifier
	 * @param badge
	 *            New badge to add
	 */
	@SuppressWarnings("unchecked")
	public static void addBadges(String email, String badge, HTable table) {
		Object[] badgeList = getBadges(table, email);
		ArrayList<String> aquiredBadges = null;
		String newBadges = null;
		try {
			aquiredBadges = (ArrayList<String>) badgeList[0];
			newBadges = (String) badgeList[1];
		} catch (java.lang.NullPointerException e) {
			// USER IS HAS NOT INSTALLED APP
			return;
		}
		if (!aquiredBadges.contains(badge)) {
			if(!newBadges.contains(badge){
				newBadges = newBadges + " " + badge;
				newBadges.trim();
			}
			updateBadges(table, email, badge, newBadges);
			System.out.println("Finished added badges to HBase, now going to post to notification servlet");
			//Post to notification servlet
			// Create a new HttpClient and Post Header
		    HttpClient httpclient = new DefaultHttpClient();
			/***
			 *
			 *		Compelete the URL below to point to the ActivityStreamSevlet
			 *
			 */
		    HttpPost httppost = new HttpPost("http://10.45.111.143:9090/CodeSquare/ActivityStreamServlet");
		    try {
		        // Add your data
		        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		        nameValuePairs.add(new BasicNameValuePair("email", email));
		        nameValuePairs.add(new BasicNameValuePair("newBadges", badge));
		        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

		        // Execute HTTP Post Request
		        HttpResponse response = httpclient.execute(httppost);
		    } catch (ClientProtocolException e) {
		        e.printStackTrace();
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
	        
		}
	}

	/***
	 * Returns the acquired and new badges
	 * 
	 * @param table
	 *            HTable to modify
	 * @param email
	 *            Row Identifier
	 * @return Object[0] is an ArrayList of acquired badges, Object[1] is newly
	 *         acquired badges
	 */
	public static Object[] getBadges(HTable table, String email) {
		Get get = new Get(Bytes.toBytes(email));
		Result data = null;
		Object[] output = new Object[2];
		ArrayList<String> resultingBadges = new ArrayList<String>();
		String newBadges = "";
		output[0] = resultingBadges;
		output[1] = newBadges;
		try {
			data = table.get(get);
		} catch (Exception e) {
			System.err.println();
		}

		if (data == null) {
			System.out.println("Not found");
			return null;
		}
		if (data.isEmpty()) {
			return null;
		}

		byte[] badges = Bytes.toBytes("Badge");

		NavigableSet<byte[]> badges_awarded = data.getFamilyMap(badges)
				.descendingKeySet();
		for (byte[] badge : badges_awarded) {
			resultingBadges.add(new String(badge));
		}
		newBadges = new String(data.getValue(Bytes.toBytes("Info"),
				Bytes.toBytes("newBadges")));

		output[0] = resultingBadges;
		output[1] = newBadges;
		return output;
	}

	/***
	 * Adds Badges to the specified user
	 * 
	 * @param table
	 *            HTable to modify
	 * @param email
	 *            Row Identifier
	 * @param badges
	 *            Badges to Add
	 */
	public static void updateBadges(HTable table, String email, String badge,
			String newBadges) {

		Put row = new Put(Bytes.toBytes(email));

		row.add(Bytes.toBytes("Badge"), Bytes.toBytes(badge),
				Bytes.toBytes((int) (System.currentTimeMillis() / 1000L)));
		row.add(Bytes.toBytes("Info"), Bytes.toBytes("newBadges"),
				Bytes.toBytes(newBadges));

		try {
			table.put(row);
		} catch (Exception e) {
			System.err.println();
		}
	}

        
   public static HashMap<String, String> getBosses() throws Exception{
		Configuration conf = getHBaseConfiguration();
		HashMap<String, String> bossEmails = new HashMap<String, String>();
		HTable table = getTable(conf);
		ResultScanner s = table.getScanner(Bytes.toBytes("Info"), Bytes.toBytes("bossEmail"));
		Iterator<Result> i = s.iterator();
		while(i.hasNext()){
			Result r = i.next();
			bossEmails.put(new String(r.getRow()), new String(r.value()));
			System.out.println(new String(r.getRow())+", "+new String(r.value()));
		}
		HConnectionManager.deleteConnection(conf, true);
		table.close();
                return bossEmails;
	}
	
	public static HTable getTable(Configuration config) throws Exception {

		// Create a table
		HBaseAdmin admin = new HBaseAdmin(config);
		if (!admin.tableExists("EmpBadges")) {
			admin.createTable(new HTableDescriptor("EmpBadges"));
			admin.disableTable("EmpBadges");
			admin.addColumn("EmpBadges", new HColumnDescriptor("Info"));
			admin.addColumn("EmpBadges", new HColumnDescriptor("Badge"));
			admin.enableTable("EmpBadges");
		}
		HTable table = new HTable(config, "EmpBadges");
		return table;
	}
        
public static void getBossFile(FSDataOutputStream fs, HashMap<String, String> hMap) throws IOException {
	for (Entry<String, String> entry : hMap.entrySet()) {
	    String key = entry.getKey();
	    String value = entry.getValue();
	    fs.write((key+" "+value+"\n").getBytes());
	}
}
}