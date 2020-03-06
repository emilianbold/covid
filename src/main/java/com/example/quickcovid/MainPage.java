package com.example.quickcovid;

import java.time.LocalDate;
import java.util.function.DoubleFunction;
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
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.charts.ChartBuilder;

public class MainPage extends HtmlPageBootstrap {

    private Table allData;
    private final LocalDate lastDate;
    private final double maxOngoing;

    public MainPage() {
        allData = DailyReportsReader.allData();

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
    }

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
        DoubleColumn values = t.doubleColumn(columnName);
        double y2 = values.get(0);
        double x2 = values.size() - 1;

        double y1;

        if (x2 == 0) {
            //for 1st value assume a start at 0
            y1 = 0;
        } else {
            y1 = values.get(1);
        }
        double x1 = x2 - 1;

        double slope = (y1 - y2) / (x1 - x2);

        double offset = y2 - (slope * x2);

        return x -> offset + slope * x;
    }
}
