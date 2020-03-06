package com.example.quickcovid;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.function.DoubleFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import quicksilver.webapp.simpleui.HtmlPageBootstrap;
import quicksilver.webapp.simpleui.HtmlStream;
import quicksilver.webapp.simpleui.HtmlStreamStringBuffer;
import quicksilver.webapp.simpleui.bootstrap4.charts.TSFigurePanel;
import quicksilver.webapp.simpleui.bootstrap4.components.BSCard;
import quicksilver.webapp.simpleui.bootstrap4.components.BSComponentContainer;
import quicksilver.webapp.simpleui.bootstrap4.components.BSHeading;
import quicksilver.webapp.simpleui.bootstrap4.components.BSNavItem;
import quicksilver.webapp.simpleui.bootstrap4.components.BSNavbar;
import quicksilver.webapp.simpleui.bootstrap4.components.BSPanel;
import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DateTimeColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.charts.ChartBuilder;
import tech.tablesaw.io.Source;
import tech.tablesaw.io.csv.CsvReader;

public class MainPage extends HtmlPageBootstrap {

    private LocalDate lastDate;
    private double maxOngoing;

    @Override
    protected BSNavbar createNavbar() {
        BSNavbar bar = super.createNavbar();

        bar.add(new BSNavItem("COVID", "/"));
        return bar;
    }

    @Override
    protected BSComponentContainer createContentPane() {
        BSPanel p = new BSPanel();

        p.add(new BSHeading("Charts", 1));

        {
            Table allData = getAllData();

            StringColumn continent = allData.stringColumn("Country/Region")
                    .map(MainPage::continentOf, n -> StringColumn.create("Continent"));

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

            allData = allData.summarize("Ongoing", "Confirmed", AggregateFunctions.sum)
                    .by("Country/Region", "Last Update", "Continent");

            allData.column("Sum [Ongoing]").setName("Ongoing");
            allData.column("Sum [Confirmed]").setName("Confirmed");

            System.out.println(allData.toString());

            allData = allData.summarize("Ongoing", "Confirmed", AggregateFunctions.sum)
                    .by("Last Update", "Continent");
            allData.column("Sum [Ongoing]").setName("Ongoing");
            allData.column("Sum [Confirmed]").setName("Confirmed");
            System.out.println(allData.toString());

            lastDate = allData.dateColumn("Last Update").max();
            maxOngoing = allData.doubleColumn("Ongoing").max();

            final Table fallData = allData;

            allData.splitOn("Continent")
                    .asTableList()
                    .forEach((Table t) -> {
                        Table continentSorted = t.sortDescendingOn("Last Update");
                        DoubleFunction<Double> trendOngoing = trend(continentSorted, "Ongoing");
                        double x2 = continentSorted.rowCount() - 1;

                        for (int days = 1; days <= 7; days++) {
                            double x3 = x2 + days;

                            double nextOngoing = trendOngoing.apply(x3);

                            Row row = fallData.appendRow();
                            row.setString("Continent", continentSorted.getString(0, "Continent"));
                            row.setDate("Last Update", continentSorted.dateColumn("Last Update").get(0).plusDays(days));
                            if (nextOngoing >= 0) {
                                row.setDouble("Ongoing", nextOngoing);
                            }
                        }
                    });

            ChartBuilder chartBuilder = ChartBuilder.createBuilder()
                    .dataTable(allData)
                    .chartType(ChartBuilder.CHART_TYPE.TIMESERIES)
                    .columnsForViewColumns("Last Update")
                    .columnsForViewRows("Ongoing")
                    .columnForColor("Continent");

//            chartBuilder.getLayoutBuilder()
//                    .autosize(true)
//                    .height(500);
            p.add(new BSCard(new TSFigurePanel(chartBuilder.divName("Ongoing").build(), "Ongoing"),
                    "Ongoing"));

            ChartBuilder chartBuilder2 = ChartBuilder.createBuilder()
                    .dataTable(allData)
                    .chartType(ChartBuilder.CHART_TYPE.TIMESERIES)
                    .columnsForViewColumns("Last Update")
                    .columnsForViewRows("Confirmed")
                    .columnForColor("Continent");

            p.add(new BSCard(new TSFigurePanel(chartBuilder2.divName("Confirmed").build(), "Confirmed"),
                    "Confirmed"));

        }

        return p;
    }

    Table getAllData() {
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

    static String continentOf(String name) {
        //keep separate
        if ("Mainland China".equals(name)) {
            return "China";
        }
        if ("Others".equals(name)) {
            return "Others";
        }
        if ("Japan".equals(name)) {
            return "Japan";
        }

        //ugly way to encode all the countries
        String europe = "/Gibraltar/Faroe Islands/Bosnia and Herzegovina/Liechtenstein/Portugal/Poland/Slovenia/Hungary/Ukraine/Andorra/Latvia/San Marino/North Ireland/Lithuania/Belarus/Iceland/Czech Republic/Netherlands/Italy/France/Germany/Spain/UK/Denmark/Finland/Ireland/Estonia/Monaco/Luxembourg/Croatia/Greece/Romania/Switzerland/Austria/Sweden/Belgium/North Macedonia/Norway/";
        if (europe.contains("/" + name + "/")) {
            return "Europe";
        }
        String nAmerica = "/US/Canada/Mexico/Dominican Republic/Saint Barthelemy/";
        if (nAmerica.contains("/" + name + "/")) {
            return "North America";
        }
        String sAmerica = "/Chile/Argentina/Brazil/Ecuador/Colombia/";
        if (sAmerica.contains("/" + name + "/")) {
            return "South America";
        }
        String asia = "/Palestine/Jordan/Indonesia/Armenia/Saudi Arabia/Qatar/Georgia/Azerbaijan/Macau/Sri Lanka/Kuwait/Nepal/Cambodia/South Korea/Singapore/Hong Kong/Iran/Iraq/Thailand/Bahrain/Taiwan/Kuwait/Malaysia/Vietnam/United Arab Emirates/Oman/India/Philippines/Israel/Lebanon/Pakistan/Russia/Afghanistan/";
        if (asia.contains("/" + name + "/")) {
            return "Asia";
        }

        String africa = "/South Africa/Tunisia/Senegal/Morocco/Algeria/Egypt/Nigeria/Ivory Coast/";
        if (africa.contains("/" + name + "/")) {
            return "Africa";
        }

        String oceania = "/Australia/New Zealand/";
        if (oceania.contains("/" + name + "/")) {
            return "Australia/Oceania";
        }

        System.out.println("No continent for " + name);
        return "N/A";
    }

    @Override
    public void render(HtmlStream stream) {
        HtmlStreamStringBuffer sb = new HtmlStreamStringBuffer();
        super.render(sb);

        String text = sb.getText();
        //take the 1st chart
        String layoutText = "var layout = {";

        StringBuilder b = new StringBuilder(text);

        b.insert(text.indexOf(layoutText) + layoutText.length(),
                "\nshapes: [\n"
                + "    {\n"
                + "      type: 'line',\n"
                + "      x0: \"" + lastDate.toString() + "\",\n"
                + "      y0: 0,\n"
                + "      x1: \"" + lastDate.toString() + "\",\n"
                + "      y1: " + maxOngoing + ",\n"
                + "      line: {\n"
                + "        color: 'rgb(192,192,192)',\n"
                + "        width: 1,\n"
                + "        dash: 'dot',\n"
                + "      }\n"
                + "    }\n"
                + "],\n");

        stream.write(b.toString());
    }

    private static DoubleFunction<Double> trend(Table t, String columnName) {
        double y2 = t.doubleColumn(columnName).get(0);
        double x2 = t.doubleColumn(columnName).size() - 1;

        double y1 = t.doubleColumn(columnName).get(1);
        double x1 = x2 - 1;

        double slope = (y1 - y2) / (x1 - x2);

        double offset = y2 - (slope * x2);

        return x -> offset + slope * x;
    }
}
