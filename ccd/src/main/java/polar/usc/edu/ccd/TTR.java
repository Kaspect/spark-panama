package polar.usc.edu.ccd;

import java.util.ArrayList;

/**
 * Created by nilay on 03/29/16.
 */
public class TTR {
    protected static String runTTR(String xhtml){
        ArrayList<Double> tagratioValue = new ArrayList<Double>();
        ArrayList<String> tagratioLineContent = new ArrayList<String>();

        String[] lines = xhtml.split("\\n");
        for (int i=0; i<lines.length; i++)
        {
            // Ignore empty lines. Comments and script tags are already removed by Tika
            if (lines[i].trim().isEmpty())
                continue;

            //System.out.print(lines[i]);
            char[] ca = lines[i].toCharArray();
            boolean tagFlag = false;
            String tempLine = "";
            int x = 0,y = 0;
            for (int j=0;j<ca.length-1;j++)
            {
                //System.out.print(ca[j]);
                if (ca[j] == '<')
                {
                    tagFlag = true;
                    //System.out.println(" -- Found tag beginning");
                }

                if (ca[j] == '>')
                {
                    tagFlag = false;
                    y++; // Increment tag count
                    //System.out.println(" -- Found tag ending");
                    continue;
                }

                // Ignore as long as tag flag is on
                if (tagFlag)
                    continue;

                // count non-tag ASCII characters
                if (ca[j] >= 0 && ca[j] <= 256)
                {
                    tempLine = tempLine+ca[j];
                    x++;
                }

                //System.out.println("x: "+x);
                //System.out.println("y: "+y);
            }
            if (y == 0)
            {
                tagratioValue.add(new Double(x));
                //System.out.println("value "+new Double(x));
            }
            else
            {
                tagratioValue.add((double) x/y);
                //System.out.println("value "+(double) x/y);
            }
            tagratioLineContent.add(tempLine);
        }
        //System.out.println();

        // Smooth the data and calculate SD to be used as a cutoff threshold
        tagratioValue = smootheTTR(tagratioValue);
        double sd = sdTTR(tagratioValue,meanTTR(tagratioValue));
        //System.out.println("SD is "+sd);

        String ttr = "";
        for (int i=0;i<tagratioValue.size();i++)
        {
            //System.out.println("value "+i+" is "+tagratioValue.get(i));
            // If value above threshold, store line number in new array.
            if (tagratioValue.get(i) > sd)
            {
                //System.out.println(tagratioLineContent.get(i));
                ttr = ttr+tagratioLineContent.get(i).trim()+" ";
            }
        }
        return ttr;
    }

    private static double meanTTR(ArrayList<Double> values) {
        double sum = 0;
        for (int i=0;i<values.size();i++)
            sum += values.get(i);
        return sum/values.size();
    }

    private static double sdTTR(ArrayList<Double> values, double mean) {
        double sum = 0;
        for (int i=0;i<values.size();i++)
            sum += (values.get(i) - mean)*(values.get(i) - mean);
        return Math.sqrt(sum/values.size());
    }

    private static ArrayList<Double> smootheTTR(ArrayList<Double> values) {
        int size = values.size();

        // How to do the smoothing? Formula unclear
        double[] smoothedValues = new double[size];
        smoothedValues[0] = values.get(0);
        smoothedValues[1] = values.get(1);
        smoothedValues[size-2] = values.get(size-2);
        smoothedValues[size-1] = values.get(size-1);

        for(int i=2;i<size-2;i++)
        {
            smoothedValues[i] = values.get(i-2)+values.get(i-1)+values.get(i)+values.get(i+1)+values.get(i+2);
            smoothedValues[i] = smoothedValues[i]/5;
        }

        for(int i=0;i<size;i++)
            values.set(i,smoothedValues[i]);

        return values;
    }
}
