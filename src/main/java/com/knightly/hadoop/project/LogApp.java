package com.knightly.hadoop.project;

import com.kumkee.userAgent.UserAgent;
import com.kumkee.userAgent.UserAgentParser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

/**
 * Created by knightly on 2017/11/27.
 */
public class LogApp {

    /**
     * Map:读取输入的文件
     */
    public static class MyMapper extends Mapper<LongWritable,Text,Text,LongWritable>{

        LongWritable one = new LongWritable(1);
        UserAgentParser userAgentParser;

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            userAgentParser = new UserAgentParser();
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            userAgentParser = null;
        }

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            //接收到的每一行数据
            String line = value.toString();

            UserAgent agent = userAgentParser.parse(line);
            String browser = agent.getBrowser();

            //通过上下文把map的处理结果输出
            context.write(new Text(browser),one);
        }
    }

    /**
     * Reducer:归并操作
     */
    public static class MyReducer extends Reducer<Text,LongWritable,Text,LongWritable>{

        @Override
        protected void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
            long sum =0;
            for (LongWritable value:values) {
                //求key出现的次数总和
                sum +=value.get();
            }
            //最终统计结果的输出
            context.write(key,new LongWritable(sum));
        }
    }

    /**
     * 定义Driver：封装MapReducer作业的所有信息
     * @param args
     */
    public static void main(String[] args) throws Exception {

        //创建Configuration
        Configuration configuration =new Configuration();

        //准备清理已存在的输出目录
        Path outputPath = new Path(args[1]);
        FileSystem fileSystem = FileSystem.get(configuration);
        if (fileSystem.exists(outputPath)){
            fileSystem.delete(outputPath,true);
            System.out.println("output file exists,but it has been deleted");
        }


        //创建Job
        Job job = Job.getInstance(configuration,"logApp");

        //设置job的处理类
        job.setJarByClass(LogApp.class);

        //设置作业处理的输入路径
        FileInputFormat.setInputPaths(job,new Path(args[0]));

        //设置map相关参数
        job.setMapperClass(MyMapper.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(LongWritable.class);

        //设置reducer相关参数
        job.setReducerClass(MyReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);

        //设置作业的输出路径
        FileOutputFormat.setOutputPath(job,new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);

    }


}
