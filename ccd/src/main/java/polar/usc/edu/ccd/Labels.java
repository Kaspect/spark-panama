/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package polar.usc.edu.ccd;

import java.util.ArrayList;
import polar.usc.edu.ccd.Series;

public class Labels {
    ArrayList<String> labels;
    Series[] series;
    public Labels(ArrayList<String> labels, Series[] series) {
        this.labels = labels;
        this.series = series;
    }
    public ArrayList<String> getLabels() {
        return labels;
    }
    public void setLabels(ArrayList<String> labels) {
        this.labels = labels;
    }
    public Series[] getSeries() {
        return series;
    }
    public void setSeries(Series[] series) {
        this.series = series;
    }
}