import java.util.ArrayList;

/**
 * Created by mirooy on 2016. 8. 5..
 */
public class Connections {
    String id;
    ArrayList<GraphNode> list = new ArrayList<GraphNode>();
    public Connections(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void addN(GraphNode s) {
        list.add(s);
    }
    public ArrayList<GraphNode> getList() {
        return list;
    }
    public int getSize() {
        return list.size();
    }

}

