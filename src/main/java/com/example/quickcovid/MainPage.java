package com.example.quickcovid;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
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
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.charts.ChartBuilder;

public class MainPage extends HtmlPageBootstrap {

    private Table allData;
    private Table europeData;
    private final LocalDate lastDate;
    private final double maxOngoing;

    public MainPage() {
        europeData = DailyReportsReader.allData();
        europeData = europeData.splitOn("Continent").asTableList().stream()
                .filter(t -> t.getString(0, "Continent").equals("Europe") || t.getString(0, "Continent").equals("North America"))
                .reduce(Table::append)
                .get();

        europeData = europeData.splitOn("Country/Region").asTableList().stream()
                .filter(t -> t.getString(0, "Country/Region").equals("US") || t.getString(0, "Continent").equals("Europe"))
                .reduce(Table::append)
                .get();

        europeData = europeData.summarize("Ongoing", "Confirmed", AggregateFunctions.sum)
                .by("Country/Region", "Last Update", "Continent");

        europeData.column("Sum [Ongoing]").setName("Ongoing");
        europeData.column("Sum [Confirmed]").setName("Confirmed");

        {
            //for each country find the latest number, match it with a previous date on Italy,
            //shift the date and change the country (aka legend label) name

            //1. for each country find the latest number
            Map<String, Double> maxCountry = europeData.splitOn("Country/Region").asTableList().stream()
                    .map(t -> Pair.of(
                    t.getString(0, "Country/Region"),
                    t.doubleColumn("Ongoing").max()))
                    .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
            Map<String, LocalDate> latestCountry = europeData.splitOn("Country/Region").asTableList().stream()
                    .map(t -> Pair.of(
                    t.getString(0, "Country/Region"),
                    t.dateColumn("Last Update").max()))
                    .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
            LocalDate latestItaly = latestCountry.get("Italy");

            //get italy
            final Table italy = europeData.splitOn("Country/Region").asTableList().stream()
                    .filter(t -> t.getString(0, "Country/Region").equals("Italy"))
                    .findAny().get()
                    .sortDescendingOn("Ongoing");

            //2.match it with a previous date on Italy
            Map<String, LocalDate> delayCountry = maxCountry.entrySet().stream()
                    .map(e -> {
                        String country = e.getKey();
                        Double max = e.getValue();

                        int delay = 0;
                        DoubleColumn italyOngoing = italy.doubleColumn("Ongoing");
                        DateColumn italyLastUpdate = italy.dateColumn("Last Update");
                        LocalDate italyMatchDate = italyLastUpdate.get(0);

                        while (delay < italy.rowCount()) {
                            double itOngoing = italyOngoing.getDouble(delay);
                            italyMatchDate = italyLastUpdate.get(delay);
                            if (itOngoing <= max) {
                                break;
                            }
                            delay++;
                        }

                        return Pair.of(country, italyMatchDate);
                    })
                    .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

            //remove countries more than 14 days away to make the chart more readable
            europeData = europeData.splitOn("Country/Region").asTableList().stream()
                    .filter(t -> {
                        String country = t.getString(0, "Country/Region");
                        return country.equals("Romania") || ChronoUnit.DAYS.between(delayCountry.get(country), latestItaly)  <= 14;
                    })
                    .reduce(Table::append)
                    .get();

            //3.shift country by delay days
            europeData.forEach((row) -> {
                String country = row.getString("Country/Region");
                long delay = ChronoUnit.DAYS.between(delayCountry.get(country), latestItaly);
                if (delay > 0) {
                    LocalDate delayedDate = row.getDate("Last Update").minusDays(delay);
                    row.setDate("Last Update", delayedDate);
                    row.setString("Country/Region", country + "(-" + delay + ")");
                }
            });
        }

        allData = DailyReportsReader.allData(true);
        //remove incomplete data on March 11th, 12th and 13th
        allData = allData.dropWhere(allData.dateColumn("Last Update").isBetweenIncluding(LocalDate.of(2020, 3, 11), LocalDate.of(2020, 3, 15)));

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

        final Table fallData = allData;

        allData.splitOn("Continent")
                .asTableList()
                .forEach((Table t) -> {
                    Table continentSorted = t.sortDescendingOn("Last Update");
                    DoubleFunction<Double> trendOngoing = trend(continentSorted, "Ongoing", "Last Update");
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
        //include estimated values for max ongoing
        maxOngoing = allData.doubleColumn("Ongoing").max();
    }

    @Override
    protected BSNavbar createNavbar() {
        BSNavbar bar = super.createNavbar();

        bar.add(new BSNavItem("COVID", "/"));
        bar.add(new BSNavItem("Source on <i class=\"fab fa-github\"></i> GitHub", "https://github.com/emilianbold/covid/"));
        return bar;
    }

    @Override
    protected BSComponentContainer createContentPane() {
        BSPanel p = new BSPanel();

        p.add(new BSHeading("Charts", 1));

        {
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
        }
        {
            ChartBuilder chartBuilder = ChartBuilder.createBuilder()
                    .dataTable(europeData)
                    .chartType(ChartBuilder.CHART_TYPE.TIMESERIES)
                    .columnsForViewColumns("Last Update")
                    .columnsForViewRows("Ongoing")
                    .columnForColor("Country/Region");

            chartBuilder.getLayoutBuilder()
                    .autosize(true);

            p.add(new BSCard(new TSFigurePanel(chartBuilder.divName("EuropeDaysBehind").build(), "EuropeDaysBehind"),
                    "Days behind Italy"));
        }
        {

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

    private static DoubleFunction<Double> trend(Table t, String columnName, String dateName) {
        DoubleColumn values = t.doubleColumn(columnName);
        double y2 = values.get(0);
        double x2 = values.size() - 1;

        double y1;

        final int xStep;

        if (x2 == 0) {
            //for 1st value assume a start at 0
            y1 = 0;
            xStep = 1;
        } else {
            y1 = values.get(1);
            DateColumn dates = t.dateColumn(dateName);
            LocalDate today = dates.get(0);
            LocalDate prev = dates.get(1);
            xStep = (int) ChronoUnit.DAYS.between(prev, today);
        }
        double x1 = x2 - xStep;

        double slope = (y1 - y2) / (x1 - x2);

        double offset = y2 - (slope * x2);

        return x -> offset + slope * x;
    }
}
