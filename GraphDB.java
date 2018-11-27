import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Wraps the parsing functionality of the MapDBHandler as an example.
 * You may choose to add to the functionality of this class if you wish.
 * @author Alan Yao
 */
public class GraphDB {
    /**
     * Example constructor shows how to create and start an XML parser.
     *
     * @param dbPath Path to the XML file to be parsed.
     */
    private final Hashtable<Long, GraphNode> contents = new Hashtable<Long, GraphNode>();
    static HashMap<Long, GraphNode> nodeList = new HashMap<>();


    public GraphDB(String dbPath) {
        try {
            File inputFile = new File(dbPath);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            MapDBHandler maphandler = new MapDBHandler(this);
            saxParser.parse(inputFile, maphandler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        HashMap<String, Connections> r = MapDBHandler.getWayarr();


        for (String s : r.keySet()) {
            r.get(s).getList();
            for (GraphNode s1 : r.get(s).getList()) {
                nodeList.put(s1.id, s1);
            }
        }
        clean();
    }

    public void add(GraphNode node) {
        contents.put(node.id, node);
    }

    public GraphNode get(String id) {
        return contents.get(id);
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     *
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     * Remove nodes with no connections from the graph.
     * While this does not guarantee that any two nodes in the remaining graph are connected,
     * we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        Enumeration<Long> enumKey = contents.keys();
        while (enumKey.hasMoreElements()) {
            Long key = enumKey.nextElement();
            GraphNode val = contents.get(key);
            if (val.neighbours == null) {
                contents.remove(key);
            }
        }
    }

}


