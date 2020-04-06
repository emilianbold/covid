package net._410go.covid;

import quicksilver.commons.app.SimpleApplication;
import quicksilver.commons.app.SimpleWebServer;
import quicksilver.webapp.simpleui.HtmlPage;
import quicksilver.webapp.simpleui.HtmlStreamStringBuffer;

public class Start {

    public static void main(String[] args) {
        SimpleApplication application = new SimpleApplication("quickcovid", ".cache/quickcovid") {
            @Override
            public boolean isSchedulerEnabled() {
                return false;
            }

        };

        SimpleWebServer webServer = new SimpleWebServer(application) {
            @Override
            protected void initRoutes() {
                webServer.get("/", (request, response) -> {
                    HtmlPage page = new MainPage();
                    HtmlStreamStringBuffer sb = new HtmlStreamStringBuffer();
                    page.render(sb);
                    return sb.getText();
                });
            }

        };
        webServer.startServer();
    }

}
