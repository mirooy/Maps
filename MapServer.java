import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.List;

/* Maven is used to pull in these dependencies. */
import com.google.gson.Gson;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import static spark.Spark.*;
import java.io.File;


/**
 * This MapServer class is the entry point for running the JavaSpark web server for the BearMaps
 * application project, receiving API calls, handling the API call processing, and generating
 * requested images and routes.
 * @author Alan Yao
 */
public class MapServer {
    /**
     * The root upper left/lower right longitudes and latitudes represent the bounding box of
     * the root tile, as the images in the img/ folder are scraped.
     * Longitude == x-axis; latitude == y-axis.
     */
    public static final double ROOT_ULLAT = 37.892195547244356, ROOT_ULLON = -122.2998046875,
            ROOT_LRLAT = 37.82280243352756, ROOT_LRLON = -122.2119140625;
    /** Each tile is 256x256 pixels. */
    public static final int TILE_SIZE = 256;
    /** HTTP failed response. */
    private static final int HALT_RESPONSE = 403;
    /** Route stroke information: typically roads are not more than 5px wide. */
    public static final float ROUTE_STROKE_WIDTH_PX = 5.0f;
    /** Route stroke information: Cyan with half transparency. */
    public static final Color ROUTE_STROKE_COLOR = new Color(108, 181, 230, 200);
    /** The tile images are in the IMG_ROOT folder. */
    private static final String IMG_ROOT = "img/";

    private static HashMap<String, BufferedImage> store = new HashMap<String, BufferedImage>();
    /**
     * The OSM XML file path. Downloaded from <a href="http://download.bbbike.org/osm/">here</a>
     * using custom region selection.
     **/
    private static final String OSM_DB_PATH = "berkeley.osm";
    /**
     * Each raster request to the server will have the following parameters
     * as keys in the params map accessible by,
     * i.e., params.get("ullat") inside getMapRaster(). <br>
     * ullat -> upper left corner latitude,<br> ullon -> upper left corner longitude, <br>
     * lrlat -> lower right corner latitude,<br> lrlon -> lower right corner longitude <br>
     * w -> user viewport window width in pixels,<br> h -> user viewport height in pixels.
     **/
    private static final String[] REQUIRED_RASTER_REQUEST_PARAMS = { "ullat",
        "ullon", "lrlat", "lrlon", "w", "h"};
    /**
     * Each route request to the server will have the following parameters
     * as keys in the params map.<br>
     * start_lat -> start point latitude,<br> start_lon -> start point longitude,<br>
     * end_lat -> end point latitude, <br>end_lon -> end point longitude.
     **/
    private static final String[] REQUIRED_ROUTE_REQUEST_PARAMS = { "start_lat", "start_lon",
        "end_lat", "end_lon"};
    /* Define any static variables here. Do not define any instance variables of MapServer. */
    private static GraphDB g;

    /**
     * Place any initialization statements that will be run before the server main loop here.
     * Do not place it in the main function. Do not place initialization code anywhere else.
     * This is for testing purposes, and you may fail tests otherwise.
     **/
    public static void initialize() {
        g = new GraphDB(OSM_DB_PATH);
        Trie t = new Trie();
        for (Long s : MapDBHandler.allNodes.keySet()) {
            if (MapDBHandler.allNodes.get(s).getName() != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", MapDBHandler.allNodes.get(s).getName());
                map.put("id", MapDBHandler.allNodes.get(s).id);
                map.put("lon", MapDBHandler.allNodes.get(s).lon);
                map.put("lat", MapDBHandler.allNodes.get(s).lat);
                t.addWord(GraphDB.cleanString(MapDBHandler.allNodes.get(s).getName()),
                        MapDBHandler.allNodes.get(s).getName(),
                        new HashMap<>(map));
            }
        }
    }

    public static void main(String[] args) {
        initialize();
        staticFileLocation("/page");
        /* Allow for all origin requests (since this is not an authenticated server, we do not
         * care about CSRF).  */
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "*");
        });

        /* Define the raster endpoint for HTTP GET requests. I use anonymous functions to define
         * the request handlers. */
        get("/raster", (req, res) -> {
            HashMap<String, Double> rasterParams =
                    getRequestParams(req, REQUIRED_RASTER_REQUEST_PARAMS);
            /* Required to have valid raster params */
            validateRequestParameters(rasterParams, REQUIRED_RASTER_REQUEST_PARAMS);
            /* Create the Map for return parameters. */
            Map<String, Object> rasteredImgParams = new HashMap<>();
            /* getMapRaster() does almost all the work for this API call */
            BufferedImage im = getMapRaster(rasterParams, rasteredImgParams);
            /* Check if we have routing parameters. */
            HashMap<String, Double> routeParams =
                    getRequestParams(req, REQUIRED_ROUTE_REQUEST_PARAMS);
            /* If we do, draw the route too. */
            if (hasRequestParameters(routeParams, REQUIRED_ROUTE_REQUEST_PARAMS)) {
                findAndDrawRoute(routeParams, rasteredImgParams, im);
            }
            /* On an image query success, add the image data to the response */
            if (rasteredImgParams.containsKey("query_success")
                    && (Boolean) rasteredImgParams.get("query_success")) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                writeJpgToStream(im, os);
                String encodedImage = Base64.getEncoder().encodeToString(os.toByteArray());
                rasteredImgParams.put("b64_encoded_image_data", encodedImage);
                os.flush();
                os.close();
            }
            /* Encode response to Json */
            Gson gson = new Gson();
            return gson.toJson(rasteredImgParams);
        });

        /* Define the API endpoint for search */
        get("/search", (req, res) -> {
            Set<String> reqParams = req.queryParams();
            String term = req.queryParams("term");
            Gson gson = new Gson();
            /* Search for actual location data. */
            if (reqParams.contains("full")) {
                List<Map<String, Object>> data = getLocations(term);
                return gson.toJson(data);
            } else {
                /* Search for prefix matching strings. */
                List<String> matches = getLocationsByPrefix(term);
                return gson.toJson(matches);
            }
        });

        /* Define map application redirect */
        get("/", (request, response) -> {
            response.redirect("/map.html", 301);
            return true;
        });
    }

    /**
     * Check if the computed parameter map matches the required parameters on length.
     */
    private static boolean hasRequestParameters(
            HashMap<String, Double> params, String[] requiredParams) {
        return params.size() == requiredParams.length;
    }

    /**
     * Validate that the computed parameters matches the required parameters.
     * If the parameters do not match, halt.
     */
    private static void validateRequestParameters(
            HashMap<String, Double> params, String[] requiredParams) {
        if (params.size() != requiredParams.length) {
            halt(HALT_RESPONSE, "Request failed - parameters missing.");
        }
    }

    /**
     * Return a parameter map of the required request parameters.
     * Requires that all input parameters are doubles.
     * @param req HTTP Request
     * @param requiredParams TestParams to validate
     * @return A populated map of input parameter to it's numerical value.
     */
    private static HashMap<String, Double> getRequestParams(
            spark.Request req, String[] requiredParams) {
        Set<String> reqParams = req.queryParams();
        HashMap<String, Double> params = new HashMap<>();
        for (String param : requiredParams) {
            if (reqParams.contains(param)) {
                try {
                    params.put(param, Double.parseDouble(req.queryParams(param)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    halt(HALT_RESPONSE, "Incorrect parameters - provide numbers.");
                }
            }
        }
        return params;
    }

    /**
     * Write a <code>BufferedImage</code> to an <code>OutputStream</code>. The image is written as
     * a lossy JPG, but with the highest quality possible.
     * @param im Image to be written.
     * @param os Stream to be written to.
     */
    static void writeJpgToStream(BufferedImage im, OutputStream os) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(1.0F); // Highest quality of jpg possible
        writer.setOutput(new MemoryCacheImageOutputStream(os));
        try {
            writer.write(im);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles raster API calls, queries for tiles and rasters the full image. <br>
     * <p>
     *     The rastered photo must have the following properties:
     *     <ul>
     *         <li>Has dimensions of at least w by h, where w and h are the user viewport width
     *         and height.</li>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *     Additional image about the raster is returned and is to be included in the Json response.
     * </p>
     * @param inputParams Map of the HTTP GET request's query parameters - the query bounding box
     *                    and the user viewport width and height.
     * @param rasteredImageParams A map of parameters for the Json response as specified:
     * "raster_ul_lon" -> Double, the bounding upper left longitude of the rastered image <br>
     * "raster_ul_lat" -> Double, the bounding upper left latitude of the rastered image <br>
     * "raster_lr_lon" -> Double, the bounding lower right longitude of the rastered image <br>
     * "raster_lr_lat" -> Double, the bounding lower right latitude of the rastered image <br>
     * "raster_width"  -> Integer, the width of the rastered image <br>
     * "raster_height" -> Integer, the height of the rastered image <br>
     * "depth"         -> Integer, the 1-indexed quadtree depth of the nodes of the rastered image.
     * Can also be interpreted as the length of the numbers in the image string. <br>
     * "query_success" -> Boolean, whether an image was successfully rastered. <br>
     * @return a <code>BufferedImage</code>, which is the rastered result.
     * @see #REQUIRED_RASTER_REQUEST_PARAMS
     */


    public static BufferedImage getMapRaster(Map<String, Double> inputParams,
                                             Map<String, Object> rasteredImageParams)
            throws IOException {
        double queryDistancePerPixel = (inputParams.get("lrlon") - inputParams.get("ullon"))
                / inputParams.get("w");
        double ullat = inputParams.get("ullat");
        double ullon = inputParams.get("ullon");
        double lrlat = inputParams.get("lrlat");
        double lrlon = inputParams.get("lrlon");
        rasteredImageParams.put("query_success", true);
        QuadTree t = new QuadTree();
        List<QTreeNode> tileList = t.depthSearch(queryDistancePerPixel, ullat,
                ullon, lrlon, lrlat);
        tileList = QuadTree.sorting(tileList);
        int depth = tileList.get(0).name.length();
        rasteredImageParams.put("depth", depth);
        int rows = QuadTree.getRowLength(tileList);
        int columns = tileList.size() / rows;
        rasteredImageParams.put("raster_ul_lon", tileList.get(0).getUpperLeftPoint().getLonX());
        rasteredImageParams.put("raster_ul_lat", tileList.get(0).getUpperLeftPoint().getLatY());
        rasteredImageParams.put("raster_lr_lon", tileList.get(tileList.size() - 1)
                .getLowerRightPoint().getLonX());
        rasteredImageParams.put("raster_lr_lat", tileList.get(tileList.size() - 1)
                .getLowerRightPoint().getLatY());
        BufferedImage result = new BufferedImage(rows * 256, columns * 256,
                BufferedImage.TYPE_INT_RGB);
        rasteredImageParams.put("raster_width", rows * 256);
        rasteredImageParams.put("raster_height", columns * 256);
        Graphics glg  = result.getGraphics();
        int x = 0;
        int y = 0;
        BufferedImage bi;
        for (QTreeNode image : tileList) {
            if (store.containsKey(image.name)) {
                bi = store.get(image.name);
            } else {
                bi = ImageIO.read(new File("img/" + image.name + ".png"));
                store.put(image.name, bi);
            }
            glg.drawImage(bi, x, y, null);
            x += 256;
            if (x >= result.getWidth()) {
                x = 0;
                y += bi.getHeight();
            }
        }
        return result;
    }
    private static double calcEuclidean(double x1, double x2, double y1, double y2) {
        return Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
    }

    private static double calcEuclidean1(GraphNode x1, GraphNode x2) {
        return Math.sqrt((x1.lat - x2.lat) * (x1.lat - x2.lat) + (x1.lon - x2.lon)
                * (x1.lon - x2.lon));

    }


    public static List<Long> shortestPath(GraphNode startNode, GraphNode endNode) {
        PriorityQueue<GraphNode> fringe = new PriorityQueue<>();
        ArrayList<GraphNode> visited = new ArrayList<>();
        HashMap<Long, Long> temp = new HashMap<>();
        startNode.distance = 0;
        fringe.add(startNode);

        if (startNode == null || endNode == null) {
            return null;
        }


        while (!fringe.isEmpty()) {
            GraphNode currNode = fringe.poll();
            if (currNode.id == endNode.id) {
                break;
            }
            if (visited.contains(currNode)) {
                continue;
            } else {
                visited.add(currNode);
            }
            for (GraphNode n : currNode.neighbours) {
                double distance = currNode.distance;
                double h = calcEuclidean1(n, currNode);
                double total = distance + h;
                if (n.distance > total) {
                    n.distance = total;

                    fringe.add(n);
                    temp.put(n.id, currNode.id);
                }
            }
        }
        List<Long> arr2 = new ArrayList<Long>();
        while (startNode != endNode) {
            arr2.add(endNode.id);
            endNode = GraphDB.nodeList.get(temp.get(endNode.id));
        }
        arr2.add(startNode.id);
        Collections.reverse(arr2);
        return arr2;
    }



    /**
     * Searches for the shortest route satisfying the input request parameters, and returns a
     * <code>List</code> of the route's node ids. <br>
     * The route should start from the closest node to the start point and end at the closest node
     * to the endpoint. Distance is defined as the euclidean distance between two points
     * (lon1, lat1) and (lon2, lat2).
     * If <code>im</code> is not null, draw the route onto the image by drawing lines in between
     * adjacent points in the route. The lines should be drawn using ROUTE_STROKE_COLOR,
     * ROUTE_STROKE_WIDTH_PX, BasicStroke.CAP_ROUND and BasicStroke.JOIN_ROUND.
     * @param routeParams Params collected from the API call. Members are as
     *                    described in REQUIRED_ROUTE_REQUEST_PARAMS.
     * @param rasterImageParams parameters returned from the image rastering.
     * @param im The rastered map image to be drawn on.
     * @return A List of node ids from the start of the route to the end.
     */
    static HashMap<Map<String, Double>, GraphNode[]> memorized =
            new HashMap<>();

    public static List<Long> findAndDrawRoute(Map<String, Double> routeParams,
                                              Map<String, Object> rasterImageParams,
                                              BufferedImage im) /**/ {
        Double smallestStartDistance = Double.MAX_VALUE;
        Double smallestEndDistance = Double.MAX_VALUE;
        GraphNode startNode = null;
        GraphNode endNode = null;
        if (memorized.containsKey(routeParams)) {
            startNode = memorized.get(routeParams)[0];
            endNode = memorized.get(routeParams)[1];
        } else {
            for (GraphNode n : GraphDB.nodeList.values()) {
                n.distance = Integer.MAX_VALUE;
                if (n == null) {
                    continue;
                }
                double startDistance = calcEuclidean(routeParams.get("start_lon"),
                        n.lon, routeParams.get("start_lat"), n.lat);
                double endDistance = calcEuclidean(routeParams.get("end_lon"),
                        n.lon, routeParams.get("end_lat"), n.lat);
                if (startDistance < smallestStartDistance) {
                    smallestStartDistance = startDistance;
                    startNode = n;
                }
                if (endDistance < smallestEndDistance) {
                    smallestEndDistance = endDistance;
                    endNode = n;
                }
            }
        }
        List<Long> routingNodes = shortestPath(startNode, endNode);
        if (rasterImageParams == null && im == null) {
            return routingNodes;
        }
        Graphics2D grph = (Graphics2D) im.getGraphics();
        grph.setColor(ROUTE_STROKE_COLOR);
        grph.setStroke(new BasicStroke(MapServer.ROUTE_STROKE_WIDTH_PX,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int w = (Integer) rasterImageParams.get("raster_width");
        int h = (Integer) rasterImageParams.get("raster_height");
        boolean success = (Boolean) rasterImageParams.get("query_success");
        double ullon = (Double) rasterImageParams.get("raster_ul_lon");
        double lrlon = (Double) rasterImageParams.get("raster_lr_lon");
        double ullat = (Double) rasterImageParams.get("raster_ul_lat");
        double lrlat = (Double) rasterImageParams.get("raster_lr_lat");
        double xdpp = Math.abs(ullon - lrlon) / w;
        double ydpp = Math.abs(ullat - lrlat) / h;
        for (int i = 0; i < routingNodes.size() - 1; i++) {
            double fx = (GraphDB.nodeList.get(routingNodes.get(i)).lon - ullon) / (xdpp);
            double fy = (ullat - GraphDB.nodeList.get(routingNodes.get(i)).lat) / (ydpp);
            double tx = (GraphDB.nodeList.get(routingNodes.get(i + 1)).lon
                    - ullon) / (xdpp);
            double ty = (ullat - GraphDB.nodeList.get(routingNodes.get(i + 1)).lat)
                    / (ydpp);
            grph.drawLine((int) fx, (int) fy, (int) tx, (int) ty);
        }
        if (!memorized.containsKey(routeParams)) {
            GraphNode node1 = new GraphNode(startNode.id, startNode.lon, startNode.lat);
            node1.neighbours = startNode.neighbours;
            node1.distance = Double.MAX_VALUE;
            node1.name = startNode.name;
            GraphNode node2 = new GraphNode(endNode.id, endNode.lon, endNode.lat);
            node2.neighbours = endNode.neighbours;
            node2.distance = Double.MAX_VALUE;
            node2.name = endNode.name;
            memorized.put(routeParams, new GraphNode[]{node1, node2});
        }
        return routingNodes;
    }

    /**
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     * @param prefix Prefix string to be searched for. Could be any case, with our without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public static List<String> getLocationsByPrefix(String prefix) {
        return Trie.wordsWithPrefix(prefix);
    }

    /**
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     * @param locationName A full name of a location searched for.
     * @return A list of locations whose cleaned name matches the
     * cleaned <code>locationName</code>, and each location is a map of parameters for the Json
     * response as specified: <br>
     * "lat" -> Number, The latitude of the node. <br>
     * "lon" -> Number, The longitude of the node. <br>
     * "name" -> String, The actual name of the node. <br>
     * "id" -> Number, The id of the node. <br>
     */
    public static List<Map<String, Object>> getLocations(String locationName) {
        return Trie.wordsContainsExactly(locationName);
    }

}
