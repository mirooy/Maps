import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;

/**
 *  Parses OSM XML files using an XML SAX parser. Used to construct the graph of roads for
 *  pathfinding, under some constraints.
 *  See OSM documentation on
 *  <a href="http://wiki.openstreetmap.org/wiki/Key:highway">the highway tag</a>,
 *  <a href="http://wiki.openstreetmap.org/wiki/Way">the way XML element</a>,
 *  <a href="http://wiki.openstreetmap.org/wiki/Node">the node XML element</a>,
 *  and the java
 *  <a href="https://docs.oracle.com/javase/tutorial/jaxp/sax/parsing.html">SAX parser tutorial</a>.
 *  @author Alan Yao
 */
public class MapDBHandler extends DefaultHandler {
    /**
     * Only allow for non-service roads; this prevents going on pedestrian streets as much as
     * possible. Note that in Berkeley, many of the campus roads are tagged as motor vehicle
     * roads, but in practice we walk all over them with such impunity that we forget cars can
     * actually drive on them.
     */
    private static final Set<String> ALLOWED_HIGHWAY_TYPES = new HashSet<>(Arrays.asList
            ("motorway", "trunk", "primary", "secondary", "tertiary", "unclassified",
                    "residential", "living_street", "motorway_link", "trunk_link", "primary_link",
                    "secondary_link", "tertiary_link"));
    private String activeState = "";
    private final GraphDB g;
    static HashMap<Long, GraphNode> allNodes = new HashMap<>();
    private static ArrayList<GraphNode> arr2 = new ArrayList<>();
    private static HashMap<String, Connections> wayarr = new HashMap<String, Connections>();
    GraphNode n = null;
    Connections way = null;
    GraphNode prev;
    GraphNode curr;
    boolean first = false;


    public MapDBHandler(GraphDB g) {
        this.g = g;
    }

    /**
     * Called at the beginning of an element. Typically, you will want to handle each element in
     * here, and you may want to track the parent element.
     * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or
     *            if Namespace processing is not being performed.
     * @param localName The local name (without prefix), or the empty string if Namespace
     *                  processing is not being performed.
     * @param qName The qualified name (with prefix), or the empty string if qualified names are
     *              not available. This tells us which element we're looking at.
     * @param attributes The attributes attached to the element. If there are no attributes, it
     *                   shall be an empty Attributes object.
     * @throws SAXException Any SAX exception, possibly wrapping another exception.
     * @see Attributes
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        /* Some example code on how you might begin to parse XML files. */

        if (qName.equals("node")) {
            activeState = "node";
            Long id = Long.parseLong(attributes.getValue("id"));
            Double lat = Double.parseDouble(attributes.getValue("lat"));
            Double lon = Double.parseDouble(attributes.getValue("lon"));
            n = new GraphNode(id, lon, lat);
            allNodes.put(n.id, n);
        } else if (qName.equals("way")) {
            activeState = "way";
            way = new Connections(attributes.getValue("id"));
            first = false;
        } else if (activeState.equals("way") && qName.equals("nd")) {
            way.addN(allNodes.get(Long.parseLong(attributes.getValue("ref"))));
        } else if (activeState.equals("way") && qName.equals("tag")) {
            String k = attributes.getValue("k");
            String v = attributes.getValue("v");
            String name = null;
            if (k.equals("highway")) {
                name = v;
            }
            if (ALLOWED_HIGHWAY_TYPES.contains(name) && way.getSize() > 1) {
                wayarr.put(way.id, way);
                GraphNode last = null;
                for (GraphNode s : way.getList()) {
                    if (way.id == null) {
                        continue;
                    }
                    if (!first) {
                        last = s;
                        first = true;
                        continue;
                    }
                    s.neighbours.add(last);
                    last.neighbours.add(s);
                    last = s;
                }

            }
        } else if (activeState.equals("node") && qName.equals("tag") && attributes.getValue("k")
                .equals("name")) {
            n.setName(attributes.getValue("v"));
            arr2.add(n);
        }
    }


    public static HashMap<Long, GraphNode> getArr() {
        return allNodes;
    }
    public static ArrayList<GraphNode> getArr2() {
        return arr2;
    }
    public static HashMap<String, Connections> getWayarr() {
        return wayarr;
    }

    public static GraphNode closestNode(Double lon, Double lat) {
        Double smallestDistance = Double.MAX_VALUE;
        GraphNode closestNode = null;
        GraphNode curr;

        for (long s : allNodes.keySet()) {
            curr = allNodes.get(s);
            Double calclon = (allNodes.get(curr).lon - lon) * (allNodes.get(curr).lon - lon);
            Double calclat = (allNodes.get(curr).lat - lat) * (allNodes.get(curr).lat - lat);
            Double result = calclat + calclon;
            Double dist = Math.sqrt(result);
            if (dist < smallestDistance) {
                smallestDistance = dist;
                closestNode = curr;
            }
        }
        return closestNode;
    }


    /**
     * Receive notification of the end of an element. You may want to take specific terminating
     * actions here, like finalizing vertices or edges found.
     * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or
     *            if Namespace processing is not being performed.
     * @param localName The local name (without prefix), or the empty string if Namespace
     *                  processing is not being performed.
     * @param qName The qualified name (with prefix), or the empty string if qualified names are
     *              not available.
     * @throws SAXException  Any SAX exception, possibly wrapping another exception.
     */


}

