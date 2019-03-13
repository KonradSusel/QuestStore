package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.*;
import dao.connectionPool.JDBCConnectionPool;
import dao.interfaces.*;
import helpers.mime.MimeTypeResolver;
import helpers.cookie.CookieHelper;
import helpers.mime.MimeTypeResolver;
import model.Item;
import model.Quest;
import model.Room;
import model.user.Codecooler;
import model.user.Mentor;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import java.io.*;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

public class CodecoolerController implements HttpHandler {
    private static final String SESSION_COOKIE_NAME = "sessionId";
    private CookieHelper cookieHelper = new CookieHelper();
    private JDBCConnectionPool connectionPool;
    private LoginDAO loginDAO;
    private UserDAO userDAO;
    private CodecoolerDAO codecoolerDAO;
    private RoomsDAO roomsDAO;
    private MentorDAO mentorDAO;
    private DAOStore daoStore;
    private DAOQuests daoQuests;
    private Optional<HttpCookie> cookie;

    public CodecoolerController(JDBCConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
        this.loginDAO = new LoginDAOImpl(connectionPool);
        this.userDAO = new UserDaoImpl(connectionPool);
        this.codecoolerDAO = new CodecoolerDaoImpl(connectionPool);
        this.roomsDAO = new RoomsDaoImpl(connectionPool);
        this.mentorDAO = new MentorDAOImplementation(connectionPool);
        this.daoStore = new StoreDaoImpl(connectionPool);
        this.daoQuests = new QuestsDaoImpl(connectionPool);
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        this.cookie = getCookieBySessionCookieName(httpExchange);

        URI uri = httpExchange.getRequestURI();
        System.out.println("looking for: " + uri.getPath());
        String path = uri.getPath();

        String[] pathParts = path.split("/");
        String urlEnding = pathParts[pathParts.length - 1];
        int index = getIdFromURL(httpExchange, urlEnding);

        if (path.equals("/queststore/codecooler/" + index)) {
            displayProfile(httpExchange);

        } else if (path.equals("/queststore/codecooler/experience/" + index)) {
            displayExperience(httpExchange);
        } else if (path.equals("/queststore/codecooler/store/" + index)) {
            displayStore(httpExchange);
        } else if (path.equals("/queststore/codecooler/logout/" + index)) {
            System.out.println("logout");
            String sessionid = getSessionIdFromCookie(cookie);
            loginDAO.removeSessionid(sessionid);
            goToLogin(httpExchange);
        } else {
            goToLogin(httpExchange);
        }
    }





//        if (method.equals("POST")) {
//            String formData = getFormData(httpExchange);
//            Map inputs = parseFormData(formData);
//            String username = inputs.get("username").toString();
//            String password = inputs.get("password").toString();
//
//            if (userDAO.isLoginSuccessful(username, password)) {
//                String sessionId = generateSessionId();
//                cookie = Optional.of(new HttpCookie(SESSION_COOKIE_NAME, sessionId));
//                httpExchange.getResponseHeaders().add("Set-Cookie", cookie.get().toString());
//                int id = userDAO.getUserId(username, password);
//                String userType = userDAO.getUserType(id);
//                loginDAO.activateSessionId(sessionId, id);
//                redirect(httpExchange, userType, id);
//            }
//        }


    private int getIdFromURL(HttpExchange httpExchange, String urlEnding) throws IOException {
        try {
            return Integer.parseInt(urlEnding);
        } catch(NumberFormatException e){
            goToLogin(httpExchange);
        }
        return 0;
    }

    private void goToLogin(HttpExchange httpExchange) throws IOException {
        System.out.println("location change");
        httpExchange.getResponseHeaders().set("Location", "/queststore/login");
        httpExchange.sendResponseHeaders(302,0);
    }

    private void displayProfile(HttpExchange httpExchange) throws IOException {
        if (cookie.isPresent()) {
            String sessionid = getSessionIdFromCookie(cookie);
            if (loginDAO.isActiveSession(sessionid)) {
                int id = loginDAO.getUserId(sessionid);
                Codecooler codecooler = codecoolerDAO.getCodecoolerById(id);
                String response = generateResponseProfile(codecooler);
                sendResponse(httpExchange, response);
            }
        }
    }

    private void displayExperience(HttpExchange httpExchange) throws IOException {
        if (cookie.isPresent()) {
            String sessionid = getSessionIdFromCookie(cookie);
            if (loginDAO.isActiveSession(sessionid)) {
                int id = loginDAO.getUserId(sessionid);
                Codecooler codecooler = codecoolerDAO.getCodecoolerById(id);
                String response = generateResponseExperience(codecooler);
                sendResponse(httpExchange, response);
            }
        }
    }

    private void displayStore(HttpExchange httpExchange) throws IOException {
        if (cookie.isPresent()) {
            String sessionid = getSessionIdFromCookie(cookie);
            if (loginDAO.isActiveSession(sessionid)) {
                int id = loginDAO.getUserId(sessionid);
                Codecooler codecooler = codecoolerDAO.getCodecoolerById(id);
                String response = generateResponseStore(codecooler);
                sendResponse(httpExchange, response);
            }
        }
    }


    private Optional<HttpCookie> getCookieBySessionCookieName(HttpExchange httpExchange) {
        String cookieStr = httpExchange.getRequestHeaders().getFirst("Cookie");
        List<HttpCookie> cookies = cookieHelper.parseCookies(cookieStr);
        return cookieHelper.findCookieByName(SESSION_COOKIE_NAME, cookies);
    }

    private String getSessionIdFromCookie(Optional<HttpCookie> cookie) {
        String cookieValue = cookie.get().getValue();
        return cookieValue.substring(1, cookieValue.length()-1);
    }

    private void redirect(HttpExchange httpExchange, String userType, int id) throws IOException {
        httpExchange.getResponseHeaders().set("Location", "/queststore/" + userType + "/" + id);
        httpExchange.sendResponseHeaders(302,0);
    }

    private String getFormData(HttpExchange httpExchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(httpExchange.getRequestBody(), "utf-8");
        BufferedReader br = new BufferedReader(isr);
        String formData = br.readLine();
        System.out.println(formData);
        return formData;
    }

    private Map<String, String> parseFormData(String formData) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            String value = new URLDecoder().decode(keyValue[1], "UTF-8");
            map.put(keyValue[0], value);
        }
        return map;
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    private String generateResponseProfile(Codecooler codecooler) {
        int roomid = codecooler.getRoomId();
        Room room = roomsDAO.getRoomById(roomid);
        Mentor mentor = mentorDAO.getMentorByRoomId(roomid);
        List<Quest> questList = daoQuests.getCodecoolerQuestsWithQuantity(codecooler);
        List<Item> itemList = daoStore.getCodecoolerItemsWithQuantity(codecooler);
        JtwigTemplate template = JtwigTemplate.classpathTemplate("queststore/templates/profile.twig");
        JtwigModel model = JtwigModel.newModel();
        model.with("codecooler", codecooler);
        model.with("room", room);
        model.with("mentor", mentor);
        model.with("questList", questList);
        model.with("itemList", itemList);
        String response = template.render(model);
        return response;
    }

    private String generateResponseExperience(Codecooler codecooler) {
        List<Quest> questList = daoQuests.getCodecoolerQuestsWithQuantity(codecooler);
        List<Quest> questsBasic = new ArrayList<>();
        List<Quest> questExtra = new ArrayList<>();
        int quantity = 0;
        int total = 0;
        for(Quest quest : questList) {
            if(quest.getQuestType().equals("basic")) {
                questsBasic.add(quest);
            } else {
                questExtra.add(quest);
            }
            quantity += quest.getQuantity();
            total += quest.getTotal();
        }
        JtwigTemplate template = JtwigTemplate.classpathTemplate("queststore/templates/experience.twig");
        JtwigModel model = JtwigModel.newModel();
        model.with("codecooler", codecooler);
        model.with("questsBasic", questsBasic);
        model.with("questExtra", questExtra);
        model.with("quantity", quantity);
        model.with("total", total);
        String response = template.render(model);
        return response;
    }

    private String generateResponseStore(Codecooler codecooler) {
        List<Item> itemsBasic = daoStore.getBasicItems();
        List<Item> itemsMagic = daoStore.getMagicItems();

        JtwigTemplate template = JtwigTemplate.classpathTemplate("queststore/templates/store.twig");
        JtwigModel model = JtwigModel.newModel();
        model.with("codecooler", codecooler);
        model.with("itemsBasic", itemsBasic);
        model.with("itemsMagic", itemsMagic);
        String response = template.render(model);
        return response;
    }

    private void sendResponse(HttpExchange httpExchange, String response) throws IOException {
        httpExchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
