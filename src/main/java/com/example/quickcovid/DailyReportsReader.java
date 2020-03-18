package com.example.quickcovid;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.javalite.common.Collections;
import tech.tablesaw.aggregate.AggregateFunctions;
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
        return allData(false);
    }

    public static Table allData(boolean fillMissingData) {
        Table allData = rawData();

        allData.forEach(row -> {
            Map<String, String> renames = new HashMap<>();
            renames.put("Iran (Islamic Republic of)", "Iran");
            renames.put("Republic of Korea", "South Korea");
            renames.put("Korea, South", "South Korea");
            renames.put("Taiwan*", "Taiwan");
            renames.put("Hong Kong SAR", "Hong Kong");
            renames.put("UK", "United Kingdom");
            renames.put("Czechia", "Czech Republic");

            String country = row.getString("Country/Region");
            if (renames.containsKey(country)) {
                row.setString("Country/Region", renames.get(country));
            }
        });

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
                        int[] sum = new int[4];
                        int[] max = new int[]{Confirmed, Deaths, Recovered, Ongoing};

                        t.forEach(row -> {
                            int Confirmed2 = row.getInt("Confirmed"),
                                    Deaths2 = row.getInt("Deaths"),
                                    Recovered2 = row.getInt("Recovered"),
                                    Ongoing2 = row.getInt("Ongoing");

                            sum[0] += Confirmed2;
                            sum[1] += Deaths2;
                            sum[2] += Recovered2;
                            sum[3] += Ongoing2;
                            max[0] = Math.max(max[0], Confirmed2);
                            max[1] = Math.max(max[1], Deaths2);
                            max[2] = Math.max(max[2], Recovered2);
                            max[3] = Math.max(max[3], Ongoing2);

                            if (Confirmed2 != Confirmed || Deaths2 != Deaths || Recovered2 != Recovered || Ongoing2 != Ongoing) {
                                System.out.println("Inconsistent " + Confirmed2 + " vs " + Confirmed + " " + t.getString(0, "Country/Region") + " " + t.getString(0, "Province/State") + " " + t.dateColumn("Last Update").get(0));
                            }
                        });

                        //compensate for duplicates by adding negative values
                        Row c = duplicateCompensate.appendRow();
                        c.setString("Province/State", t.getString(0, "Province/State"));
                        c.setString("Country/Region", t.getString(0, "Country/Region"));
                        c.setString("Continent", t.getString(0, "Continent"));
                        c.setDate("Last Update", t.dateColumn("Last Update").get(0));
                        //Confirmed,Deaths,Recovered,Ongoing
                        c.setInt("Confirmed", -(sum[0] - max[0]));
                        c.setInt("Deaths", -(sum[1] - max[1]));
                        c.setInt("Recovered", -(sum[2] - max[2]));
                        c.setInt("Ongoing", -(sum[3] - max[3]));
                    }
                });

        allData = allData.append(duplicateCompensate);

        if (fillMissingData) {
            while (true) {
                Table patch = missingData(allData);
                if (patch.isEmpty()) {
                    break;
                }
                allData = allData.append(patch);
            }
        }

        return allData;
    }

    private static Table missingData(Table allData) {
        allData = allData.sortOn("Country/Region", "Last Update");

        List<Pair<LocalDate, List<String>>> countriesPerDay = allData.splitOn("Last Update").asTableList().stream()
                .map(t -> {
                    LocalDate date = t.dateColumn("Last Update").get(0);
                    List<String> countries = t.stringColumn("Country/Region").unique().asList();

                    return Pair.of(date, countries);
                })
                .sorted((p1, p2) -> p1.getLeft().compareTo(p2.getLeft()))
                .collect(Collectors.toList());

        List<String> prev = null;
        LocalDate prevDate = null;

        Table patch = allData.copy();
        patch.clear();

        for (Pair<LocalDate, List<String>> timestamp : countriesPerDay) {
            if (prev != null) {
                Set<String> today = new HashSet<>(timestamp.getRight());
                Set<String> yesterday = new HashSet<>(prev);

                if (!today.containsAll(yesterday)) {
                    yesterday.removeAll(today);
                    Table yesterdayValues = sum(allData, prevDate, yesterday.toArray(String[]::new));
                    double count = yesterdayValues
                            .summarize("Ongoing", AggregateFunctions.sum)
                            .apply()
                            .doubleColumn("Sum [Ongoing]")
                            .sum();

                    System.out.println(timestamp.getLeft() + " is missing at least " + count + " from " + yesterday);

//                if (count > 500) {
                    //patch in with today's date the same value
                    yesterdayValues.forEach(row -> row.setDate("Last Update", timestamp.getLeft()));
                    patch = patch.append(yesterdayValues);
//                }
                }
            }

            prev = timestamp.getRight();
            prevDate = timestamp.getLeft();
        }

        return patch;
    }

    private static Table sum(Table data, LocalDate date, String... countries) {
        data = data.where(data.dateColumn("Last Update").isEqualTo(date));
        return data.where(data.stringColumn("Country/Region").isIn(Collections.list(countries)));
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
