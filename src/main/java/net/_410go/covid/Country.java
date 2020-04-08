package net._410go.covid;

import java.util.Arrays;
import java.util.List;

public class Country {

    public static String continentOf(String name) {
        //keep separate
        if ("Mainland China".equals(name) || "China".equals(name)) {
            return "China";
        }
        if ("Others".equals(name) || "Cruise Ship".equals(name) || "Diamond Princess".equals(name)
                || "MS Zaandam".equals(name)) {
            return "Cruise Ship";
        }
        if ("Japan".equals(name)) {
            return "Japan";
        }

        List<String> europe = Arrays.asList(
                "Albania",
                "Andorra",
                "Austria",
                "Belarus",
                "Belgium",
                "Bosnia and Herzegovina",
                "Botswana",
                "Bulgaria",
                "Channel Islands",
                "Croatia",
                "Cyprus",
                "Czech Republic",
                "Czechia",
                "Denmark",
                "Estonia",
                "Faroe Islands",
                "Finland",
                "France",
                "Germany",
                "Gibraltar",
                "Greece",
                "Guernsey",
                "Holy See",
                "Hungary",
                "Iceland",
                "Ireland",
                "Italy",
                "Jersey",
                "Kosovo",
                "Latvia",
                "Liechtenstein",
                "Lithuania",
                "Luxembourg",
                "Malta",
                "Moldova",
                "Monaco",
                "Montenegro",
                "Netherlands",
                "North Ireland",
                "North Macedonia",
                "Norway",
                "Poland",
                "Portugal",
                "Republic of Ireland",
                "Republic of Moldova",
                "Romania",
                "San Marino",
                "Serbia",
                "Slovakia",
                "Slovenia",
                "Spain",
                "Sweden",
                "Switzerland",
                "UK",
                "Ukraine",
                "United Kingdom",
                "Vatican City");
        if (europe.contains(name)) {
            return "Europe";
        }

        List<String> nAmerica = Arrays.asList(
                "Antigua and Barbuda",
                "Aruba",
                "Bahamas",
                "Bahamas, The",
                "Barbados",
                "Belize",
                "Canada",
                "Cayman Islands",
                "Costa Rica",
                "Cuba",
                "Curacao",
                "Dominica",
                "Dominican Republic",
                "El Salvador",
                "Greenland",
                "Grenada",
                "Guadeloupe",
                "Guatemala",
                "Haiti",
                "Honduras",
                "Jamaica",
                "Martinique",
                "Mexico",
                "Nicaragua",
                "Panama",
                "Puerto Rico",
                "Saint Barthelemy",
                "Saint Kitts and Nevis",
                "Saint Lucia",
                "Saint Martin",
                "Saint Vincent and the Grenadines",
                "St. Martin",
                "The Bahamas",
                "Trinidad and Tobago",
                "US");
        if (nAmerica.contains(name)) {
            return "North America";
        }

        List<String> sAmerica = Arrays.asList(
                "Argentina",
                "Bolivia",
                "Brazil",
                "Chile",
                "Colombia",
                "Ecuador",
                "Fench Guiana",
                "French Guiana",
                "Guyana",
                "Paraguay",
                "Peru",
                "Suriname",
                "Uruguay",
                "Venezuela");
        if (sAmerica.contains(name)) {
            return "South America";
        }

        List<String> asia = Arrays.asList(
                "Afghanistan",
                "Armenia",
                "Azerbaijan",
                "Bahrain",
                "Bangladesh",
                "Burma",
                "Bhutan",
                "Brunei",
                "Cambodia",
                "East Timor",
                "Georgia",
                "Hong Kong SAR",
                "Hong Kong",
                "India",
                "Indonesia",
                "Iran (Islamic Republic of)",
                "Iran",
                "Iraq",
                "Israel",
                "Jordan",
                "Kazakhstan",
                "Korea  South",
                "Korea, South",
                "Kuwait",
                "Kuwait",
                "Kyrgyzstan",
                "Laos",
                "Lebanon",
                "Macao SAR",
                "Macau",
                "Malaysia",
                "Maldives",
                "Mongolia",
                "Nepal",
                "Oman",
                "Pakistan",
                "Palestine",
                "Philippines",
                "Qatar",
                "Republic of Korea",
                "Russia",
                "Russian Federation",
                "Saudi Arabia",
                "Singapore",
                "South Korea",
                "Sri Lanka",
                "Syria",
                "Taipei and environs",
                "Taiwan",
                "Taiwan*",
                "Thailand",
                "Timor-Leste",
                "Turkey",
                "United Arab Emirates",
                "Uzbekistan",
                "Viet Nam",
                "Vietnam",
                "West Bank and Gaza",
                "occupied Palestinian territory");
        if (asia.contains(name)) {
            return "Asia";
        }

        List<String> africa = Arrays.asList(
                "Algeria",
                "Angola",
                "Angola",
                "Benin",
                "Burkina Faso",
                "Burundi",
                "Cabo Verde",
                "Cameroon",
                "Cape Verde",
                "Central African Republic",
                "Chad",
                "Congo (Brazzaville)",
                "Congo (Kinshasa)",
                "Cote d'Ivoire",
                "Djibouti",
                "Egypt",
                "Equatorial Guinea",
                "Eritrea",
                "Eswatini",
                "Ethiopia",
                "Gabon",
                "Gambia",
                "Gambia, The",
                "Ghana",
                "Guinea",
                "Guinea-Bissau",
                "Ivory Coast",
                "Kenya",
                "Liberia",
                "Libya",
                "Madagascar",
                "Malawi",
                "Mali",
                "Mauritania",
                "Mauritius",
                "Mayotte",
                "Morocco",
                "Mozambique",
                "Namibia",
                "Niger",
                "Nigeria",
                "Republic of the Congo",
                "Reunion",
                "Rwanda",
                "Sao Tome and Principe",
                "Senegal",
                "Seychelles",
                "Sierra Leone",
                "Somalia",
                "South Africa",
                "South Sudan",
                "Sudan",
                "Tanzania",
                "The Gambia",
                "Togo",
                "Tunisia",
                "Uganda",
                "Western Sahara",
                "Zambia",
                "Zimbabwe");
        if (africa.contains(name)) {
            return "Africa";
        }

        List<String> oceania = Arrays.asList(
                "Australia",
                "Fiji",
                "Guam",
                "New Zealand",
                "Papua New Guinea");
        if (oceania.contains(name)) {
            return "Australia/Oceania";
        }

        System.out.println("No continent for " + name);
        return "N/A";
    }
}
