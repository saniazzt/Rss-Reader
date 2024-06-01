import java.net.*;
import java.io.*;

import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jsoup.Jsoup;
import org.w3c.dom.NodeList;

public class Main{

    private static final int MAX_ITEMS = 5;
    private static String[][] data = new String[100][4];



    public static void main(final String[] args) throws Exception
    {
        mainMenu();
    }

    public static void mainMenu() throws Exception{

        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to Rss Reader!");

        File f = new File("src/data.txt");
        FileReader fileReader = new FileReader(f);
        BufferedReader reader = new BufferedReader(fileReader);
        String line;
        String[] info;
        int t = 0;
        while ((line = reader.readLine()) != null){
            info = line.split(";");
            data[t][0] = info[0];
            data[t][1] = fetchPageSource(info[1]);
            data[t][2] = extractRssUrl(info[1]);
            t++;
        }

        while (true){

            System.out.println("Type a valid number for your desired action:\n"
                    +"[1] Show updates\n[2] Add URL\n[3] Remove URL\n[4] Exit");
            int option = scanner.nextInt();
            switch (option) {
                case 1:
                    showUpdates();
                    break;
                case 2:
                    addUrl();
                    break;
                case 3:
                    removeUrl();
                    break;
                case 4:
                    FileWriter writer = new FileWriter(f,true);
                    BufferedWriter bufferedWriter = new BufferedWriter(writer);
                    for (String[] d : data){
                        if (d[0]==null || d[3]==null) continue;
                        bufferedWriter.write(d[0]+";"+d[3]+"index.html"+';'+d[3]+"rss.xml\n");
                        System.out.println(d[0]+";"+d[3]+"index.html"+';'+d[3]+"rss.xml\n");
                    }
                    bufferedWriter.close();
                    System.exit(0);
                default:
                    break;
            }
        }
    }

    public static void addUrl() throws Exception {
        System.out.println("Please enter website URL to add:");
        Scanner scanner = new Scanner(System.in);
        String link = scanner.next();
        String html = fetchPageSource(link);
        String title = extractPageTitle(html);
        String rss = extractRssUrl(link);
        boolean exists = false;
        for (String[] d : data){
            if (title.equals(d[0])){
                System.out.println(link+" already exists.");
                exists = true;
                break;
            }
        }

        if (!exists){

            for (String[] d : data){
                if (d[0]==null && d[1]==null && d[2]==null){
                    d[0] = title;
                    d[1] = html;
                    d[2] = rss;
                    d[3] = link;
                    break;
                }
            }

            System.out.println("Added "+link+" succesfully.");
        }

    }

    public static void removeUrl() throws Exception {
        System.out.println("Please enter website URL to remove:");
        Scanner scanner = new Scanner(System.in);
        String link = scanner.next();
        String title = extractPageTitle(fetchPageSource(link));
        boolean exists = false;
        int t = 0;
        for (String[] d : data){

            if (title.equals(d[0])){
                exists = true;
                break;
            }
            t++;
        }
        if (!exists){
            System.out.println("Couldn't find " + link);
        }else{
            for (int i = t; i < data.length -1 ;i++){
                data[i][0] = data [i+1][0];
                data[i][1] = data [i+1][1];
                data[i][2] = data [i+1][2];
            }

            System.out.println("Removed "+link+" succesfully.");

        }

    }

    public static int showUpdates(){
        System.out.println("Show updates for:");
        System.out.println("[0] All websites");
        int t = 1;
        for (String[] d : data){
            if (d[0]!=null)
                System.out.println("["+String.valueOf(t++)+"]" + d[0]);

        }
        System.out.println("Enter -1 to return.");

        Scanner scanner = new Scanner(System.in);
        int option = scanner.nextInt();
        if (option==0){
            for (String[] d : data){
                if (d[2]==null) continue;
                retrieveRssContent(d[2]);
            }
        } else if (option==-1){
            return -1;
        } else {
            retrieveRssContent(data[option-1][2]);
        }
        return 1;
    }

    /////////////////////////

    public static String extractPageTitle(String html){
        try{
            org.jsoup.nodes.Document doc = Jsoup.parse(html);
            return doc.select("title").first().text();
        }
        catch (Exception e){
            return "Error: no title tag found in page source!";
        }
    }

    public static String extractRssUrl(String url) throws IOException
    {
        org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
        return doc.select("[type='application/rss+xml']").attr("abs:href");
    }

    public static String fetchPageSource(String urlString) throws Exception
    {
        URI uri = new URI(urlString);
        URL url = uri.toURL();
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML ,like Gecko) Chrome/108.0.0.0 Safari/537.36");
        return toString(urlConnection.getInputStream());
    }

    private static String toString(InputStream inputStream) throws IOException
    {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream , "UTF-8"));
        String inputLine;
        StringBuilder stringBuilder = new StringBuilder();
        while ((inputLine = bufferedReader.readLine()) != null)
            stringBuilder.append(inputLine);

        return stringBuilder.toString();
    }

    public static void retrieveRssContent(String rssUrl)
    {
        try {
            String rssXml = fetchPageSource(rssUrl);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            StringBuilder xmlStringBuilder = new StringBuilder();
            xmlStringBuilder.append(rssXml);
            ByteArrayInputStream input = new ByteArrayInputStream(
                    xmlStringBuilder.toString().getBytes("UTF-8"));
            org.w3c.dom.Document doc = documentBuilder.parse(input);
            NodeList itemNodes = doc.getElementsByTagName("item");

            for (int i = 0; i < MAX_ITEMS; ++i) {
                org.w3c.dom.Node itemNode = itemNodes.item(i);
                if (itemNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    org.w3c.dom.Element element = (org.w3c.dom.Element) itemNode;
                    System.out.println("Title: " + element.getElementsByTagName("title").item(0).getTextContent())
                    ;
                    System.out.println("Link: " + element.getElementsByTagName("link").item(0).getTextContent());
                    System.out.println("Description: " + element.getElementsByTagName("description").item(0).
                            getTextContent());
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("Error in retrieving RSS content for " + rssUrl + ": " + e.getMessage());
        }
    }
}