/**
 * Created by mirooy on 2016. 8. 5..
 */


import java.util.*;


public class GraphNode implements Comparable<GraphNode> {
    long id;
    double lon;
    double lat;
    ArrayList<GraphNode> neighbours;
    String name;
    double distance;

    public GraphNode(long id, double lon, double lat) {
        this.id = id;
        this.lon = lon;
        this.lat = lat;
        this.neighbours = new ArrayList<>();
        name = null;
    }

    public int compareTo(GraphNode v1) {
        if (this.distance < v1.distance) {
            return -1;
        } else if (this.distance > v1.distance) {
            return 1;
        }
        return 0;
    }

    public String getName() {
        return name;
    }
    public void setName(String x1) {
        name = x1;
    }

}
