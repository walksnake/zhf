package me.limg.zhf;

import me.limg.zhf.loader.ZhiHuLoader;
import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;

import java.io.File;

public class Main {

    public static void main(String[] args) throws Exception {
        String dataDir = System.getProperty("user.dir");
        String[] aids = null;
        String[] qids = null;
        String url = null;

        CommandLineParser parser = new DefaultParser();
        Options options = buildDefaultOptions();
        CommandLine commandLine = parser.parse(options, args);

        if(args == null || args.length <= 0){
            printHelp(options);
            return;
        }

        if (commandLine.hasOption("help")) {
            printHelp(options);
            return;
        }

        if (commandLine.hasOption("data")) {
            dataDir = commandLine.getOptionValue("data");
        } else {
            dataDir += File.separator + "data";
        }

        if (commandLine.hasOption("url")) {
            url = commandLine.getOptionValue("url");
        }

        if (commandLine.hasOption("qids")) {
            qids = commandLine.getOptionValues("qids");
        }

        if (commandLine.hasOption("aids")) {
            aids = commandLine.getOptionValues("aids");
        }

        if (StringUtils.isEmpty(url) && (qids == null || qids.length <= 0)) {
            print("请选择要处理的问题ID列表或者指定URL");
            return;
        }

        if (aids != null && aids.length > 0) {
            if (qids.length > 1) {
                print("处理答案时，问题ID列表只能有一个值");
                return;
            }
        }

        ZhiHuLoader zhiHuLoader = new ZhiHuLoader();
        zhiHuLoader.setRootPath(dataDir);
        if (!StringUtils.isEmpty(url)) {
            zhiHuLoader.loadFromUrl(url);
            return;
        }

        if (aids != null && aids.length > 0) {
            String qid = qids[0];
            for (String aid : aids) {
                zhiHuLoader.loadAnswer(qid, aid);
            }
        } else {
            for (String qid : qids) {
                zhiHuLoader.loadThread(qid);
            }
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("zhf", options);
    }


    private static void print(String message){
        System.out.println(message);
    }

    private static Options buildDefaultOptions() {
        Options options = new Options();
        options.addOption(Option.builder("data").argName( "data" )
                .hasArg()
                .required(false)
                .desc("配置数据存放路径，默认是data" ).build());
        options.addOption(Option.builder("qids").argName("qids")
                .hasArgs()
                .valueSeparator(',')
                .required(false)
                .desc("配置问题ID，多个ID用','隔开").build());
        options.addOption(Option.builder("url").argName("url")
                .hasArg()
                .required(false)
                .desc("配置问题URL").build());
        options.addOption(Option.builder("aids").argName("aids").hasArgs().valueSeparator(',').required(false).desc("配置回答ID， 多个ID用','隔开").build());
        options.addOption(Option.builder("help").argName("help").hasArg(false).required(false).desc("打印帮助信息").build());

        return options;
    }

}
