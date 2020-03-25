package com.example.quickcovid;

public class Country {

    public static String continentOf(String name) {
        //keep separate
        if ("Mainland China".equals(name) || "China".equals(name)) {
            return "China";
        }
        if ("Others".equals(name) || "Cruise Ship".equals(name)) {
            return "Cruise Ship";
        }
        if ("Japan".equals(name)) {
            return "Japan";
        }

        //ugly way to encode all the countries
        String europe = "/Kosovo/Jersey/Montenegro/Guernsey/Channel Islands/Holy See/Republic of Moldova/Cyprus/Albania/Moldova/Bulgaria/Republic of Ireland/Malta/Serbia/Slovakia/Vatican City/Gibraltar/Faroe Islands/Bosnia and Herzegovina/Liechtenstein/Portugal/Poland/Slovenia/Hungary/Ukraine/Andorra/Latvia/San Marino/North Ireland/Lithuania/Belarus/Iceland/Czech Republic/Czechia/Netherlands/Italy/France/Germany/Spain/UK/United Kingdom/Denmark/Finland/Ireland/Estonia/Monaco/Luxembourg/Croatia/Greece/Romania/Switzerland/Austria/Sweden/Belgium/North Macedonia/Norway/";
        if (europe.contains("/" + name + "/")) {
            return "Europe";
        }
        String nAmerica = "/US/Canada/Mexico/Greenland/Nicaragua/Bahamas/The Bahamas/Bahamas, The/Saint Vincent and the Grenadines/Barbados/Belize/Dominica/El Salvador/Dominican Republic/Saint Barthelemy/Costa Rica/Grenada/Haiti/Martinique/St. Martin/Saint Martin/Panama/Honduras/Jamaica/Cayman Islands/Guadeloupe/Guatemala/Trinidad and Tobago/Puerto Rico/Aruba/Cuba/Saint Lucia/Antigua and Barbuda/Curacao/";
        if (nAmerica.contains("/" + name + "/")) {
            return "North America";
        }
        String sAmerica = "/Suriname/Fench Guiana/Venezuela/Uruguay/Bolivia/Paraguay/Peru/Chile/Argentina/Brazil/Ecuador/Colombia/French Guiana/Guyana/";
        if (sAmerica.contains("/" + name + "/")) {
            return "South America";
        }
        String asia = "/East Timor/Timor-Leste/Uzbekistan/Syria/Kyrgyzstan/Laos/Kazakhstan/Turkey/occupied Palestinian territory/Macao SAR/Russian Federation/Viet Nam/Taipei and environs/Hong Kong SAR/Republic of Korea/Korea, South/Korea  South/Iran (Islamic Republic of)/Mongolia/Brunei/Bangladesh/Maldives/Bhutan/Palestine/Jordan/Indonesia/Armenia/Saudi Arabia/Qatar/Georgia/Azerbaijan/Macau/Sri Lanka/Kuwait/Nepal/Cambodia/South Korea/Singapore/Hong Kong/Iran/Iraq/Thailand/Bahrain/Taiwan/Taiwan*/Kuwait/Malaysia/Vietnam/United Arab Emirates/Oman/India/Philippines/Israel/Lebanon/Pakistan/Russia/Afghanistan/";
        if (asia.contains("/" + name + "/")) {
            return "Asia";
        }

        String africa = "/Cape Verde/Mayotte/Republic of the Congo/The Gambia/Gambia, The/Cabo Verde/Angola/Zambia/Zimbabwe/Uganda/Tanzania/Seychelles/Somalia/Liberia/Libya/Madagascar/Mauritius/Mozambique/Niger/Angola/Benin/Central African Republic/Chad/Congo (Brazzaville)/Djibouti/Equatorial Guinea/Eritrea/Eswatini/Gambia/Rwanda/Ethiopia/Gabon/Mauritania/Kenya/Guinea/Namibia/Ghana/Sudan/Reunion/Cote d'Ivoire/Congo (Kinshasa)/Burkina Faso/Togo/Cameroon/South Africa/Tunisia/Senegal/Morocco/Algeria/Egypt/Nigeria/Ivory Coast/";
        if (africa.contains("/" + name + "/")) {
            return "Africa";
        }

        String oceania = "/Australia/New Zealand/Fiji/Papua New Guinea/Guam/";
        if (oceania.contains("/" + name + "/")) {
            return "Australia/Oceania";
        }

        System.out.println("No continent for " + name);
        return "N/A";
    }
}
