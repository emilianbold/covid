package com.example.quickcovid;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DateTimeColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.Source;
import tech.tablesaw.io.csv.CsvReader;

public class DailyReportsReader {

    public static Table allData() {
        Table allData = rawData();
        StringColumn continent = allData.stringColumn("Country/Region")
                .map(Country::continentOf, n -> StringColumn.create("Continent"));

        allData = allData.addColumns(continent);

        IntColumn confirmed = allData.intColumn("Confirmed");
        IntColumn deaths = allData.intColumn("Deaths");
        IntColumn recovered = allData.intColumn("Recovered");
        IntColumn ongoing = IntColumn.create("Ongoing", confirmed.size());
        for (int i = 0; i < confirmed.size(); i++) {
            int d = deaths.isMissing(i) ? 0 : deaths.get(i);
            int r = recovered.isMissing(i) ? 0 : recovered.get(i);
            ongoing.set(i, confirmed.get(i) - d - r);
//                System.out.println(i + " -> " + ongoing.get(i));
        }
        allData.addColumns(ongoing);

        return allData;
    }

    public static Table rawData() {
        return Stream.of(new File("COVID-19/csse_covid_19_data/csse_covid_19_daily_reports").listFiles((File pathname) -> pathname.getName().endsWith(".csv")))
                .map(f -> {
                    try {
                        Table t = new CsvReader().read(new Source(f));
                        if (t.column("Last Update").type() != ColumnType.LOCAL_DATE_TIME) {
                            Logger.getLogger(MainPage.class.getName()).log(Level.WARNING, "Bad timestamp for " + f);
                            return null;
                        } else {
                            //remove time from date time
                            DateTimeColumn update = t.dateTimeColumn("Last Update");
                            DateColumn date = update.map(d -> d.toLocalDate(), DateColumn::create);
                            t.replaceColumn("Last Update", date);

                            return t;
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(MainPage.class.getName()).log(Level.SEVERE, null, ex);
                        return null;
                    }
                })
                .filter(t -> t != null)
                .reduce((t, u) -> t.append(u))
                .get();
    }
}
