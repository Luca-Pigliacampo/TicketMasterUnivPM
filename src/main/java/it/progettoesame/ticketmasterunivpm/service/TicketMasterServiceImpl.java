package it.progettoesame.ticketmasterunivpm.service;

import it.progettoesame.ticketmasterunivpm.exceptions.EventParseExcpetion;
import it.progettoesame.ticketmasterunivpm.exceptions.EventsNotFoundException;
import it.progettoesame.ticketmasterunivpm.filter.EventsFilter;
import it.progettoesame.ticketmasterunivpm.model.Event;
import it.progettoesame.ticketmasterunivpm.parser.EventsParser;
import it.progettoesame.ticketmasterunivpm.stats.EventsStats;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.util.*;


@Service
public class TicketMasterServiceImpl implements TicketMasterServiceInt {

    final private JSONObject events = new JSONObject();
    final private JSONObject allStats = new JSONObject();
    final private JSONArray statsArray = new JSONArray();
    final private String[] supportedEventsParam = {"countryCode", "city", "local_date", "segment", "genre", "subgenre"};
    final private String[] supportedStatsParam = {"countryCode", "city"};
    final private String[] supportedCountries = {"AL", "AT", "BE", "BG", "CH", "CY", "CZ", "DE", "DK", "EE", "ES", "FO",
            "FI", "FR", "GB", "GR", "HR", "HU", "IE", "IS", "IT", "LT", "LU", "MC", "ME", "MT", "ND", "NL", "NO", "PL",
            "PT", "RO", "RS", "SE", "SK", "SI", "TR", "UA"};
    final private EventsParser eventsParser = new EventsParser();
    final private EventsFilter eventsFilter = new EventsFilter();
    final private EventsStats eventsStats = new EventsStats();

    /**
     * Questo metodo ricava l'url con cui il programma dovrà effettuare la chiamata API.
     *
     * @param c il codice del paese inserito come parametro dall'utente
     * @return una stringa contente l'url
     *
     * @author amir-othmani
     */
    private String getUrl(String c) {
        String urlBase = "https://app.ticketmaster.com/discovery/v2/events.json?apikey=ytOGRTWK4lKDd4B9gvj8odbPaejuGh8V";
        return urlBase + "&countryCode=" + c + "&size=200";
    }

    /**
     * Questo metodo costruisce il JSONObject da restituire all'utente a partire dall'url associato alla richiesta.
     *
     * @param selectedCountry il codice del paese selezionato dall'utente
     * @throws EventParseExcpetion se il JSONObject ricavato dalla chiamata API associata al paese selezionato risulta
     *                              incompatibile con la struttura degli eventi data dal model
     * @throws EventsNotFoundException se il paese selezionato non ha nessun evento
     *
     * @author amir-othmani
     */
    private void buildEventsFromURL(String selectedCountry) throws EventParseExcpetion, EventsNotFoundException {
        try {
                events.clear();
                InputStream input = new URL(getUrl(selectedCountry)).openStream();
                JSONParser parser = new JSONParser();
                JSONObject result = (JSONObject) parser.parse(new InputStreamReader(input));
                eventsParser.buildEventsArray(result);
                if (eventsParser.getNotFilteredEvents().isEmpty())
                    throw new EventsNotFoundException();
                else {
                    events.put("num_events_found", eventsParser.getNotFilteredEvents().size());
                    events.put("list_events_found", eventsParser.getNotFilteredEvents());
                }
        }
        catch ( ParseException | IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     * Questo metodo ricava tutte le città che sono presenti in una certa lista di eventi.
     *
     * @param events gli eventi da cui ricavare tutte le città
     * @return un ArrayList&ltEvent&gt che contiene appunto tutte le città trovate
     *
     * @author amir-othmani
     */
    private ArrayList<String> getAllCities(ArrayList<Event> events) {
        ArrayList<String> containerCities = new ArrayList<>();
        for (Event e: events) {
            if (!containerCities.contains(e.getCity()))
                containerCities.add(e.getCity());
        }
        return containerCities;
    }

    /**
     * Questo metodo restituisce tutti parametri supportati dal programma per quanto riguarda la ricerca degli eventi.
     *
     * @return un array di stringhe contente i parametri supportati
     *
     * @author amir-othmani
     */
    public String[] getSupportedEventsParam() {
        return supportedEventsParam;
    }

    /**
     * Questo metodo restituisce tutti parametri supportati dal programma per quanto riguarda la ricerca delle
     * statistiche.
     *
     * @return un array di stringhe contente i parametri supportati
     *
     * @author amir-othmani
     */
    public String[] getSupportedStatsParam() {
        return supportedStatsParam;
    }

    /**
     * Questo metodo verifica se i parametri selezionati dall'utente siano supportati dal programma.
     *
     * @param param i parametri inseriti dall'utente
     * @param supportedParam  i parametri supportati
     * @return un valore booleano
     */
    public boolean areSupportedParam(HashMap<String, String> param, String[] supportedParam) {
        if (param.size()>supportedParam.length)
            return false;
        else {
            int verify=0;
            for (String p : supportedParam) {
                if (param.containsKey(p))
                    verify++;
            }
            return verify == param.size();
        }
    }
    /**
     * Questo metodo verifica se il paese selezionato dall'utente sia supportato dal programma.
     *
     * @param country il paese selezionato dall'utente
     * @return un valore booleano
     *
     * @author amir-othmani
     */
    public boolean isSupportedCountry(String country) {
        for (String c: supportedCountries) {
            if (c.equals(country))
                return true;
        }
        return false;
    }

    /**
     * Questo metodo restituisce gli eventi sulla base dei parametri inseriti dall'utente e richiama tutti gli altri
     * metodi necessari.
     *
     * @param selectedParam i parametri inseriti dall'utente
     * @return un JSONObject che conterrà la lista degli eventi (eventualmente filtrati) trovati e il loro numero
     *
     * @author amir-othmani
     */
    public JSONObject getEvents(HashMap<String, String> selectedParam) {
        try {
            buildEventsFromURL(selectedParam.get("countryCode"));
            selectedParam.remove("countryCode");
            if (!selectedParam.isEmpty() && !eventsParser.getNotFilteredEvents().isEmpty())
                return eventsFilter.filterEvents(selectedParam, eventsParser.getNotFilteredEvents());
        }
        catch ( Exception e ) {
            events.put("events_not_found", e.getMessage());
        }
        return events;
    }

    /**
     * Questo metodo restituisce le statistiche settimanali riguardante gli eventi del paese selezionato dall'utente
     * raggruppate per città o, se l'utente inserisce la città come parametro, solo della città selezionata.
     *
     * @param selectedParam i parametri inseriti dall'utente
     * @return un JSONObject che contiene le statistiche ricavate.
     *
     * @author amir-othmani
     */
    public JSONObject getStats(HashMap<String, String> selectedParam) {
        try {
            allStats.clear();
            buildEventsFromURL(selectedParam.get("countryCode"));
            if (eventsParser.getNotFilteredEvents().isEmpty())
                throw new EventsNotFoundException();
            if (selectedParam.containsKey("city")) {
                eventsFilter.buildFilteredEvents(eventsParser.getNotFilteredEvents(), selectedParam);
                return eventsStats.statsPerWeek(eventsFilter.getListFilteredEvents(), selectedParam);
            }
            statsArray.clear();
            for (String city: getAllCities(eventsParser.getNotFilteredEvents())) {
                HashMap<String, String> paramCity = new HashMap<>();
                paramCity.put("city", city);
                eventsFilter.buildFilteredEvents(eventsParser.getNotFilteredEvents(), paramCity);
                statsArray.add(eventsStats.statsPerWeek(eventsFilter.getListFilteredEvents(), paramCity));
            }
            allStats.put("country", eventsParser.getNotFilteredEvents().get(0).getCountry());
            allStats.put("all_cities", statsArray);
        }
        catch ( Exception e ) {
            allStats.put("events_not_found", e.getMessage());
        }
        return allStats;
    }
}
