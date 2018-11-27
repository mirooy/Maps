
import java.util.ArrayList;
import java.util.List;


public class QuadTree {

    QTreeNode root;

    public QuadTree() {
        root = new QTreeNode();
    }

    public ArrayList<QTreeNode> depthSearch(double distancePerPixel,
                                            double ullat, double ullon,
                                            double lrlon, double lrlat) {
        ArrayList<QTreeNode> rtn = new ArrayList<QTreeNode>();
        if (root != null) {
            root.searchHelper(distancePerPixel, ullat, ullon, lrlon, lrlat, rtn);
        }
        return rtn;
    }

    public static int getRowLength(List<QTreeNode> data) {
        if (data.size() == 1) {
            return 1;
        }
        if (data.size() == 0) {
            return 0;
        }
        int rowLength = 0;
        double sameRow = data.get(0).getUpperLeftPoint().getLatY();
        for (QTreeNode s : data) {
            if (s.getUpperLeftPoint().getLatY() == sameRow) {
                rowLength++;
            }
        }
        return rowLength;
    }

    public static QTreeNode organise(List<QTreeNode> data) {
        QTreeNode temp = data.get(0);
        for (QTreeNode t : data) {
            temp = temp.compare(t);
        }
        return temp;
    }

    public static List<QTreeNode> sorting(List<QTreeNode> data) {
        List<QTreeNode> rtn = new ArrayList<>();
        QTreeNode temp;
        while (data.size() != 0) {
            temp = (organise(data));
            rtn.add(temp);
            data.remove(temp);
        }
        return rtn;
    }
}


