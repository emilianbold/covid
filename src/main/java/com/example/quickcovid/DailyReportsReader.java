package com.example.quickcovid;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DateTimeColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Row;
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

        //remove incomplete data on March 11th, 12th and 13th
        allData = allData.dropWhere(allData.dateColumn("Last Update").isBetweenIncluding(LocalDate.of(2020, 3, 11), LocalDate.of(2020, 3, 13)));

        Table duplicateCompensate = allData.copy();
        duplicateCompensate.clear();

        allData.splitOn("Province/State", "Country/Region", "Last Update").asTableList()
                .forEach(t -> {
                    if (t.rowCount() > 1) {
//                        System.out.println("Duplicate data for " + t.getString(0, "Province/State") + " " + t.getString(0, "Country/Region") + " " + t.getString(0, "Last Update") + " ");
                        //Confirmed,Deaths,Recovered,Ongoing
                        int Confirmed = t.intColumn("Confirmed").get(0),
                                Deaths = t.intColumn("Deaths").get(0),
                                Recovered = t.intColumn("Recovered").get(0),
                                Ongoing = t.intColumn("Ongoing").get(0);
                        int totalDuplicates = t.rowCount();
                        t.forEach(row -> {
                            int Confirmed2 = row.getInt("Confirmed"),
                                    Deaths2 = row.getInt("Deaths"),
                                    Recovered2 = row.getInt("Recovered"),
                                    Ongoing2 = row.getInt("Ongoing");
                            if (Confirmed2 != Confirmed || Deaths2 != Deaths || Recovered2 != Recovered || Ongoing2 != Ongoing) {
                                System.out.println("Inconsistent " + Ongoing2 + " vs " + Ongoing);
                            }
                        });

                        //compensate for duplicates by adding negative values
                        Row c = t.appendRow();
                        c.setString("Province/State", t.getString(0, "Province/State"));
                        c.setString("Country/Region", t.getString(0, "Country/Region"));
                        c.setDate("Last Update", t.dateColumn("Last Update").get(0));
                        //Confirmed,Deaths,Recovered,Ongoing
                        c.setInt("Confirmed", -(totalDuplicates - 1) * Confirmed);
                        c.setInt("Deaths", -(totalDuplicates - 1) * Deaths);
                        c.setInt("Recovered", -(totalDuplicates - 1) * Recovered);
                        c.setInt("Ongoing", -(totalDuplicates - 1) * Ongoing);
                    }
                });

        allData = allData.append(duplicateCompensate);

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
