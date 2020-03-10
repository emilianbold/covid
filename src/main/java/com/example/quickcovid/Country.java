package com.example.quickcovid;

public class Country {

    public static String continentOf(String name) {
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
        String europe = "/Cyprus/Albania/Moldova/Bulgaria/Republic of Ireland/Malta/Serbia/Slovakia/Vatican City/Gibraltar/Faroe Islands/Bosnia and Herzegovina/Liechtenstein/Portugal/Poland/Slovenia/Hungary/Ukraine/Andorra/Latvia/San Marino/North Ireland/Lithuania/Belarus/Iceland/Czech Republic/Netherlands/Italy/France/Germany/Spain/UK/Denmark/Finland/Ireland/Estonia/Monaco/Luxembourg/Croatia/Greece/Romania/Switzerland/Austria/Sweden/Belgium/North Macedonia/Norway/";
        if (europe.contains("/" + name + "/")) {
            return "Europe";
        }
        String nAmerica = "/US/Canada/Mexico/Dominican Republic/Saint Barthelemy/Costa Rica/Martinique/St. Martin/";
        if (nAmerica.contains("/" + name + "/")) {
            return "North America";
        }
        String sAmerica = "/Paraguay/Peru/Chile/Argentina/Brazil/Ecuador/Colombia/French Guiana/";
        if (sAmerica.contains("/" + name + "/")) {
            return "South America";
        }
        String asia = "/Brunei/Bangladesh/Maldives/Bhutan/Palestine/Jordan/Indonesia/Armenia/Saudi Arabia/Qatar/Georgia/Azerbaijan/Macau/Sri Lanka/Kuwait/Nepal/Cambodia/South Korea/Singapore/Hong Kong/Iran/Iraq/Thailand/Bahrain/Taiwan/Kuwait/Malaysia/Vietnam/United Arab Emirates/Oman/India/Philippines/Israel/Lebanon/Pakistan/Russia/Afghanistan/";
        if (asia.contains("/" + name + "/")) {
            return "Asia";
        }

        String africa = "/Togo/Cameroon/South Africa/Tunisia/Senegal/Morocco/Algeria/Egypt/Nigeria/Ivory Coast/";
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
}
