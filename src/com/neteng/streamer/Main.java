package com.neteng.streamer;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {


    static String cmd1 = "python3 frame_extractor.py '/media/hossein/hossein/projects/net_eng/phas3/neteng_streamer/src/com/neteng/streamer/movie.Mjpeg' '/media/hossein/hossein/projects/net_eng/phas3/neteng_streamer/src/com/neteng/streamer/output'";

    static String cmd2 = "python3 temp.py";
    public static void main(String argv[])
    {
        try {
            Runtime run = Runtime.getRuntime();
            Process pr = run.exec(cmd1);
            pr.waitFor();
            BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line = "";
            while ((line = buf.readLine()) != null) {
                System.out.println(line);
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
