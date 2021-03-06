package codesquare.badges.sharedpasses;

import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import codesquare.Toolbox;

/**
 * Class to find each Employee's LOC based on commit files
 * 
 * returned file format is {empId} #{LOC} where "{" and "}" are not expressed
 * ie. 4825 #29
 * 
 * accepts a directory - searches for all files recursively
 * 
 * @author deanna.surma
 * 
 */
public class LOC {
	// receives commits
	// returns empId #LOC

	public LOC(String input, String output, Configuration config,FileSystem hdfs) throws Exception {
		
		Job job = new Job(config);

		job.setJarByClass(codesquare.badges.sharedpasses.LOC.class);
		job.setJobName("LOC");
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		job.setMapperClass(Map.class);
		job.setReducerClass(Reduce.class);
		job.setNumReduceTasks(1);
		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		Toolbox.addDirectory(job, hdfs, new Path(input));
		FileOutputFormat.setOutputPath(job, new Path(output));
		try{
			job.waitForCompletion(true);
		}catch(IOException e){
			System.out.println("No Input Paths to run this MapReduce on!");
		}
	}
	
	public LOC(String input1,String input2,String input3,String input4,String input5,String input6,String input7, String output, Configuration config,FileSystem hdfs) throws Exception {
		
		Job job = new Job(config);

		job.setJarByClass(codesquare.badges.sharedpasses.LOC.class);
		job.setJobName("LOC");
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		job.setMapperClass(Map.class);
		job.setReducerClass(Reduce.class);
		job.setNumReduceTasks(1);
		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		Toolbox.addDirectory(job, hdfs, new Path(input1));
		Toolbox.addDirectory(job, hdfs, new Path(input2));
		Toolbox.addDirectory(job, hdfs, new Path(input3));
		Toolbox.addDirectory(job, hdfs, new Path(input4));
		Toolbox.addDirectory(job, hdfs, new Path(input5));
		Toolbox.addDirectory(job, hdfs, new Path(input6));
		Toolbox.addDirectory(job, hdfs, new Path(input7));
		FileOutputFormat.setOutputPath(job, new Path(output));
		job.waitForCompletion(true);
	}
	
	

	/**
	 * 
	 * @write key: empId value: LOC (insertions - deletions)
	 */
	public static class Map extends
			Mapper<LongWritable, Text, Text, IntWritable> {
		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			String line = value.toString();
			String[] components = line.split("\\s+", 9);
			IntWritable one = new IntWritable(Integer.parseInt(components[6])
					- Integer.parseInt(components[7]));
			context.write((new Text(components[1])), one);
			System.out.println("LOCMAP KEY: "+(new Text(components[1])));
			System.out.println("LOCMAP VAL: "+one);
		}
	}

	/**
	 * 
	 * @write key: empId value: total LOC
	 */
	public static class Reduce extends Reducer<Text, IntWritable, Text, Text> {
		public void reduce(Text key, Iterable<IntWritable> values,
				Context context) throws IOException, InterruptedException {
			int sum = 0;
			for (IntWritable val : values) {
				sum += val.get();
			}
			context.write(key, new Text("#" + sum));
			context.setStatus("LOCRED KEY: "+key);
			context.setStatus("LOCRED VAL: "+(new Text("#" + sum)));
		}
	}
}